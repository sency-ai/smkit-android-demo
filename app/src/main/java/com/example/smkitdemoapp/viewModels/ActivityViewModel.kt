package com.example.smkitdemoapp.viewModels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smkitdemoapp.BuildConfig
import com.example.smkitdemoapp.events.ExerciseRepDetected
import com.example.smkitdemoapp.events.SessionEvent
import com.example.smkitdemoapp.models.AssessmentExerciseResult
import com.example.smkitdemoapp.models.DemoExercise
import com.example.smkitdemoapp.states.session.SessionState
import com.example.smkitdemoapp.states.configure.ConfigureState
import com.example.smkitdemoapp.states.configure.Failed
import com.example.smkitdemoapp.states.configure.Loading
import com.example.smkitdemoapp.states.configure.Passed
import com.example.smkitdemoapp.states.exercise.Idle
import com.example.smkitdemoapp.states.exercise.ExerciseState
import com.example.smkitdemoapp.states.exercise.Playing
import com.example.smkitdemoapp.states.exercise.countRep
import com.example.smkitdemoapp.states.session.Ready
import com.example.smkitdemoapp.states.session.Running
import com.example.smkitdemoapp.states.session.Stopped
import com.sency.smkit.SMKit
import com.sency.smkit.listener.ConfigurationResult
import com.sency.smkit.listener.SMKitSessionListener
import com.sency.smkit.model.DetectionSessionResultData
import com.sency.smkit.model.FrameInfo
import com.sency.smkit.model.SMKitJoint
import com.sency.smkit.model.SMKitMovementData
import com.sency.smkit.model.BodyCalibrationState
import com.example.smkitdemoapp.skeleton.detectSkeletonType
import com.example.smkitdemoapp.skeleton.SkeletonType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.jvm.Throws

class ActivityViewModel: ViewModel() {

    private val exerciseList = mutableListOf<DemoExercise>()
    private var smKit: SMKit? = null

    /** Ensures we only start the first exercise once per session; captureSessionDidSet can fire multiple times. */
    private var hasStartedFirstExerciseThisSession = false

    /** When true, do not auto-start first exercise from captureSessionDidSet; assessment fragment drives flow. */
    var isAssessmentMode: Boolean = false
        private set

    /** Body calibration state for assessment calibration screen (BodyInside = in frame). */
    private val _bodyCalibrationState = MutableLiveData<BodyCalibrationState?>(null)
    val bodyCalibrationState: LiveData<BodyCalibrationState?> get() = _bodyCalibrationState

    /** Accumulated during current assessment exercise; cleared when starting next. */
    private val assessmentTechniqueScores = mutableListOf<Float>()
    private var assessmentPeakRom = 0f
    private val assessmentFeedbacks = mutableSetOf<String>()
    private val assessmentResults = mutableListOf<AssessmentExerciseResult>()
    private var assessmentTotalExercises = 0

    private val _assessmentResults = MutableLiveData<List<AssessmentExerciseResult>>(emptyList())
    val assessmentResultsLive: LiveData<List<AssessmentExerciseResult>> get() = _assessmentResults

    fun getAssessmentExerciseIndex(): Int = assessmentTotalExercises - exerciseList.size
    fun getAssessmentTotal(): Int = assessmentTotalExercises

    private val _exerciseListSize = MutableLiveData(exerciseList.size)
    val exerciseListSize: LiveData<Int> get() = _exerciseListSize

    private val _sessionEvents = MutableLiveData<SessionEvent>()
    val sessionEvents: LiveData<SessionEvent> get() = _sessionEvents

    private val _configureState = MutableStateFlow<ConfigureState>(Loading)
    val configureState: Flow<ConfigureState> get() = _configureState

    private val _sessionState = MutableStateFlow<SessionState>(Ready)
    val sessionState: Flow<SessionState> get() = _sessionState

    private val _exerciseState = MutableLiveData<ExerciseState>(Idle)
    val exerciseState: LiveData<ExerciseState> get() = _exerciseState

    private val _resultData = MutableLiveData<DetectionSessionResultData?>(null)
    val resultData: LiveData<DetectionSessionResultData?> get() = _resultData

    private val _frameInfo = MutableLiveData<FrameInfo?>(null)
    val frameInfo: LiveData<FrameInfo?> get() = _frameInfo

    private val _poseData = MutableLiveData<Map<SMKitJoint, PointF>?>(null)
    val poseData: LiveData<Map<SMKitJoint, PointF>?> get() = _poseData

    private val _feedbacks = MutableLiveData<List<String>>(emptyList())
    val feedbacks: LiveData<List<String>> get() = _feedbacks

    private val _isShallowRep = MutableLiveData<Boolean?>(null)
    val isShallowRep: LiveData<Boolean?> get() = _isShallowRep

    private val _isInPosition = MutableLiveData<Boolean>(false)
    val isInPosition: LiveData<Boolean> get() = _isInPosition

    /** Current ROM value 0f–1f from detection; only relevant when [romRangeMin]/[romRangeMax] are set. */
    private val _currentRomValue = MutableLiveData(0f)
    val currentRomValue: LiveData<Float> get() = _currentRomValue

    /** ROM target range from startDetection; null when exercise has no ROM. */
    private val _romRangeMin = MutableLiveData<Float?>(null)
    val romRangeMin: LiveData<Float?> get() = _romRangeMin
    private val _romRangeMax = MutableLiveData<Float?>(null)
    val romRangeMax: LiveData<Float?> get() = _romRangeMax

    /** True when current exercise is dynamic (rep-counting); false for static/mobility. */
    private val _isDynamicExercise = MutableLiveData(false)
    val isDynamicExercise: LiveData<Boolean> get() = _isDynamicExercise

    private val _showSkeleton = MutableLiveData(true)
    val showSkeleton: LiveData<Boolean> get() = _showSkeleton

    fun setShowSkeleton(show: Boolean) {
        _showSkeleton.value = show
    }

    fun addExercise(exercise: DemoExercise) {
        exerciseList.add(exercise)
        _exerciseListSize.postValue(exerciseList.size)
    }

    fun clearExerciseList() {
        exerciseList.clear()
        _exerciseListSize.postValue(exerciseList.size)
    }

    fun configure(context: Context) {
        smKit = SMKit.Builder(context).authKey(BuildConfig.sdk_auth_key).isUI(false).build()
        smKit?.configure(configureListener)
        smKit?.smKitSessionListener(smKitSessionListener)
    }


    fun startSession(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        hasStartedFirstExerciseThisSession = false
        smKit?.startSession(lifecycleOwner, surfaceProvider)
        _sessionState.value = Running
    }

    /** Start collecting body calibration for assessment calibration screen (called from captureSessionDidSet when assessment). */
    private fun startBodyCalibrationCollection() {
        _bodyCalibrationState.value = null
        viewModelScope.launch(Dispatchers.Main) {
            try {
                smKit?.observeBodyCalibrationData()?.collect { state ->
                    _bodyCalibrationState.postValue(state)
                }
            } catch (_: Exception) {}
        }
    }

    private fun loadExercise(): String {
        if(exerciseList.isEmpty()) throw IllegalStateException("No Exercises")
        val exercise= exerciseList.removeAt(0)
        return exercise.name
    }

    fun runExercise() {
        when(_exerciseState.value) {
            null,
            Idle -> {
                try{
                    if (isAssessmentMode) {
                        assessmentTechniqueScores.clear()
                        assessmentPeakRom = 0f
                        assessmentFeedbacks.clear()
                    }
                    _feedbacks.postValue(emptyList())
                    _isShallowRep.postValue(null)
                    _currentRomValue.postValue(0f)
                    _romRangeMin.postValue(null)
                    _romRangeMax.postValue(null)
                    _isDynamicExercise.postValue(false)
                    val exercise = loadExercise()
                    val kit = smKit ?: throw IllegalStateException("SMKit not configured")
                    @Suppress("UNCHECKED_CAST")
                    val result = kit.startDetection(exercise, null) as? Pair<Any, Any>
                    result?.let { (romRange, exerciseType) ->
                        try {
                            // RomRange is a value class wrapping ClosedRange<Float>; at runtime may be boxed or unboxed
                            val rangeObj = romRange.javaClass.methods
                                .firstOrNull { it.name == "getValue" }?.invoke(romRange) ?: romRange
                            val start = rangeObj.javaClass.methods
                                .firstOrNull { it.name == "getStart" }?.invoke(rangeObj) as? Float
                            val endInclusive = rangeObj.javaClass.methods
                                .firstOrNull { it.name == "getEndInclusive" }?.invoke(rangeObj) as? Float
                            if (start != null && endInclusive != null && start < endInclusive) {
                                _romRangeMin.postValue(start)
                                _romRangeMax.postValue(endInclusive)
                            }
                            val typeName = exerciseType.javaClass.methods
                                .firstOrNull { it.name == "name" }?.invoke(exerciseType) as? String
                            _isDynamicExercise.postValue(typeName == "Dynamic")
                        } catch (_: Exception) { }
                    }
                    _exerciseState.postValue(Playing(exercise))
                } catch (e: Exception) {
                    // SMKit may throw e.g. DetectionAlreadyRunning, IllegalStateException; log and keep session running.
                    Log.e("ActivityViewModel", "Failed to start exercise: ${e.message}", e)
                }
            }
            is Playing -> throw IllegalStateException("Exercise already Running")
        }
    }

    /** Stops current exercise and starts the next one; if none left, stops session. */
    fun nextExercise() {
        when (_exerciseState.value) {
            null, Idle -> { /* no op */ }
            is Playing -> {
                smKit?.stopDetection()
                clearRomAndExerciseType()
                _exerciseState.postValue(Idle)
                if (exerciseList.isEmpty()) {
                    stopSession()
                } else {
                    runExercise()
                }
            }
        }
    }

    private fun clearExercise() {
        _exerciseState.postValue(Idle)
    }

    @Throws(IllegalStateException::class)
    fun pauseExercise() {
        when(val exercise = _exerciseState.value) {
            null,
            Idle -> {
                throw IllegalStateException("Exercise not Set")
            }
            is Playing -> {
                val exerciseInfo = smKit?.stopDetection()
                Log.d("ViewModel", "Exercise ${exercise.exerciseName} Finished: $exerciseInfo")
                clearRomAndExerciseType()
                _exerciseState.postValue(Idle)
            }
        }
    }

    fun stopSession() {
        hasStartedFirstExerciseThisSession = false
        isAssessmentMode = false
        clearExercise()
        clearRomAndExerciseType()
        smKit?.stopSession()?.also(::logSessionResults)
        _sessionState.value = Stopped
    }

    private fun clearRomAndExerciseType() {
        _currentRomValue.postValue(0f)
        _romRangeMin.postValue(null)
        _romRangeMax.postValue(null)
        _isDynamicExercise.postValue(false)
    }

    /** Call when assessment exercise timer ends. Stops detection, builds result, returns true if more exercises. */
    fun finishCurrentAssessmentExercise(timeInPositionSeconds: Float): Boolean {
        val name = (_exerciseState.value as? Playing)?.exerciseName ?: return false
        try {
            smKit?.stopDetection()
        } catch (_: Exception) {}
        clearRomAndExerciseType()
        _exerciseState.postValue(Idle)
        val avgScore = if (assessmentTechniqueScores.isEmpty()) 0f
            else assessmentTechniqueScores.reduce(Float::plus) / assessmentTechniqueScores.size
        val peakRom = if (assessmentPeakRom > 0) assessmentPeakRom else null
        assessmentResults.add(
            AssessmentExerciseResult(
                name = name,
                techniqueScore = avgScore * 100f,
                feedbacks = assessmentFeedbacks.toList(),
                timeInPosition = timeInPositionSeconds,
                peakRom = peakRom
            )
        )
        _assessmentResults.postValue(assessmentResults.toList())
        assessmentTechniqueScores.clear()
        assessmentPeakRom = 0f
        assessmentFeedbacks.clear()
        return exerciseList.isNotEmpty()
    }

    fun getNextAssessmentExerciseName(): String? = exerciseList.firstOrNull()?.name

    fun getAssessmentResults(): List<AssessmentExerciseResult> = assessmentResults.toList()

    fun clearAssessmentResults() {
        assessmentResults.clear()
        _assessmentResults.postValue(emptyList())
    }


    private fun logSessionResults(results: DetectionSessionResultData) {
        Log.d("ViewModel", "Session Results: $results")
        viewModelScope.launch(Dispatchers.Default) {
            _resultData.postValue(results)
        }
    }

    fun clearChoices() {
        exerciseList.clear()
    }

    /** Extracts feedback descriptions without depending on FormFeedbackType (not on app classpath). */
    private fun feedbackToListOfStrings(data: SMKitMovementData): List<String> {
        return try {
            val feedbackProp = data::class.java.getMethod("getFeedback")
            @Suppress("UNCHECKED_CAST")
            val list = feedbackProp.invoke(data) as? List<*> ?: emptyList<Any>()
            list.map { it.toString() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Preloads the iOS-aligned assessment exercise list and prepares for assessment flow. */
    fun loadDemoAssessmentExercises() {
        isAssessmentMode = true
        clearAssessmentResults()
        clearExerciseList()
        assessmentTotalExercises = 5
        listOf(
            DemoExercise.OverheadMobility,
            DemoExercise.SquatRegularOverheadStatic,
            DemoExercise.JeffersonCurl,
            DemoExercise.StandingSideBendRight,
            DemoExercise.StandingSideBendLeft
        ).forEach { addExercise(it) }
    }

    private val configureListener = object: ConfigurationResult {
        override fun onFailure() {
            _configureState.value = Failed
        }

        override fun onSuccess() {
            smKit?.preparePoseEstimation()
            _configureState.value = Passed
        }
    }

    private val smKitSessionListener = object : SMKitSessionListener {
        override fun captureSessionDidSet(frameInfo: FrameInfo) {
            _frameInfo.postValue(frameInfo)
            if (isAssessmentMode) {
                startBodyCalibrationCollection()
                return
            }
            if (exerciseList.isEmpty() || hasStartedFirstExerciseThisSession) return
            hasStartedFirstExerciseThisSession = true
            viewModelScope.launch(Dispatchers.Main) {
                runExercise()
            }
        }

        override fun captureSessionDidStop() {
            hasStartedFirstExerciseThisSession = false
        }

        override fun handleDetectionData(movementData: SMKitMovementData?) {
            movementData?.let { data ->
                if (isAssessmentMode) {
                    if (data.isInPosition) {
                        data.techniqueScore.let { if (it > 0) assessmentTechniqueScores.add(it) }
                        if (data.currentRomValue > 0) assessmentPeakRom = maxOf(assessmentPeakRom, data.currentRomValue)
                        feedbackToListOfStrings(data).forEach { assessmentFeedbacks.add(it) }
                    }
                }
                _feedbacks.postValue(feedbackToListOfStrings(data))
                _isShallowRep.postValue(data.isShallowRep)
                _isInPosition.postValue(data.isInPosition)
                _currentRomValue.postValue(data.currentRomValue)
            }
            if (movementData?.didFinishMovement == true) {
                val exercise = exerciseState.value
                if (exercise is Playing) {
                    val newState = exercise.countRep()
                    _exerciseState.postValue(newState)
                    _sessionEvents.postValue(ExerciseRepDetected(newState.repCounter))
                }
            }
        }

        override fun handlePositionData(poseData: Map<SMKitJoint, PointF>?) {
            _poseData.postValue(poseData)
        }

        override fun handleSessionErrors() {}
    }
}
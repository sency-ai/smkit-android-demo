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
import com.example.smkitdemoapp.events.SessionEvent
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.jvm.Throws

class ActivityViewModel: ViewModel() {

    private val exerciseList = mutableListOf<DemoExercise>()
    private var smKit: SMKit? = null

    private val _exerciseListSize = MutableLiveData(exerciseList.size)
    val exerciseListSIze: LiveData<Int> get() = _exerciseListSize

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

    fun addExercise(exercise: DemoExercise) {
        exerciseList.add(exercise)
        _exerciseListSize.postValue(exerciseList.size)
    }

    fun clearExerciseList() {
        exerciseList.clear()
        _exerciseListSize.postValue(exerciseList.size)
    }

    fun configure(context: Context) {
        smKit = SMKit.Builder(context).authKey("YOUR_AUTH_KEY").isUI(false).build()
        smKit?.configure(configureListener)
        smKit?.smKitSessionListener(smKitSessionListener)
    }


    fun startSession(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        smKit?.startSession(lifecycleOwner, surfaceProvider)
        _sessionState.value = Running
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
                    val exercise = loadExercise()
                    smKit?.startDetection(exercise)
                    _exerciseState.postValue(Playing(exercise))
                } catch (e: IllegalStateException) {
                    stopSession()
                }
            }
            is Playing -> throw IllegalStateException("Exercise already Running")
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
                _exerciseState.postValue(Idle)
            }
        }
    }

    fun stopSession() {
        clearExercise()
        smKit?.stopSession()?.also(::logSessionResults)
        _sessionState.value = Stopped
    }


    private fun logSessionResults(results: DetectionSessionResultData) {
        Log.d("ViewModel", "Session Results: $results")
        viewModelScope.launch(Dispatchers.Default) {
            _resultData.postValue(results)
        }
    }

    fun clearChooices() {
        exerciseList.clear()
    }

    private val configureListener = object: ConfigurationResult {
        override fun onFailure() {
            _configureState.value = Failed
        }

        override fun onSuccess() {
            _configureState.value = Passed
        }
    }

    private val smKitSessionListener = object : SMKitSessionListener {
        override fun captureSessionDidSet(frameInfo: FrameInfo) {}

        override fun captureSessionDidStop() {}

        override fun handleDetectionData(movementData: SMKitMovementData?) {
            Log.d("ViewModel movement", "movmentData: $movementData")
            if (movementData?.didFinishMovement != true) return
            val exercise = exerciseState.value
            if (exercise is Playing) {
                _exerciseState.postValue(exercise.countRep())
            }
        }

        override fun handlePositionData(poseData: Map<SMKitJoint, PointF>?) {
            Log.d("ViewModel", "poseDate: $poseData")
        }

        override fun handleSessionErrors() {}
    }
}
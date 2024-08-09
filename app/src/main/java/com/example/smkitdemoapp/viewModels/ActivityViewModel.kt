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
import com.example.smkitdemoapp.models.DemoExercise
import com.example.smkitdemoapp.models.ExerciseState
import com.example.smkitdemoapp.models.ConfigureState
import com.example.smkitdemoapp.models.SessionState
import com.sency.smkit.SMKit
import com.sency.smkit.listener.ConfigurationResult
import com.sency.smkit.listener.SMKitSessionListener
import com.sency.smkit.model.FrameInfo
import com.sency.smkit.model.SMKitJoint
import com.sency.smkit.model.SMKitMovementData

class ActivityViewModel: ViewModel() {

    private val exerciseList = mutableListOf<DemoExercise>()
    private var smKit: SMKit? = null

    private val _configureState = MutableLiveData<ConfigureState?>(null)
    val configureState: LiveData<ConfigureState?> get() = _configureState

    private val _sessionState = MutableLiveData<SessionState?>(null)
    val sessionState: LiveData<SessionState?> get() = _sessionState

    private val _exerciseState = MutableLiveData(initExerciseState())
    val exerciseState: LiveData<ExerciseState> get() = _exerciseState

    private val _repCounter = MutableLiveData(0)
    val repCounter: LiveData<Int> get() = _repCounter

    fun addExercise(exercise: DemoExercise) {
        exerciseList.add(exercise)
    }

    private val configureListener = object: ConfigurationResult {
        override fun onFailure() {
            val state = _configureState.value ?: initState()
            _configureState.postValue(state.copy(configure = false))
        }

        override fun onSuccess() {
            val state = _configureState.value ?: initState()
            _configureState.postValue(state.copy(configure = true))
            val exerciseState = _exerciseState.value
            val name = exerciseList[0].name
            _exerciseState.postValue(exerciseState?.copy(exerciseIndex = 0, exerciseName = name))
        }
    }

    fun configure(context: Context) {
        smKit = SMKit.Builder(context).authKey("").isUI(false).build()
        smKit?.configure(configureListener)
        smKit?.smKitSessionListener(object : SMKitSessionListener {
            override fun captureSessionDidSet(frameInfo: FrameInfo) { }

            override fun captureSessionDidStop() {}

            override fun handleDetectionData(movementData: SMKitMovementData?) {
                Log.d("ViewModel movement", "movmentData: $movementData")
                if(movementData?.didFinishMovement == true) {
                    val count = _repCounter.value ?: 0
                    _repCounter.postValue(count + 1)
                }
            }

            override fun handleOriginalCameraImage(image: Bitmap?) {}

            override fun handlePositionData(poseData: Map<SMKitJoint, PointF>?) {
                Log.d("ViewModel", "poseDate: $poseData")
            }

            override fun handleSessionErrors() {}
        })
    }

    fun onPause() {
        val exerciseInfo = smKit?.stopDetection()
        Log.d("ViewModel", "onPause: $exerciseInfo")
        val exerciseState = exerciseState.value
        val newIndex = (exerciseState?.exerciseIndex ?: 0) + 1
        if (newIndex == exerciseList.size) {
            onStop()
            return
        }
        val newName = exerciseList[newIndex].name
        _exerciseState.postValue(exerciseState?.copy(exerciseIndex = newIndex, exerciseName = newName, exerciseRunning = false))
    }

    fun onStart() {
        val exerciseState = exerciseState.value
        val index = exerciseState?.exerciseIndex ?: 0
        val exercise = exerciseList[index]
        smKit?.startDetection(exercise.name)
        _exerciseState.postValue(exerciseState?.copy(exerciseIndex = index, exerciseName = exercise.name, exerciseRunning = true))
    }

    fun onStop() {
        val resultData = smKit?.stopSession()
        Log.d("ViewModel", "onStop: $resultData")
        _exerciseState.value = exerciseState.value?.copy(exerciseIndex = 0, exerciseName = "", exerciseRunning = false)
        _sessionState.value = SessionState(sessionRunning = false)
    }

    fun startSession(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        smKit?.startSession(lifecycleOwner, surfaceProvider)
        _sessionState.value = SessionState(sessionRunning = true)
    }

    private fun initState() = ConfigureState(
        configure = false,
    )

    private fun initExerciseState() = ExerciseState(
        exerciseIndex = 0,
        exerciseName = "",
        exerciseRunning = false
    )
}
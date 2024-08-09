package com.example.smkitdemoapp.viewModels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.smkitdemoapp.models.DemoExercise
import com.example.smkitdemoapp.models.ExerciseState
import com.example.smkitdemoapp.models.State
import com.sency.smkit.SMKit
import com.sency.smkit.listener.ConfigurationResult
import com.sency.smkit.listener.DetectionDataListener
import com.sency.smkit.listener.SMKitSessionListener
import com.sency.smkit.model.FrameInfo
import com.sency.smkit.model.SMKitJoint
import com.sency.smkit.model.SMKitMovementData

class ActivityViewModel: ViewModel() {

    private val exerciseList = mutableListOf<DemoExercise>()
    private var smKit: SMKit? = null

    private val _state = MutableLiveData(initState())
    val state: LiveData<State> get() = _state

    private val _exerciseState = MutableLiveData(initExerciseState())
    val exerciseState: LiveData<ExerciseState> get() = _exerciseState

    private val _repCounter = MutableLiveData(0)
    val repCounter: LiveData<Int> get() = _repCounter

    fun addExercise(exercise: DemoExercise) {
        exerciseList.add(exercise)
    }

    private val configureListener = object: ConfigurationResult {
        override fun onFailure() {
            val state = _state.value
            _state.postValue(state?.copy(configure = false))
        }

        override fun onSuccess() {
            val state = _state.value
            _state.postValue(state?.copy(configure = true))
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
        val newName = exerciseList[newIndex].name
        _exerciseState.postValue(exerciseState?.copy(exerciseIndex = newIndex, exerciseName = newName))
    }

    fun onStart() {
        val exerciseState = exerciseState.value
        val index = exerciseState?.exerciseIndex ?: 0
        Log.d("ViewModel", "onStart: $index")
        if (index == exerciseList.size) {
            onStop()
            return
        }
        val exercise = exerciseList[index]
        smKit?.startDetection(exercise.name)
        _exerciseState.postValue(exerciseState?.copy(exerciseIndex = index, exerciseName = exercise.name))
    }

    fun onStop() {
        val resultData = smKit?.stopSession()
        Log.d("ViewModel", "onStop: $resultData")
        _exerciseState.value = exerciseState.value?.copy(exerciseIndex = 0, exerciseName = "")
    }

    fun startSession(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        smKit?.startSession(lifecycleOwner, surfaceProvider)
    }

    private fun initState() = State(
        configure = false,
    )

    private fun initExerciseState() = ExerciseState(
        exerciseIndex = 0,
        exerciseName = "",
    )
}
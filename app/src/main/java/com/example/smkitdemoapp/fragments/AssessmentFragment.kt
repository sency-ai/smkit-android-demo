package com.example.smkitdemoapp.fragments

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.replace
import com.example.smkitdemoapp.R
import com.example.smkitdemoapp.databinding.FragmentAssessmentBinding
import com.sency.smkit.model.BodyCalibrationState
import com.sency.smkit.model.BodyInside
import com.example.smkitdemoapp.calibration.PhoneCalibrationHelper
import com.example.smkitdemoapp.viewModels.ActivityViewModel
import com.example.smkitdemoapp.states.exercise.ExerciseState
import com.example.smkitdemoapp.states.exercise.Playing

private const val EXERCISE_DURATION_SEC = 15f
private const val TICK_MS = 100L
private const val COUNTDOWN_INTERVAL_MS = 1000L

enum class AssessmentPhase { CALIBRATION, COUNTDOWN, EXERCISE }

class AssessmentFragment : Fragment() {

    private var _binding: FragmentAssessmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ActivityViewModel by activityViewModels()

    private var phase = AssessmentPhase.CALIBRATION
    private var countdownValue = 3
    private var phoneCalibrationHelper: PhoneCalibrationHelper? = null
    private var exerciseTimeRemaining = EXERCISE_DURATION_SEC
    private var timeInPositionSec = 0f
    private val handler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            if (phase != AssessmentPhase.EXERCISE) return
            exerciseTimeRemaining -= 0.1f
            if (viewModel.isInPosition.value == true) timeInPositionSec += 0.1f
            updateExerciseTimerUi()
            if (exerciseTimeRemaining <= 0) {
                onExerciseTimeEnd()
                return
            }
            handler.postDelayed(this, TICK_MS)
        }
    }
    private val countdownRunnable = object : Runnable {
        override fun run() {
            if (phase != AssessmentPhase.COUNTDOWN || countdownValue <= 0) return
            countdownValue--
            binding.countdownNumber.text = if (countdownValue > 0) countdownValue.toString() else ""
            if (countdownValue <= 0) {
                onCountdownEnd()
            } else {
                handler.postDelayed(this, COUNTDOWN_INTERVAL_MS)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAssessmentBinding.inflate(inflater, container, false)
        viewModel.startSession(viewLifecycleOwner, binding.previewView.surfaceProvider)
        setObservers()
        setClickListeners()
        showCalibration()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (phase == AssessmentPhase.CALIBRATION) phoneCalibrationHelper?.start()
    }

    override fun onPause() {
        if (phase == AssessmentPhase.CALIBRATION) phoneCalibrationHelper?.stop()
        super.onPause()
    }

    override fun onDestroyView() {
        handler.removeCallbacks(tickRunnable)
        handler.removeCallbacks(countdownRunnable)
        phoneCalibrationHelper?.stop()
        phoneCalibrationHelper = null
        super.onDestroyView()
        _binding = null
    }

    private fun setClickListeners() {
        binding.calibrationSkipButton.setOnClickListener { onCalibrationComplete() }
        binding.calibrationClose.setOnClickListener { stopAndBack() }
        binding.countdownClose.setOnClickListener { stopAndBack() }
        binding.assessmentExerciseClose.setOnClickListener { stopAndBack() }
    }

    private fun setObservers() {
        viewModel.bodyCalibrationState.observe(viewLifecycleOwner) { state ->
            if (state == null) return@observe
            val inFrame = state is BodyInside
            when (state) {
                is BodyInside -> {
                    binding.bodyCalOverlay.guideRect = state.rect
                    binding.bodyCalOverlay.videoWidth = state.frameSize.width
                    binding.bodyCalOverlay.videoHeight = state.frameSize.height
                }
                is com.sency.smkit.model.BodyOutside -> {
                    binding.bodyCalOverlay.guideRect = state.rect
                    binding.bodyCalOverlay.videoWidth = state.frameSize.width
                    binding.bodyCalOverlay.videoHeight = state.frameSize.height
                }
                is com.sency.smkit.model.Idle -> {
                    binding.bodyCalOverlay.guideRect = state.rect
                    binding.bodyCalOverlay.videoWidth = state.frameSize.width
                    binding.bodyCalOverlay.videoHeight = state.frameSize.height
                }
            }
            binding.bodyCalOverlay.inPosition = inFrame
            binding.calibrationBodyStatus.text = if (inFrame) "✓" else "○"
            binding.calibrationBodyStatus.setTextColor(if (inFrame) Color.GREEN else Color.WHITE)
            updateCalibrationStatusMessage(phoneReady = phoneCalibrationHelper?.isPhoneReady?.value == true, inFrame)
            if (inFrame && phoneCalibrationHelper?.isPhoneReady?.value == true && phase == AssessmentPhase.CALIBRATION) {
                handler.postDelayed({ onCalibrationComplete() }, 500)
            }
        }
        viewModel.poseData.observe(viewLifecycleOwner) { pose ->
            binding.skeletonOverlay.poseData = pose
        }
        viewModel.frameInfo.observe(viewLifecycleOwner) { info ->
            if (info != null) {
                binding.skeletonOverlay.videoWidth = info.width
                binding.skeletonOverlay.videoHeight = info.height
                binding.skeletonOverlay.isImageFlipped = info.isImageFlipped
            }
        }
        viewModel.exerciseState.observe(viewLifecycleOwner) { state ->
            if (state is Playing && phase == AssessmentPhase.EXERCISE) {
                binding.assessmentExerciseName.text = state.exerciseName
                binding.assessmentExerciseIndex.text = "${viewModel.getAssessmentExerciseIndex()} / ${viewModel.getAssessmentTotal()}"
                binding.assessmentRepCounter.text = state.repCounter.toString()
            }
        }
        viewModel.feedbacks.observe(viewLifecycleOwner) { list ->
            if (phase != AssessmentPhase.EXERCISE) return@observe
            binding.assessmentFeedbacksContainer.removeAllViews()
            list.forEach { text ->
                binding.assessmentFeedbacksContainer.addView(TextView(requireContext()).apply {
                    setTextColor(Color.WHITE)
                    setText(text)
                })
            }
        }
        viewModel.isInPosition.observe(viewLifecycleOwner) { inPosition ->
            if (phase != AssessmentPhase.EXERCISE) return@observe
            binding.assessmentInPositionLabel.text = if (inPosition) "In position" else "Get in position"
            binding.assessmentInPositionLabel.setTextColor(if (inPosition) Color.GREEN else Color.WHITE)
            binding.assessmentRomGauge.isInPosition = inPosition
        }
        viewModel.romRangeMin.observe(viewLifecycleOwner) { _ ->
            if (phase != AssessmentPhase.EXERCISE) return@observe
            updateAssessmentRomGauge()
        }
        viewModel.romRangeMax.observe(viewLifecycleOwner) { _ ->
            if (phase != AssessmentPhase.EXERCISE) return@observe
            updateAssessmentRomGauge()
        }
        viewModel.currentRomValue.observe(viewLifecycleOwner) { value ->
            if (phase != AssessmentPhase.EXERCISE) return@observe
            binding.assessmentRomGauge.value = value
        }
        viewModel.isDynamicExercise.observe(viewLifecycleOwner) { isDynamic ->
            if (phase != AssessmentPhase.EXERCISE) return@observe
            binding.assessmentRepCounter.visibility = if (isDynamic) View.VISIBLE else View.GONE
        }
    }

    private fun updateAssessmentRomGauge() {
        val min = viewModel.romRangeMin.value
        val max = viewModel.romRangeMax.value
        val hasRange = min != null && max != null
        binding.assessmentRomGauge.visibility = if (hasRange) View.VISIBLE else View.GONE
        if (hasRange) {
            binding.assessmentRomGauge.rangeMin = min!!
            binding.assessmentRomGauge.rangeMax = max!!
        }
    }

    private fun showCalibration() {
        phase = AssessmentPhase.CALIBRATION
        binding.calibrationOverlay.visibility = View.VISIBLE
        binding.countdownOverlay.visibility = View.GONE
        binding.exercisePhaseUi.visibility = View.GONE
        if (phoneCalibrationHelper == null) {
            phoneCalibrationHelper = PhoneCalibrationHelper(requireContext().getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager)
            phoneCalibrationHelper?.isPhoneReady?.observe(viewLifecycleOwner) { ready ->
                binding.calibrationPhoneStatus.text = if (ready) "✓" else "○"
                binding.calibrationPhoneStatus.setTextColor(if (ready) Color.GREEN else Color.WHITE)
                val inFrame = viewModel.bodyCalibrationState.value is BodyInside
                updateCalibrationStatusMessage(phoneReady = ready, inFrame)
                if (ready && inFrame && phase == AssessmentPhase.CALIBRATION) {
                    handler.postDelayed({ onCalibrationComplete() }, 500)
                }
            }
        }
        phoneCalibrationHelper?.start()
        updateCalibrationStatusMessage(phoneReady = false, inFrame = false)
    }

    private fun updateCalibrationStatusMessage(phoneReady: Boolean, inFrame: Boolean) {
        binding.calibrationStatusMessage.text = when {
            !phoneReady -> "Tilt your phone upright"
            !inFrame -> "Step into the frame"
            else -> "Hold still…"
        }
    }

    private fun onCalibrationComplete() {
        if (phase != AssessmentPhase.CALIBRATION) return
        phoneCalibrationHelper?.stop()
        binding.calibrationOverlay.visibility = View.GONE
        showCountdown()
    }

    private fun showCountdown() {
        phase = AssessmentPhase.COUNTDOWN
        val nextName = viewModel.getNextAssessmentExerciseName() ?: run {
            goToSummary()
            return
        }
        binding.countdownExerciseName.text = nextName
        countdownValue = 3
        binding.countdownNumber.text = "3"
        binding.countdownOverlay.visibility = View.VISIBLE
        binding.exercisePhaseUi.visibility = View.GONE
        handler.postDelayed(countdownRunnable, COUNTDOWN_INTERVAL_MS)
    }

    private fun onCountdownEnd() {
        handler.removeCallbacks(countdownRunnable)
        binding.countdownOverlay.visibility = View.GONE
        phase = AssessmentPhase.EXERCISE
        exerciseTimeRemaining = EXERCISE_DURATION_SEC
        timeInPositionSec = 0f
        binding.assessmentTimerProgress.max = (EXERCISE_DURATION_SEC * 10).toInt()
        binding.assessmentTimerProgress.progress = (EXERCISE_DURATION_SEC * 10).toInt()
        viewModel.runExercise()
        binding.exercisePhaseUi.visibility = View.VISIBLE
        updateExerciseTimerUi()
        handler.postDelayed(tickRunnable, TICK_MS)
    }

    private fun updateExerciseTimerUi() {
        binding.assessmentTimer.text = String.format("%.1f", maxOf(0f, exerciseTimeRemaining))
        binding.assessmentTimerProgress.progress = (exerciseTimeRemaining * 10).toInt()
    }

    private fun onExerciseTimeEnd() {
        handler.removeCallbacks(tickRunnable)
        val hasMore = viewModel.finishCurrentAssessmentExercise(timeInPositionSec)
        if (hasMore) {
            showCountdown()
        } else {
            goToSummary()
        }
    }

    private fun goToSummary() {
        viewModel.stopSession()
        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, AssessmentSummaryFragment())
            .commit()
    }

    private fun stopAndBack() {
        viewModel.stopSession()
        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, WelcomeFragment())
            .commit()
    }
}

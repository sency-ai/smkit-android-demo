package com.example.smkitdemoapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.Preview.SurfaceProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.transition.Visibility
import com.example.smkitdemoapp.R
import com.example.smkitdemoapp.databinding.FragmentWorkoutBinding
import com.example.smkitdemoapp.events.ExerciseRepDetected
import com.example.smkitdemoapp.events.SessionEvent
import com.example.smkitdemoapp.states.configure.ConfigureState
import com.example.smkitdemoapp.states.configure.Failed
import com.example.smkitdemoapp.states.configure.Loading
import com.example.smkitdemoapp.states.configure.Passed
import com.example.smkitdemoapp.states.exercise.ExerciseState
import com.example.smkitdemoapp.states.exercise.Idle
import com.example.smkitdemoapp.states.exercise.Playing
import com.example.smkitdemoapp.states.session.Ready
import com.example.smkitdemoapp.states.session.Running
import com.example.smkitdemoapp.states.session.SessionState
import com.example.smkitdemoapp.states.session.Stopped
import com.example.smkitdemoapp.viewModels.ActivityViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class WorkoutFragment: Fragment() {
    private var _binding: FragmentWorkoutBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ActivityViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutBinding.inflate(inflater, container, false)
        setObservers()
        setClickListeners()
        viewModel.startSession(viewLifecycleOwner, binding.previewView.surfaceProvider)
        return binding.root
    }

    private fun setClickListeners() {
        with(binding) {
            pauseView.setOnClickListener {
                viewModel.pauseExercise()
                setPlayerViewsPauseState()
            }
            playView.setOnClickListener {
                viewModel.runExercise()
                setPlayerViewsStartState()
            }
            stopView.setOnClickListener {
                viewModel.stopSession()
                setPlayerViewsStopState()
            }
        }
    }

    private fun setObservers() {
        observeExerciseState()
        observeSessionState()
        observeSessionEvents()
    }

    private fun observeSessionState() {
        viewModel.sessionState.filterNotNull().onEach(::handleSessionState).launchIn(CoroutineScope(Dispatchers.Main))
    }

    private fun handleSessionState(sessionState: SessionState) {
        when (sessionState) {
            Ready -> hideMediaPlayer()
            Running -> showMediaPlayer()
            Stopped -> {
                hideMediaPlayer()
                navigateToSelectionPage()
            }
        }
    }

    private fun observeExerciseState() {
        viewModel.exerciseState.observe(viewLifecycleOwner, ::handleExerciseState)
    }

    private fun handleExerciseState(state: ExerciseState) {
        when (state) {
            Idle -> {
                binding.exerciseNameView.text = ""
                binding.pauseView.hide()
                binding.repCounterView.hide()
                binding.playView.show()
            }
            is Playing -> {
                binding.exerciseNameView.text = state.exerciseName
                binding.repCounterView.text = state.repCounter.toString()
                binding.playView.hide()
                binding.pauseView.show()
                binding.repCounterView.show()
            }
        }
    }

    private fun observeSessionEvents() {
        viewModel.sessionEvents.observe(viewLifecycleOwner, ::handleSessionEvents)
    }

    private fun handleSessionEvents(event: SessionEvent) {
        when (event) {
            is ExerciseRepDetected -> setRepCounterNumber(event.repNumber)
        }
    }

    private fun setRepCounterNumber(repNumber: Int) {
        binding.repCounterView.text = repNumber.toString()
    }

    private fun showMediaPlayer() {
        binding.playView.show()
        binding.stopView.show()
    }
    private fun hideMediaPlayer() {
        binding.playView.hide()
        binding.pauseView.hide()
        binding.stopView.hide()
    }

    private fun View.hide() {
        isEnabled = false
        visibility = View.INVISIBLE
    }
    private fun View.show() {
        isEnabled = true
        visibility = View.VISIBLE
    }

    private fun navigateToSelectionPage() {
        parentFragmentManager.beginTransaction().apply {
            replace(R.id.nav_host_fragment, ResultFragment())
        }.commit()
    }

    private fun FragmentWorkoutBinding.setPlayerViewsStopState() {
        pauseView.visibility = View.INVISIBLE
        playView.visibility = View.INVISIBLE
        stopView.visibility = View.INVISIBLE
        repCounterView.visibility = View.INVISIBLE
        previewView.visibility = View.INVISIBLE
    }

    private fun FragmentWorkoutBinding.setPlayerViewsStartState() {
        pauseView.visibility = View.VISIBLE
        playView.visibility = View.INVISIBLE
        stopView.visibility = View.VISIBLE
        repCounterView.visibility = View.INVISIBLE
    }

    private fun FragmentWorkoutBinding.setPlayerViewsPauseState() {
        pauseView.visibility = View.INVISIBLE
        repCounterView.visibility = View.INVISIBLE
        playView.visibility = View.VISIBLE
        stopView.visibility = View.VISIBLE
    }
}
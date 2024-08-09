package com.example.smkitdemoapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.Preview.SurfaceProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import com.example.smkitdemoapp.R
import com.example.smkitdemoapp.databinding.FragmentWorkoutBinding
import com.example.smkitdemoapp.viewModels.ActivityViewModel

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
        viewModel.configure(binding.root.context)

        setObservers()
        setClickListeners()
        startSession(viewLifecycleOwner, binding.previewView.surfaceProvider)
        return binding.root
    }

    private fun setObservers() {
        viewModel.configureState.observe(viewLifecycleOwner) { state ->
            if(state == null) return@observe
            when (state.configure) {
                false -> Toast.makeText(
                    binding.root.context,
                    "Failed to configure",
                    Toast.LENGTH_SHORT
                ).show()

                true -> {
                    Toast.makeText(
                        binding.root.context,
                        "configure Success",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
        viewModel.repCounter.observe(viewLifecycleOwner) { rep ->
            binding.repCounterView.text = rep.toString()
        }
        viewModel.exerciseState.observe(viewLifecycleOwner) { state ->
            binding.exerciseNameView.text = state.exerciseName
        }
        viewModel.sessionState.observe(viewLifecycleOwner) { state ->
            if(state == null) return@observe
            when(state.sessionRunning) {
                true -> {
                    binding.playView.isEnabled = true
                    binding.pauseView.isEnabled = true
                    binding.stopView.isEnabled = true
                }
                false -> {
                    binding.playView.isEnabled = false
                    binding.pauseView.isEnabled = false
                    binding.stopView.isEnabled = false
                    parentFragmentManager.beginTransaction().apply {
                        add(R.id.nav_host_fragment, SelectExerciesFragment())
                    }.commit()
                }
            }
        }
    }

    private fun setClickListeners() {
        with(binding) {
            pauseView.setOnClickListener {
                viewModel.onPause()
                pauseView.visibility = View.INVISIBLE
                playView.visibility = View.VISIBLE
                repCounterView.visibility = View.INVISIBLE
                repCounterView.text = "0"
            }
            playView.setOnClickListener {
                viewModel.onStart()
                pauseView.visibility = View.VISIBLE
                playView.visibility = View.INVISIBLE
                repCounterView.visibility = View.VISIBLE
            }
            stopView.setOnClickListener {
                viewModel.onStop()
                pauseView.visibility = View.INVISIBLE
                playView.visibility = View.INVISIBLE
                stopView.visibility = View.INVISIBLE
                repCounterView.visibility = View.INVISIBLE
            }
        }
    }

    private fun startSession(lifecycleOwner: LifecycleOwner, surfaceProvider: SurfaceProvider) {
        viewModel.startSession(lifecycleOwner, surfaceProvider)
    }
}
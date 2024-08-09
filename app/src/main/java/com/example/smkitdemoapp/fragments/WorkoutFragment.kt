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
import com.example.smkitdemoapp.databinding.FragmentWorkoutBinding
import com.example.smkitdemoapp.viewModels.ActivityViewModel
import com.sency.smkit.SMKit
import com.sency.smkit.listener.ConfigurationResult

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
        viewModel.state.observe(viewLifecycleOwner) { state ->
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
    }

    private fun setClickListeners() {
        with(binding) {
            pauseView.setOnClickListener {
                viewModel.onPause()
                pauseView.visibility = View.INVISIBLE
                playView.visibility = View.VISIBLE
            }
            playView.setOnClickListener {
                viewModel.onStart()
                pauseView.visibility = View.VISIBLE
                playView.visibility = View.INVISIBLE
            }
            stopView.setOnClickListener {
                viewModel.onStop()
                pauseView.visibility = View.INVISIBLE
                playView.visibility = View.INVISIBLE
                stopView.visibility = View.INVISIBLE
            }
        }
    }

    private fun startSession(lifecycleOwner: LifecycleOwner, surfaceProvider: SurfaceProvider) {
        viewModel.startSession(lifecycleOwner, surfaceProvider)
    }
}
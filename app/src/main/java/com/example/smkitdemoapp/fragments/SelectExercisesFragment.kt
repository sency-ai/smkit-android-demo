package com.example.smkitdemoapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smkitdemoapp.R
import com.example.smkitdemoapp.adapter.ExerciseClickEvent
import com.example.smkitdemoapp.adapter.SelectExerciseAdapter
import com.example.smkitdemoapp.databinding.FragmentSelectExercisesBinding
import com.example.smkitdemoapp.models.DemoExercise
import com.example.smkitdemoapp.viewModels.ActivityViewModel

class SelectExercisesFragment : Fragment() {

    private var _binding: FragmentSelectExercisesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ActivityViewModel by activityViewModels()

    private val exerciseClickEvent = object : ExerciseClickEvent {
        override fun exercise(exercise: DemoExercise) {
            viewModel.addExercise(exercise)
        }
    }
    private val adapter = SelectExerciseAdapter(exerciseClickEvent).apply {
        addItems(DemoExercise.ios2DExercises)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectExercisesBinding.inflate(inflater, container, false)

        bindViews()
        viewModel.clearChoices()
        return binding.root
    }

    private fun bindViews() {
        bindList()
        setStartClickListener()
        viewModel.exerciseListSize.observe(viewLifecycleOwner) { size ->
            binding.startButton.isEnabled = size != 0
            binding.headerCounter.text = size.toString()
        }
        binding.clearButton.setOnClickListener {
            viewModel.clearExerciseList()
            adapter.clearChoices()
        }
    }

    private fun setStartClickListener() {
        binding.startButton.setOnClickListener {
            viewModel.setShowSkeleton(binding.showSkeletonSwitch.isChecked)
            parentFragmentManager.beginTransaction().apply {
                add(R.id.nav_host_fragment, WorkoutFragment())
                addToBackStack(WorkoutFragment::class.java.simpleName)
            }.commit()
        }
    }

    private fun bindList() {
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
    }
}

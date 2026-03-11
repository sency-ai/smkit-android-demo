package com.example.smkitdemoapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.smkitdemoapp.R
import com.example.smkitdemoapp.databinding.FragmentWelcomeBinding

class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        binding.start2dButton.setOnClickListener { onStart2D() }
        binding.demoAssessmentButton.setOnClickListener { onDemoAssessment() }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onStart2D() {
        parentFragmentManager.commit {
            replace(R.id.nav_host_fragment, SelectExercisesFragment())
            addToBackStack(SelectExercisesFragment::class.java.simpleName)
        }
    }

    private fun onDemoAssessment() {
        (activity as? DemoAssessmentStarter)?.startDemoAssessment() ?: onStart2D()
    }
}

interface DemoAssessmentStarter {
    fun startDemoAssessment()
}

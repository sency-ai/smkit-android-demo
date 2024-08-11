package com.example.smkitdemoapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.replace
import com.example.smkitdemoapp.R
import com.example.smkitdemoapp.databinding.FragmentConfigureBinding
import com.example.smkitdemoapp.states.configure.ConfigureState
import com.example.smkitdemoapp.states.configure.Failed
import com.example.smkitdemoapp.states.configure.Loading
import com.example.smkitdemoapp.states.configure.Passed
import com.example.smkitdemoapp.viewModels.ActivityViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ConfigureFragment: Fragment() {

    private var _binding: FragmentConfigureBinding? = null
    private val binding get() = _binding!!

    private val activityViewModel: ActivityViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfigureBinding.inflate(inflater, container, false)
        observeConfigureState()
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activityViewModel.configure(binding.root.context)
    }

    private fun observeConfigureState() {
        activityViewModel.configureState.filterNotNull().onEach(::handleConfigureState).launchIn(
            CoroutineScope(Dispatchers.Main)
        )
    }

    private fun handleConfigureState(state: ConfigureState) {
        when (state) {
            Failed -> showFailureMessage()
            Loading -> showProgressBar()
            Passed -> {
                showPassedMessage()
                hideProgressBar()
                navigateToSelectionPage()
            }
        }
    }

    private fun navigateToSelectionPage() {
        parentFragmentManager.beginTransaction().apply {
            replace(R.id.nav_host_fragment, SelectExerciesFragment())
        }.commit()
    }


    private fun showPassedMessage() {
        Toast.makeText(
            binding.root.context,
            "configure Success",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showFailureMessage() {
        Toast.makeText(
            binding.root.context,
            "Failed to configure",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }
    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }


}
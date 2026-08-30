package com.example.smkitdemoapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

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
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeConfigureState()
        activityViewModel.configure(binding.root.context)
    }

    private fun observeConfigureState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                activityViewModel.configureState.filterNotNull().collect(::handleConfigureState)
            }
        }
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
            replace(R.id.nav_host_fragment, WelcomeFragment())
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

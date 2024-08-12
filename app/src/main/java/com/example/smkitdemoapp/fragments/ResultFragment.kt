package com.example.smkitdemoapp.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.smkitdemoapp.R
import com.example.smkitdemoapp.databinding.FragmentResultBinding
import com.example.smkitdemoapp.viewModels.ActivityViewModel
import com.google.gson.Gson
import com.google.gson.GsonBuilder

class ResultFragment: Fragment() {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    private val activityViewModel: ActivityViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        binding.copyButton.setOnClickListener {
            copyToClipboard(binding.resultsText.text.toString())
        }
        binding.startoverButton.setOnClickListener {
            parentFragmentManager.beginTransaction().apply {
                replace(R.id.nav_host_fragment, SelectExerciesFragment())
            }.commit()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activityViewModel.detectionSessionResultData?.let { data ->
            val gson = GsonBuilder().setPrettyPrinting().create()
            val jsonString = gson.toJson(data)
            binding.resultsText.text = jsonString
        }
    }

    private fun copyToClipboard(text: String) {
        // Get the ClipboardManager
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // Create a ClipData object with the text
        val clip = ClipData.newPlainText("Exercise JSON", text)

        // Set the ClipData to the ClipboardManager
        clipboard.setPrimaryClip(clip)

        // Optional: Show a confirmation message
        showToast("Text copied to clipboard")
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
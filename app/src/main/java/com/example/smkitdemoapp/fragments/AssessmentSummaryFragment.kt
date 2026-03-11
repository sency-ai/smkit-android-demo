package com.example.smkitdemoapp.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.replace
import com.example.smkitdemoapp.R
import com.example.smkitdemoapp.databinding.FragmentAssessmentSummaryBinding
import com.example.smkitdemoapp.models.AssessmentExerciseResult
import com.example.smkitdemoapp.viewModels.ActivityViewModel

class AssessmentSummaryFragment : Fragment() {

    private var _binding: FragmentAssessmentSummaryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ActivityViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAssessmentSummaryBinding.inflate(inflater, container, false)
        binding.summaryStartOver.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, WelcomeFragment())
                .commit()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val results = viewModel.getAssessmentResults()
        val overall = if (results.isEmpty()) 0 else results.map { it.techniqueScore }.reduce(Float::plus) / results.size
        binding.summaryOverallScore.text = overall.toInt().toString()
        binding.summaryOverallScore.setTextColor(scoreColor(overall.toInt()))

        val container = binding.summaryCardsContainer
        container.removeAllViews()
        results.forEach { result ->
            val card = createResultCard(result)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = 24
            container.addView(card, lp)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun scoreColor(score: Int): Int = when {
        score >= 80 -> Color.parseColor("#4CAF50")
        score >= 60 -> Color.parseColor("#FF9800")
        else -> Color.parseColor("#F44336")
    }

    private fun createResultCard(result: AssessmentExerciseResult): View {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 32)
            setBackgroundResource(R.drawable.rounded_container)
        }
        val title = TextView(requireContext()).apply {
            text = result.name
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        card.addView(title)

        if (result.feedbacks.isEmpty() && result.timeInPosition > 0) {
            val noIssues = TextView(requireContext()).apply {
                text = "No issues"
                setTextColor(Color.parseColor("#4CAF50"))
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
            }
            card.addView(noIssues)
        }

        val scoreBar = ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = result.techniqueScore.toInt().coerceIn(0, 100)
            progressDrawable?.setTint(scoreColor(result.techniqueScore.toInt()))
        }
        val barLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 24)
        barLp.topMargin = 12
        card.addView(scoreBar, barLp)

        val sub = TextView(requireContext()).apply {
            text = "Time in position: ${"%.1f".format(result.timeInPosition)}s"
            setTextColor(Color.GRAY)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
        }
        val subLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        subLp.topMargin = 8
        card.addView(sub, subLp)

        result.peakRom?.let { rom ->
            val romText = TextView(requireContext()).apply {
                text = if (rom >= 1f) "100% ROM" else "Peak ROM: ${(rom * 100).toInt()}%"
                setTextColor(if (rom >= 1f) Color.parseColor("#4CAF50") else Color.GRAY)
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
            }
            val romLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            romLp.topMargin = 4
            card.addView(romText, romLp)
        }

        if (result.feedbacks.isNotEmpty()) {
            val issuesLabel = TextView(requireContext()).apply {
                text = "Issues detected:"
                setTextColor(Color.GRAY)
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
            }
            val issuesLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            issuesLp.topMargin = 12
            card.addView(issuesLabel, issuesLp)
            result.feedbacks.forEach { feedback ->
                val line = TextView(requireContext()).apply {
                    text = "• $feedback"
                    setTextColor(Color.DKGRAY)
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
                }
                card.addView(line)
            }
        }

        return card
    }
}

package com.example.smkitdemoapp.models

/**
 * Per-exercise result for assessment summary (matches iOS AssessmentExerciseResult).
 */
data class AssessmentExerciseResult(
    val name: String,
    val techniqueScore: Float,   // 0–100
    val feedbacks: List<String>,
    val timeInPosition: Float,   // seconds
    val peakRom: Float? = null  // 0.0–1.0, null if no ROM
)

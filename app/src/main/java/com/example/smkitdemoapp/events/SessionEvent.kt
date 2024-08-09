package com.example.smkitdemoapp.events

sealed interface SessionEvent
data class ExerciseRepDetected(val repNumber: Int): SessionEvent
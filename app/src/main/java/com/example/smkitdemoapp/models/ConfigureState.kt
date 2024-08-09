package com.example.smkitdemoapp.models

data class ConfigureState (
    val configure: Boolean,
)

data class SessionState (
    val sessionRunning : Boolean
)

data class ExerciseState (
    val exerciseIndex: Int,
    val exerciseName: String,
    val exerciseRunning: Boolean
)
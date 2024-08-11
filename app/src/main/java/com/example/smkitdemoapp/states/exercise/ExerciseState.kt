package com.example.smkitdemoapp.states.exercise

sealed interface ExerciseState
data object Idle: ExerciseState
data class Playing(val exerciseName: String, var repCounter: Int = 0): ExerciseState

fun Playing.countRep(): Playing {
    return copy(repCounter = repCounter + 1)
}
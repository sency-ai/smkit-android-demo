package com.example.smkitdemoapp.states.exercise

sealed interface ExerciseState
data object Idle: ExerciseState
data class Playing(val exerciseName: String, var repCounter: Int = 0): ExerciseState

fun Playing.countRep() {
    repCounter += 1
}
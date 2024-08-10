package com.example.smkitdemoapp.states.configure

sealed interface ConfigureState
data object Loading: ConfigureState
data object Passed: ConfigureState
data object Failed: ConfigureState
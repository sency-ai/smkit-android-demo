package com.example.smkitdemoapp.states.session

sealed interface SessionState
data object Stopped: SessionState
data object Running: SessionState
data object Ready: SessionState
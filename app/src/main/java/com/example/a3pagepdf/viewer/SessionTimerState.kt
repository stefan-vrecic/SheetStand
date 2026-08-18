package com.example.a3pagepdf.viewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * State for the small elapsed-time stopwatch in each viewer's top bar.
 * Unrelated to [PageTimerState]'s per-page countdown — this one just counts
 * up while running. Starting and pausing both go through a confirmation
 * dialog; [pendingAction] tracks which one is awaiting a yes/no from the user.
 */
class SessionTimerState {
    var isRunning by mutableStateOf(false)
    var elapsedSeconds by mutableIntStateOf(0)
    var pendingAction by mutableStateOf<SessionTimerAction?>(null)
}

enum class SessionTimerAction { START, PAUSE }

@Composable
fun rememberSessionTimerState(): SessionTimerState = remember { SessionTimerState() }

/** Drives the count-up loop as a side effect while [SessionTimerState.isRunning]. */
@Composable
fun SessionTimerEffect(state: SessionTimerState) {
    LaunchedEffect(state.isRunning) {
        while (state.isRunning) {
            delay(1000)
            state.elapsedSeconds++
        }
    }
}

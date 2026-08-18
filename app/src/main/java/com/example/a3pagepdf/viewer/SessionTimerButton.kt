package com.example.a3pagepdf.viewer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Small session stopwatch for a viewer top bar — just a tappable text label,
 * no button chrome. Tapped while stopped/paused it asks to confirm starting;
 * tapped while running it asks to confirm pausing. Self-contained (owns its
 * own [SessionTimerState]) so it drops into any top bar with no wiring from
 * the hosting Activity.
 */
@Composable
fun SessionTimerButton(modifier: Modifier = Modifier) {
    val state = remember { SessionTimerState() }
    SessionTimerEffect(state)

    val label = if (state.elapsedSeconds > 0 || state.isRunning) formatTime(state.elapsedSeconds) else "Timer"
    Text(
        text = label,
        fontSize = 13.sp,
        modifier = modifier
            .clickable {
                state.pendingAction =
                    if (state.isRunning) SessionTimerAction.PAUSE else SessionTimerAction.START
            }
            .padding(horizontal = 3.dp, vertical = 4.dp)
    )

    state.pendingAction?.let { action ->
        val starting = action == SessionTimerAction.START
        AlertDialog(
            onDismissRequest = { state.pendingAction = null },
            title = { Text(if (starting) "Start timer?" else "Pause timer?") },
            confirmButton = {
                TextButton(onClick = {
                    state.isRunning = starting
                    state.pendingAction = null
                }) { Text(if (starting) "Start" else "Pause") }
            },
            dismissButton = {
                TextButton(onClick = { state.pendingAction = null }) { Text("Cancel") }
            }
        )
    }
}

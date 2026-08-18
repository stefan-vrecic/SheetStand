package com.example.a3pagepdf.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Small floating pill showing the metronome's beat lights, meant to stay
 * visible even when the rest of the top bar is hidden in fullscreen mode.
 * Tapping it pauses/resumes the metronome without needing to leave
 * fullscreen to reach the dropdown control.
 */
@Composable
fun MetronomeOverlay(state: MetronomeState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(12.dp)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { state.isPaused = !state.isPaused }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MetronomeBeatLights(state)
    }
}

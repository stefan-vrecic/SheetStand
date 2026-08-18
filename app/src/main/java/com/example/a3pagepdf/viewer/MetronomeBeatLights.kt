package com.example.a3pagepdf.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Inline row of beat-indicator dots, dimmed while the metronome is paused. */
@Composable
fun MetronomeBeatLights(state: MetronomeState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.graphicsLayer(alpha = if (state.isPaused) 0.4f else 1f)
    ) {
        for (beat in 0 until state.beatsPerMeasure) {
            val isActive = beat == state.currentBeat
            val isAccent = beat == 0

            val dotColor = when {
                isActive && isAccent -> Color(0xFFE53935)
                isActive -> MaterialTheme.colorScheme.primary
                isAccent -> Color(0xFFFFCDD2)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (isAccent) 22.dp else 18.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

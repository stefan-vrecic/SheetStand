package com.example.a3pagepdf.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Small floating pill showing the metronome's beat lights, meant to stay
 * visible even when the rest of the top bar is hidden in fullscreen mode.
 * Tapping it pauses/resumes the metronome without needing to leave
 * fullscreen to reach the dropdown control.
 *
 * Uses unicode play/pause glyphs rather than Material icons: the extended
 * icon set (which has a real Pause glyph) isn't a dependency here, and the
 * rest of the app's controls already lean on unicode symbols (e.g. the
 * "◀ Prev" / "Next ▶" buttons) instead of pulling it in for one icon.
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
        // Paused shows ▶ (tap to resume); playing shows ⏸ (tap to pause).
        Text(
            text = if (state.isPaused) "▶" else "⏸",
            fontSize = 16.sp,
            modifier = Modifier.padding(end = 8.dp)
        )
        MetronomeBeatLights(state)
    }
}

package com.example.a3pagepdf.viewer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private const val ACCENT_ALPHA = 0.55f
private const val REGULAR_ALPHA = 0.35f
private const val PAUSED_ALPHA_SCALE = 0.35f
private const val RING_WIDTH_DP = 14

/**
 * Big radiant circle, centered on whatever area it's layered over, that
 * pulses outward once per beat — an opt-in alternative to
 * [MetronomeBeatLights]' small dots (see [MetronomeState.bigPulseEnabled])
 * for practicing at a distance from the screen, where a row of 18dp dots is
 * too small to track out of the corner of your eye. Purely visual: a bare
 * [Canvas] has no gesture handlers of its own, so it doesn't intercept the
 * scroll/tap/drag gestures on whatever it's drawn over.
 */
@Composable
fun MetronomeRadiantOverlay(state: MetronomeState, modifier: Modifier = Modifier) {
    // 0f (just triggered, centre point) -> 1f (fully expanded, faded out).
    // Re-triggered every beat by snapping back to 0 and animating out again,
    // so the *motion* reads as the beat rather than just a color flash.
    val progress = remember { Animatable(1f) }
    val isAccent = state.currentBeat == 0

    LaunchedEffect(state.currentBeat) {
        if (state.isPaused) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = pulseDurationMs(state.bpm),
                easing = LinearOutSlowInEasing
            )
        )
    }

    val accentColor = Color(0xFFE53935)
    val regularColor = MaterialTheme.colorScheme.primary
    val baseAlpha = if (isAccent) ACCENT_ALPHA else REGULAR_ALPHA
    val pausedScale = if (state.isPaused) PAUSED_ALPHA_SCALE else 1f

    Canvas(modifier = modifier) {
        val maxRadius = size.minDimension / 2f
        val radius = maxRadius * progress.value
        val alpha = (1f - progress.value) * baseAlpha * pausedScale
        val ringWidthPx = RING_WIDTH_DP.dp.toPx()
        if (alpha > 0f && radius > 0f) {
            // A stroked ring, not a filled disc: a full-screen-sized filled
            // circle repainted every animation frame (~60fps for up to
            // ~900ms, every single beat) was heavy enough fill-rate work to
            // visibly steal frame budget from the autoscroll loop running
            // concurrently — the ring covers a fraction of the pixels for
            // the same visual "expanding pulse" read.
            drawCircle(
                color = if (isAccent) accentColor else regularColor,
                radius = radius,
                alpha = alpha,
                center = center,
                style = Stroke(width = ringWidthPx)
            )
        }
    }
}

/**
 * Pulse expands over roughly 70% of the beat interval — long enough to read
 * clearly, short enough that it's fully faded before the next beat lands even
 * at fast tempos. Clamped to a sane range for very slow/fast BPM extremes.
 */
private fun pulseDurationMs(bpm: Float): Int {
    val beatMs = 60000f / bpm
    return (beatMs * 0.7f).toInt().coerceIn(120, 900)
}

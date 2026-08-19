package com.example.a3pagepdf.viewer

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * State for the continuous pixel-based auto-scroll feature, plus optional
 * "boundary jump" correction to snap past page-divider gaps as each page
 * boundary enters the viewport from the bottom.
 */
class AutoScrollState {
    var isPlaying by mutableStateOf(false)
    var speed by mutableFloatStateOf(40f)

    // Text-field mirror of `speed`, kept in sync both ways: dragging the
    // slider reformats this, and typing a valid value updates `speed`. Split
    // out (rather than deriving the field's text from `speed` on every
    // recomposition) so mid-typing states like "1" don't get clobbered back
    // to a clamped/reformatted value before the user finishes typing.
    var speedText by mutableStateOf(speed.toInt().toString())

    var delayEnabled by mutableStateOf(false)

    // Captures whether the delay was checked at the exact moment Play was
    // pressed, since the checkbox itself gets reset immediately after.
    var shouldDelayThisRun by mutableStateOf(false)
    var delayCountdown by mutableStateOf<Int?>(null)

    var jumpPxText by mutableStateOf("0")
    var lastJumpAppliedForIndex by mutableStateOf(-1)

    // Practice-tempo scaler, kept separate from `speed` itself (rather than
    // baked into it) so the speed box/slider still show the player's actual
    // chosen px/s — tempo is a multiplier on top, the way a metronome app's
    // "% tempo" knob doesn't rewrite the base BPM.
    var tempoMultiplierText by mutableStateOf("1")
    val tempoMultiplier: Float
        get() = tempoMultiplierText.toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f

    fun togglePlay(onPlayStarted: () -> Unit = {}) {
        isPlaying = !isPlaying
        if (isPlaying) {
            shouldDelayThisRun = delayEnabled // capture before resetting
            delayEnabled = false
            onPlayStarted()
        }
    }

    fun stop() {
        isPlaying = false
    }
}

@Composable
fun rememberAutoScrollState(): AutoScrollState = remember { AutoScrollState() }

/**
 * Drives the actual scrolling loop as a side effect. Call once, alongside a
 * rememberAutoScrollState() instance and the LazyListState it should scroll.
 */
@Composable
fun AutoScrollEffect(state: AutoScrollState, listState: LazyListState) {
    LaunchedEffect(state.isPlaying, state.speed, state.tempoMultiplierText, state.shouldDelayThisRun) {
        if (!state.isPlaying) {
            state.delayCountdown = null
            return@LaunchedEffect
        }
        try {
            if (state.shouldDelayThisRun) {
                for (i in 3 downTo 1) {
                    state.delayCountdown = i
                    delay(1000)
                }
            }
            state.delayCountdown = null

            // Boundary-jump correction is now a smooth speed ramp rather than an
            // instant snap: once triggered, the effective speed is boosted for
            // `rampSeconds` so the extra `jumpPx` worth of distance accumulates
            // gradually instead of jumping the page in one frame. `rampSeconds`
            // is deliberately 4x the time jumpPx would take at the current base
            // speed — solving `boostExtraSpeed * rampSeconds == jumpPx` for that
            // ratio gives `boostExtraSpeed == speed / 4`, a gentle ~25% speed-up
            // held for longer rather than a jarring, brief burst.
            var boostExtraSpeed = 0f
            var boostRemainingSeconds = 0f

            while (state.isPlaying) {
                delay(16)
                val dtSeconds = 16f / 1000f

                if (boostRemainingSeconds > 0f) {
                    boostRemainingSeconds -= dtSeconds
                    if (boostRemainingSeconds <= 0f) {
                        boostRemainingSeconds = 0f
                        boostExtraSpeed = 0f
                    }
                }

                // Tempo scales the base speed only; the jump-ramp math below is
                // derived from this already-scaled value too, so a slower
                // practice tempo gets a proportionally slower (not oversized)
                // boundary-jump boost.
                val tempoSpeed = state.speed * state.tempoMultiplier

                val step = (tempoSpeed + boostExtraSpeed) * dtSeconds
                listState.scrollBy(step)

                // ---- Boundary-jump correction ----
                // If the topmost visible page's bottom edge (== next page's top
                // edge) is within jumpPx pixels of entering the viewport from the
                // bottom, kick off a ramp that covers jumpPx over the next
                // `rampSeconds`, once per page transition. Skipped while a ramp
                // is already in progress so overlapping boundaries can't stomp
                // on each other's boost, and while tempoSpeed is 0 (tempo
                // multiplier at 0 pauses scrolling entirely — nothing to ramp).
                val jumpPx = state.jumpPxText.toIntOrNull() ?: 0
                if (jumpPx > 0 && boostRemainingSeconds <= 0f && tempoSpeed > 0f) {
                    val topItem = listState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.offset + it.size > 0 }
                    if (topItem != null) {
                        val viewportEnd = listState.layoutInfo.viewportEndOffset
                        val distanceToBoundary = (topItem.offset + topItem.size) - viewportEnd
                        if (distanceToBoundary in 0..jumpPx &&
                            state.lastJumpAppliedForIndex != topItem.index
                        ) {
                            val rampSeconds = (jumpPx / tempoSpeed) * 4f
                            boostRemainingSeconds = rampSeconds
                            boostExtraSpeed = jumpPx / rampSeconds
                            state.lastJumpAppliedForIndex = topItem.index
                        }
                    }
                }
            }
        } finally {
            state.delayCountdown = null
        }
    }
}
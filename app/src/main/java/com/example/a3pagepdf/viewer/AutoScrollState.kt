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
 * "boundary jump" correction to snap past page-divider gaps.
 */
class AutoScrollState {
    var isPlaying by mutableStateOf(false)
    var speed by mutableFloatStateOf(40f)
    var delayEnabled by mutableStateOf(false)

    // Captures whether the delay was checked at the exact moment Play was
    // pressed, since the checkbox itself gets reset immediately after.
    var shouldDelayThisRun by mutableStateOf(false)
    var delayCountdown by mutableStateOf<Int?>(null)

    var jumpPxText by mutableStateOf("0")
    var lastJumpAppliedForIndex by mutableStateOf(-1)

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
    LaunchedEffect(state.isPlaying, state.speed, state.shouldDelayThisRun) {
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
            while (state.isPlaying) {
                delay(16)
                val step = state.speed * (16f / 1000f)
                listState.scrollBy(step)

                // ---- Boundary-jump correction ----
                // If the topmost visible page's bottom edge (== next page's top
                // edge) is within jumpPx pixels of reaching the viewport top,
                // snap forward by jumpPx once per page transition.
                val jumpPx = state.jumpPxText.toIntOrNull() ?: 0
                if (jumpPx > 0) {
                    val topItem = listState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.offset + it.size > 0 }
                    if (topItem != null) {
                        val distanceToBoundary = topItem.offset + topItem.size
                        if (distanceToBoundary in 0..jumpPx &&
                            state.lastJumpAppliedForIndex != topItem.index
                        ) {
                            listState.scrollBy(jumpPx.toFloat())
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
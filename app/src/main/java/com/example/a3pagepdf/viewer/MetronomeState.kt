package com.example.a3pagepdf.viewer

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * State for the metronome feature: BPM, time signature, current beat (for
 * the beat-light UI), tap tempo, and audible clicks via ToneGenerator.
 */
class MetronomeState {
    var isOn by mutableStateOf(false)
    var isPaused by mutableStateOf(false)
    var bpm by mutableFloatStateOf(120f)
    var timeSigNumerator by mutableStateOf("4")
    var timeSigDenominator by mutableStateOf("4")
    var currentBeat by mutableIntStateOf(0)
    var menuExpanded by mutableStateOf(false)

    // Opt-in alternative to the small beat-light dots (MetronomeBeatLights):
    // a big pulsing circle radiating from the centre of the page (see
    // MetronomeRadiantOverlay) for practicing at a distance where the dots
    // are too small to track. Off by default so existing behavior is unchanged.
    var bigPulseEnabled by mutableStateOf(false)

    val tapTimestamps = mutableStateListOf<Long>()
    var tapFeedback by mutableStateOf("Tap Tempo")

    val beatsPerMeasure: Int
        get() = timeSigNumerator.toIntOrNull()?.coerceIn(1, 12) ?: 4

    /**
     * Registers a tap and re-estimates BPM from recent tap intervals.
     * Resets the tap history if there's been a long pause (> 2s) since the last tap.
     */
    fun registerTap() {
        val now = System.currentTimeMillis()
        if (tapTimestamps.isNotEmpty() && now - tapTimestamps.last() > 2000L) {
            tapTimestamps.clear()
        }
        tapTimestamps.add(now)
        if (tapTimestamps.size > 8) {
            tapTimestamps.removeAt(0)
        }

        if (tapTimestamps.size >= 2) {
            val intervals = tapTimestamps.zipWithNext { a, b -> b - a }
            val avgIntervalMs = intervals.average()
            val estimatedBpm = (60000.0 / avgIntervalMs).toFloat().coerceIn(40f, 240f)
            bpm = estimatedBpm
            tapFeedback = "Tap Tempo (${estimatedBpm.toInt()} BPM)"
        } else {
            tapFeedback = "Tap again…"
        }
    }

    fun onBpmSliderChanged(newBpm: Float) {
        bpm = newBpm
        tapTimestamps.clear()
        tapFeedback = "Tap Tempo"
    }
}

@Composable
fun rememberMetronomeState(): MetronomeState = remember { MetronomeState() }

/** Drives the beat-timing loop and audible clicks as a side effect. */
@Composable
fun MetronomeEffect(state: MetronomeState) {
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 90) }
    DisposableEffect(Unit) {
        onDispose { toneGenerator.release() }
    }

    fun playClick(isAccent: Boolean) {
        val tone = if (isAccent) ToneGenerator.TONE_CDMA_PIP else ToneGenerator.TONE_PROP_BEEP
        toneGenerator.startTone(tone, 50)
    }

    LaunchedEffect(state.isOn, state.bpm, state.beatsPerMeasure) {
        if (!state.isOn) return@LaunchedEffect
        state.currentBeat = 0
        if (!state.isPaused) playClick(true) // first beat, immediately
        while (state.isOn) {
            if (state.isPaused) {
                delay(100) // idle poll while paused; lights stay frozen on screen
                continue
            }
            val intervalMs = (60000f / state.bpm).toLong().coerceAtLeast(50L)
            delay(intervalMs)
            if (!state.isOn) break
            if (state.isPaused) continue // paused mid-wait; skip this beat
            state.currentBeat = (state.currentBeat + 1) % state.beatsPerMeasure
            playClick(state.currentBeat == 0)
        }
    }
}
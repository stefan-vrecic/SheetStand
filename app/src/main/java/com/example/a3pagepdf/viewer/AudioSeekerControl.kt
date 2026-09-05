package com.example.a3pagepdf.viewer

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Compact inline placement for the top bar row: just an "Audio" button,
 * nothing once a track is loaded. Deliberately tiny and fixed-width (no
 * `weight`) — the full player (play/pause, scrub, speed, reload) lives in
 * [AudioSeekerExpandedRow] on its own full-width line instead of cramming
 * into the already-packed top bar row (Open/Jump/Prev/Next/timer/metronome/
 * etc. all fought this control for space when it tried to live inline).
 */
@Composable
fun AudioSeekerControl(state: AudioSeekerState, modifier: Modifier = Modifier) {
    if (state.mediaPlayer != null) return

    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) loadAudioIntoState(context, state, uri)
        else Log.d("AudioSeeker", "picker returned no uri (cancelled)")
    }

    CompactButton(onClick = { picker.launch("audio/*") }, modifier = modifier) {
        Text("Audio")
    }
}

/**
 * The full backing-track player — play/pause, A/B-loop scrub bar, time,
 * practice speed, reload — meant for its own full-width row beneath the top
 * bar, shown only while [AudioSeekerState.mediaPlayer] is non-null (the
 * caller decides where that row goes; see [AudioSeekerControl] for the
 * always-present inline "Audio" trigger). Splitting these two apart is
 * what fixed the top bar wrapping onto multiple lines once a track was
 * loaded — [state] still has to be hoisted to the Activity either way (see
 * [AudioSeekerState]'s own doc) so both pieces share the same session.
 */
@Composable
fun AudioSeekerExpandedRow(state: AudioSeekerState, modifier: Modifier = Modifier) {
    val player = state.mediaPlayer ?: return
    val context = LocalContext.current

    // While the user's actively dragging, show their drag position instead of
    // the polled one — otherwise the next poll tick fights the drag gesture.
    var dragPositionMs by remember { mutableStateOf<Int?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) loadAudioIntoState(context, state, uri)
        else Log.d("AudioSeeker", "picker returned no uri (cancelled)")
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        CompactButton(onClick = {
            if (state.isPlaying) player.pause() else player.start()
            state.isPlaying = !state.isPlaying
        }) { Text(if (state.isPlaying) "⏸" else "▶") }

        Spacer(modifier = Modifier.width(8.dp))

        AudioLoopSeeker(
            state = state,
            seekerPositionMs = dragPositionMs ?: state.positionMs,
            onSeekerPositionChange = { dragPositionMs = it },
            onSeekerChangeFinished = {
                val target = dragPositionMs
                if (target != null) {
                    player.seekTo(target)
                    state.positionMs = target
                }
                dragPositionMs = null
            },
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "${formatMs(dragPositionMs ?: state.positionMs)} / ${formatMs(state.durationMs)}",
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.width(8.dp))

        SpeedControl(state)

        Spacer(modifier = Modifier.width(8.dp))

        // Small "swap file" affordance — a second tap target rather than
        // repurposing the play/pause button, so changing tracks mid-playback
        // is a deliberate action, not an accidental double-function of Play.
        Text(
            text = "⟲",
            fontSize = 16.sp,
            modifier = Modifier.clickable { picker.launch("audio/*") }
        )
    }
}

/** Shared by both [AudioSeekerControl]'s and [AudioSeekerExpandedRow]'s file pickers. */
private fun loadAudioIntoState(context: Context, state: AudioSeekerState, uri: Uri) {
    Log.d("AudioSeeker", "loading $uri")
    state.release()
    val player = MediaPlayer()
    try {
        player.setDataSource(context, uri)
        player.prepare() // content:// / local file — fast enough not to need prepareAsync
        player.setOnCompletionListener {
            state.isPlaying = false
            state.positionMs = 0
        }
        state.mediaPlayer = player
        state.durationMs = player.duration
        state.positionMs = 0
        state.initialiseLoop(player.duration)
        state.setSpeed(state.speedMultiplier) // re-apply the persisted practice speed to the fresh player
        Log.d("AudioSeeker", "loaded, duration=${player.duration}ms")
    } catch (e: Exception) {
        Log.w("AudioSeeker", "failed to load $uri", e)
        player.release()
    }
}

private enum class LoopMarker { A, B }

/**
 * The normal playback seeker with two draggable A/B handles layered over it.
 * The handle labels remain compact and visible, while their timestamp appears
 * only during a drag so the top bar does not become permanently cluttered.
 */
@Composable
private fun AudioLoopSeeker(
    state: AudioSeekerState,
    seekerPositionMs: Int,
    onSeekerPositionChange: (Int) -> Unit,
    onSeekerChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var draggedMarker by remember { mutableStateOf<LoopMarker?>(null) }
    var dragTimeMs by remember { mutableIntStateOf(0) }

    BoxWithConstraints(modifier = modifier.height(52.dp)) {
        val duration = state.durationMs.coerceAtLeast(1)
        val density = LocalDensity.current
        val trackWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)

        Slider(
            value = seekerPositionMs.coerceIn(0, duration).toFloat(),
            onValueChange = { onSeekerPositionChange(it.toInt()) },
            onValueChangeFinished = onSeekerChangeFinished,
            valueRange = 0f..duration.toFloat(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )

        val startFraction = state.loopStartMs.toFloat() / duration
        val endFraction = state.loopEndMs.toFloat() / duration
        LoopHandle(
            label = "A",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = maxWidth * startFraction - 10.dp),
            onDragStart = {
                draggedMarker = LoopMarker.A
                dragTimeMs = state.loopStartMs
            },
            onDrag = { deltaPx ->
                val updated = (state.loopStartMs + deltaPx / trackWidthPx * duration)
                    .roundToInt()
                state.setLoopStart(updated)
                dragTimeMs = state.loopStartMs
            },
            onDragEnd = { draggedMarker = null }
        )
        LoopHandle(
            label = "B",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = maxWidth * endFraction - 10.dp),
            onDragStart = {
                draggedMarker = LoopMarker.B
                dragTimeMs = state.loopEndMs
            },
            onDrag = { deltaPx ->
                val updated = (state.loopEndMs + deltaPx / trackWidthPx * duration)
                    .roundToInt()
                state.setLoopEnd(updated)
                dragTimeMs = state.loopEndMs
            },
            onDragEnd = { draggedMarker = null }
        )

        draggedMarker?.let { marker ->
            Text(
                text = "$marker ${formatMs(dragTimeMs)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

/** A compact touch handle over the seeker track, used by [AudioLoopSeeker]. */
@Composable
private fun LoopHandle(
    label: String,
    onDragStart: () -> Unit,
    onDrag: (deltaPx: Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        fontSize = 10.sp,
        color = Color.White,
        modifier = modifier
            .size(20.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
            .pointerInput(label) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd
                ) { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x)
                }
            }
            .padding(top = 3.dp)
    )
}

/**
 * Tappable practice-speed chip for the audio backing track. A visible dropdown
 * is used instead of a hidden drag gesture so the available 4% increments are
 * discoverable and selecting a speed is reliable on touch screens.
 */
@Composable
private fun SpeedControl(state: AudioSeekerState) {
    var expanded by remember { mutableStateOf(false) }
    // The playback state snaps values to multiples of 0.04, so deriving the
    // menu from those same multiples means every visible choice is exact.
    val speedOptions = remember { (13..50).map { it * SPEED_STEP } }

    Box {
        Text(
            text = "%.2fx".format(state.speedMultiplier),
            fontSize = 12.sp,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            speedOptions.forEach { speed ->
                DropdownMenuItem(
                    text = { Text("%.2fx".format(speed)) },
                    onClick = {
                        state.setSpeed(speed)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun formatMs(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

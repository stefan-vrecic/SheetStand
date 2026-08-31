package com.example.a3pagepdf.viewer

import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Basic backing-track player for the top bar: load an mp3/wav, play/pause,
 * scrub. Takes [state] from the caller rather than owning it — unlike most
 * self-contained top-bar controls in this app, this one has to survive the
 * top bar being hidden entirely (tap-to-fullscreen), the same reason
 * MetronomeState is hoisted to the Activity rather than remembered inside
 * MetronomeControl. If this composable owned its own `remember`ed state
 * instead, toggling fullscreen would remove it from composition and dispose
 * that state — silently releasing the loaded track and resetting the speed
 * back to 1.00x every time. Deliberately minimal otherwise (no queue, no
 * looping, no waveform) — "just basic seeker" per the ask.
 *
 * Sits where a plain `Spacer(Modifier.weight(1f))` used to (see call sites)
 * so it naturally claims whatever room the metronome's beat-lights aren't
 * using — more when they're hidden (metronome off), less when they're shown.
 */
@Composable
fun AudioSeekerControl(state: AudioSeekerState, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // While the user's actively dragging, show their drag position instead of
    // the polled one — otherwise the next poll tick fights the drag gesture.
    var dragPositionMs by remember { mutableStateOf<Int?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) {
            Log.d("AudioSeeker", "picker returned no uri (cancelled)")
            return@rememberLauncherForActivityResult
        }
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
            state.setSpeed(state.speedMultiplier) // re-apply the persisted practice speed to the fresh player
            Log.d("AudioSeeker", "loaded, duration=${player.duration}ms")
        } catch (e: Exception) {
            Log.w("AudioSeeker", "failed to load $uri", e)
            player.release()
        }
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        val player = state.mediaPlayer
        if (player == null) {
            CompactButton(onClick = { picker.launch("audio/*") }) { Text("Load Audio") }
        } else {
            CompactButton(onClick = {
                if (state.isPlaying) player.pause() else player.start()
                state.isPlaying = !state.isPlaying
            }) { Text(if (state.isPlaying) "⏸" else "▶") }

            Spacer(modifier = Modifier.width(8.dp))

            Slider(
                value = (dragPositionMs ?: state.positionMs).toFloat(),
                onValueChange = { dragPositionMs = it.toInt() },
                onValueChangeFinished = {
                    val target = dragPositionMs
                    if (target != null) {
                        player.seekTo(target)
                        state.positionMs = target
                    }
                    dragPositionMs = null
                },
                valueRange = 0f..state.durationMs.coerceAtLeast(1).toFloat(),
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

package com.example.a3pagepdf.viewer

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val POSITION_POLL_MILLIS = 250L
private const val LOOP_POLL_MILLIS = 50L
private const val MIN_LOOP_LENGTH_MILLIS = 100

const val SPEED_STEP = 0.04f
const val SPEED_MIN = 0.5f
const val SPEED_MAX = 2.0f
private const val DEFAULT_SPEED = 1.0f

/**
 * State for the basic play/scrub audio player in TwoPageActivity/
 * ThreePageActivity's top bar — a backing track played alongside the sheet
 * music, unrelated to the PDF itself (nothing here persists across a file
 * re-open). Owns a [MediaPlayer] directly rather than pulling in
 * ExoPlayer/Media3: this app has no other audio/video playback need that
 * would justify the extra dependency, and MediaPlayer's synchronous API is a
 * fine fit for "play one local file with a scrub bar."
 */
class AudioSeekerState {
    // by mutableStateOf, not a plain var — AudioSeekerControl's UI branches on
    // whether this is null (showing "Load Audio" vs. the play/scrub row), and
    // a plain var's reassignment is invisible to Compose: it would compile and
    // "work" (the MediaPlayer really does load) while the UI silently never
    // updates to show it, because nothing observable changed as far as the
    // composition is concerned.
    var mediaPlayer: MediaPlayer? by mutableStateOf(null)
    var isPlaying by mutableStateOf(false)
    var durationMs by mutableIntStateOf(0)
    var positionMs by mutableIntStateOf(0)

    // Markers are always visible once a track is loaded, but looping does not
    // start until the player deliberately moves either marker. That avoids
    // making a newly loaded track unexpectedly repeat from beginning to end.
    var loopStartMs by mutableIntStateOf(0)
        private set
    var loopEndMs by mutableIntStateOf(0)
        private set
    var isLoopEnabled by mutableStateOf(false)
        private set

    // Deliberately NOT reset by release() — a practice speed picked for one
    // track is a reasonable default for the next one loaded too, rather than
    // silently snapping back to 1.00x every time you swap files.
    var speedMultiplier by mutableFloatStateOf(DEFAULT_SPEED)

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
        durationMs = 0
        positionMs = 0
        loopStartMs = 0
        loopEndMs = 0
        isLoopEnabled = false
    }

    /** Initialises the visible A/B markers without automatically enabling looping. */
    fun initialiseLoop(duration: Int) {
        loopStartMs = 0
        loopEndMs = duration
        isLoopEnabled = false
    }

    /** Moves marker A while preserving a meaningful interval before marker B. */
    fun setLoopStart(position: Int) {
        loopStartMs = position.coerceIn(0, (loopEndMs - MIN_LOOP_LENGTH_MILLIS).coerceAtLeast(0))
        isLoopEnabled = loopEndMs - loopStartMs >= MIN_LOOP_LENGTH_MILLIS
    }

    /** Moves marker B while preserving a meaningful interval after marker A. */
    fun setLoopEnd(position: Int) {
        loopEndMs = position.coerceIn(
            (loopStartMs + MIN_LOOP_LENGTH_MILLIS).coerceAtMost(durationMs),
            durationMs
        )
        isLoopEnabled = loopEndMs - loopStartMs >= MIN_LOOP_LENGTH_MILLIS
    }

    /**
     * Clamps to [SPEED_MIN]..[SPEED_MAX], snaps to the nearest [SPEED_STEP]
     * (so repeated small drag adjustments can't drift off the 4% grid), and
     * — if a player is currently loaded — applies it immediately.
     * [MediaPlayer.setPitch] is pinned to 1f alongside the speed change:
     * without that, PlaybackParams shifts pitch proportionally to speed,
     * which is exactly wrong for a practice-speed control on a music app —
     * slowing a backing track down shouldn't drop it an octave.
     */
    fun setSpeed(newSpeed: Float) {
        val clamped = newSpeed.coerceIn(SPEED_MIN, SPEED_MAX)
        val stepped = (clamped / SPEED_STEP).roundToInt() * SPEED_STEP
        speedMultiplier = stepped
        val player = mediaPlayer ?: return
        try {
            player.playbackParams = player.playbackParams.setSpeed(stepped).setPitch(1f)
        } catch (e: Exception) {
            // Some OEM MediaPlayer implementations reject certain
            // PlaybackParams combinations — the label still updates and the
            // speed takes effect next time playback (re)starts, so this
            // fails soft rather than crashing.
        }
    }
}

@Composable
fun rememberAudioSeekerState(): AudioSeekerState = remember { AudioSeekerState() }

/**
 * Polls [MediaPlayer.getCurrentPosition] while playing (it has no
 * position-changed callback of its own) and releases the player when this
 * leaves composition — i.e. when the hosting Activity goes away.
 */
@Composable
fun AudioSeekerEffect(state: AudioSeekerState) {
    LaunchedEffect(state.isPlaying) {
        while (state.isPlaying) {
            val player = state.mediaPlayer ?: break
            val currentPosition = player.currentPosition
            if (state.isLoopEnabled && currentPosition >= state.loopEndMs) {
                player.seekTo(state.loopStartMs)
                state.positionMs = state.loopStartMs
            } else {
                state.positionMs = currentPosition
            }
            // Looping needs a tighter check than ordinary progress display;
            // otherwise a fast backing track can audibly overshoot B before
            // the next poll sends it back to A.
            delay(if (state.isLoopEnabled) LOOP_POLL_MILLIS else POSITION_POLL_MILLIS)
        }
    }

    DisposableEffect(Unit) {
        onDispose { state.release() }
    }
}

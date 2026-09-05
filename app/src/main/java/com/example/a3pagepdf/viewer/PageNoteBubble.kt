package com.example.a3pagepdf.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val BASE_NOTE_FONT_SP = 13
internal const val MIN_NOTE_SCALE = 0.5f
internal const val MAX_NOTE_SCALE = 3f
private const val HANDLE_IDLE_MILLIS = 4000L

/**
 * One placed text note (used by [PdfPageList]): draggable by its body
 * (updates [onMove] live, then [onMoveEnd] once released), resizable via the
 * small corner handle (updates [onResize]/[onResizeEnd] the same way). The
 * resize handle fades out after [HANDLE_IDLE_MILLIS] of no interaction with
 * this note, to stay out of the way once you're done placing it; tapping the
 * note wakes it back up and reports itself via [onTap] as the
 * most-recently-tapped note — that's what the top bar's trash icon deletes,
 * not by tapping the note directly. Drag detection uses touch-slop
 * internally, so a tap that doesn't move enough never reaches [onMove] — the
 * tap detector (a separate pointerInput below) handles that case instead.
 */
@Composable
fun PageNoteBubble(
    note: PageNote,
    containerSizePx: IntSize,
    onMove: (dxFraction: Float, dyFraction: Float) -> Unit,
    onMoveEnd: () -> Unit,
    onResize: (dScale: Float) -> Unit,
    onResizeEnd: () -> Unit,
    onTap: () -> Unit
) {
    var handleVisible by remember(note.id) { mutableStateOf(true) }
    var activityToken by remember(note.id) { mutableStateOf(0) }

    // Restarts every time activityToken changes, so any fresh interaction
    // pushes the auto-hide back out instead of hiding mid-use.
    LaunchedEffect(activityToken) {
        delay(HANDLE_IDLE_MILLIS)
        handleVisible = false
    }

    fun wakeHandle() {
        handleVisible = true
        activityToken++
    }

    Box(
        modifier = Modifier.offset {
            IntOffset(
                (note.xFraction * containerSizePx.width).toInt(),
                (note.yFraction * containerSizePx.height).toInt()
            )
        }
    ) {
        Text(
            text = note.text,
            fontSize = (BASE_NOTE_FONT_SP * note.scale).sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(4.dp)
                )
                .pointerInput(note.id) {
                    detectTapGestures(onTap = { wakeHandle(); onTap() })
                }
                .pointerInput(note.id, containerSizePx) {
                    detectDragGestures(
                        onDragStart = { wakeHandle() },
                        onDragEnd = { onMoveEnd(); wakeHandle() }
                    ) { change, dragAmount ->
                        change.consume()
                        if (containerSizePx.width > 0 && containerSizePx.height > 0) {
                            onMove(
                                dragAmount.x / containerSizePx.width,
                                dragAmount.y / containerSizePx.height
                            )
                        }
                    }
                }
                .padding(horizontal = 6.dp, vertical = 3.dp)
        )

        // Resize handle — offset slightly outside the bubble's corner so it doesn't
        // overlap the delete/wake touch area of the text itself. Fades out when idle.
        AnimatedVisibility(
            visible = handleVisible,
            modifier = Modifier.align(Alignment.BottomEnd),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .offset(x = 6.dp, y = 6.dp)
                    .size(18.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .pointerInput(note.id) {
                        detectDragGestures(
                            onDragStart = { wakeHandle() },
                            onDragEnd = { onResizeEnd(); wakeHandle() }
                        ) { change, dragAmount ->
                            change.consume()
                            onResize(dragAmount.x / 150f)
                        }
                    }
            )
        }
    }
}

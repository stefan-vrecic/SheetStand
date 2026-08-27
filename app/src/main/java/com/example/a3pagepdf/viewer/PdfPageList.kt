package com.example.a3pagepdf.viewer

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** A tap has landed on a page while its pencil was armed; waiting on the
 * text-entry dialog before it turns into a persisted [PageNote]. */
private data class PendingNotePlacement(val page: Int, val xFraction: Float, val yFraction: Float)

private const val BASE_NOTE_FONT_SP = 13
private const val MIN_NOTE_SCALE = 0.5f
private const val MAX_NOTE_SCALE = 3f
private const val HANDLE_IDLE_MILLIS = 4000L
private const val TAG = "PageNotes"

/**
 * Continuous, full-width scrolling list of every page in the PDF, used by
 * auto-scroll mode. Each page also gets a small "Note" chip (top-left) that
 * arms "add text" mode for that page: the next tap on the sheet drops a note
 * there via a small dialog. [armedPage]/[onArmedPageChange] are hoisted to the
 * caller so the top bar's own button (a bigger, easier-to-hit target that
 * arms whichever page is currently on screen) can drive the exact same
 * state — same arm/tap/dialog flow either way. Placed notes can be dragged by
 * their body and resized via the corner handle; tapping one reports it up via
 * [onNoteTapped] as the most-recently-tapped note. Deleting is done from the
 * top bar's trash icon, which removes that note ([notesRefreshToken] bumps
 * whenever that happens, so this list reloads from [PageNoteStore] to reflect
 * it). Notes persist per-PDF via [PageNoteStore].
 */
@Composable
fun PdfPageList(
    pageSource: PdfPageSource,
    listState: LazyListState,
    armedPage: Int?,
    onArmedPageChange: (Int?) -> Unit,
    notesRefreshToken: Int,
    onNoteTapped: (String) -> Unit,
    onRenameRequested: () -> Unit = {}
) {
    val context = LocalContext.current
    val uri = pageSource.currentUri

    var notes by remember(uri, notesRefreshToken) {
        mutableStateOf(uri?.let { PageNoteStore.load(context, it) } ?: emptyList())
    }
    var pendingPlacement by remember(uri) { mutableStateOf<PendingNotePlacement?>(null) }

    // Live (in-memory) edits from dragging/resizing; committed to PageNoteStore once the gesture ends.
    val updateNote: (String, (PageNote) -> PageNote) -> Unit = { id, transform ->
        notes = notes.map { if (it.id == id) transform(it) else it }
    }
    val persistNote: (String) -> Unit = { id ->
        val current = notes.firstOrNull { it.id == id }
        if (current != null && uri != null) {
            notes = PageNoteStore.update(context, uri, current)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(pageSource.pageCount) { index ->
            val pageNumber = index + 1

            LaunchedEffect(pageNumber) {
                pageSource.ensurePageRendered(pageNumber)
            }

            val bmp = pageSource.bitmapCache[pageNumber]
            var imageSizePx by remember { mutableStateOf(IntSize.Zero) }
            Box(modifier = Modifier.fillMaxWidth()) {
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Page $pageNumber",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged { imageSizePx = it }
                            // Note entry is deliberately initiated by the visible chips,
                            // not by a second, invisible tap target over the sheet. That
                            // made the old two-step flow feel like the chips missed taps.
                            // Long-press is a separate gesture (renaming), so it doesn't
                            // fight the chips for a plain tap the way a second tap target
                            // would have.
                            .pointerInput(pageNumber) {
                                detectTapGestures(onLongPress = { onRenameRequested() })
                            }
                    )

                    Text(
                        text = "$pageNumber / ${pageSource.pageCount}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    // Both visible controls open the same real note-entry dialog directly.
                    // The saved note starts near the matching side of the page, so the
                    // button itself is the complete, reliable interaction — no precision
                    // tap on the sheet is needed to get the field to appear.
                    Text(
                        text = "Note",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable {
                                pendingPlacement = PendingNotePlacement(
                                    page = pageNumber,
                                    xFraction = 0.10f,
                                    yFraction = 0.10f
                                )
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    if (imageSizePx.width > 0) {
                        notes.filter { it.page == pageNumber }.forEach { note ->
                            PageNoteBubble(
                                note = note,
                                containerSizePx = imageSizePx,
                                onMove = { dxFraction, dyFraction ->
                                    updateNote(note.id) {
                                        it.copy(
                                            xFraction = (it.xFraction + dxFraction).coerceIn(0f, 1f),
                                            yFraction = (it.yFraction + dyFraction).coerceIn(0f, 1f)
                                        )
                                    }
                                },
                                onMoveEnd = { persistNote(note.id) },
                                onResize = { dScale ->
                                    updateNote(note.id) {
                                        it.copy(scale = (it.scale + dScale).coerceIn(MIN_NOTE_SCALE, MAX_NOTE_SCALE))
                                    }
                                },
                                onResizeEnd = { persistNote(note.id) },
                                onTap = { onNoteTapped(note.id) }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    )
                }
            }

            if (pageNumber < pageSource.pageCount) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 4.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    pendingPlacement?.let { placement ->
        Log.d(TAG, "rendering Add-text dialog for $placement")
        AddNoteDialog(
            onDismiss = { pendingPlacement = null },
            onConfirm = { trimmed ->
                if (uri != null) {
                    notes = PageNoteStore.add(
                        context, uri, placement.page, placement.xFraction, placement.yFraction, trimmed
                    )
                }
                pendingPlacement = null
            }
        )
    }
}

/**
 * Text-entry dialog for a freshly-placed note. Rewritten as its own
 * composable (was inlined before) to fix two real gaps in the old version:
 * the field never grabbed focus/the keyboard on open — so "tap page, dialog
 * opens, start typing" silently did nothing until you *also* tapped the
 * field, which read as the whole feature being broken — and "Add" stayed
 * tappable (and silently no-op'd) with empty/whitespace-only text instead of
 * just being disabled.
 */
@Composable
private fun AddNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (trimmedText: String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val trimmed = text.trim()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add text") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Note") },
                modifier = Modifier.focusRequester(focusRequester)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(trimmed) },
                enabled = trimmed.isNotEmpty()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * One placed text note: draggable by its body (updates [onMove] live, then
 * [onMoveEnd] once released), resizable via the small corner handle (updates
 * [onResize]/[onResizeEnd] the same way). The resize handle fades out after
 * [HANDLE_IDLE_MILLIS] of no interaction with this note, to stay out of the
 * way once you're done placing it; tapping the note wakes it back up and
 * reports itself via [onTap] as the most-recently-tapped note — that's what
 * the top bar's trash icon deletes, not by tapping the note directly. Drag
 * detection uses touch-slop internally, so a tap that doesn't move enough
 * never reaches [onMove] — the tap detector (a separate pointerInput below)
 * handles that case instead.
 */
@Composable
private fun PageNoteBubble(
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

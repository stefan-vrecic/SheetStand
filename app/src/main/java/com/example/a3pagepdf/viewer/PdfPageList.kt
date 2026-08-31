package com.example.a3pagepdf.viewer

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A tap has landed on a page while its pencil was armed; waiting on the
 * text-entry dialog before it turns into a persisted [PageNote]. */
private data class PendingNotePlacement(val page: Int, val xFraction: Float, val yFraction: Float)

private const val TAG = "PageNotes"

/**
 * Continuous, full-width scrolling list of every page in the PDF, used by
 * auto-scroll mode. Each page also gets a small "Note" chip (top-left) that
 * opens [AddNoteDialog] directly for that page. [armedPage]/[onArmedPageChange]
 * are hoisted to the caller so the top bar's own button (a bigger,
 * easier-to-hit target) can drive the same state. Placed notes
 * ([PageNoteBubble]) can be dragged by their body and resized via the corner
 * handle; tapping one reports it up via [onNoteTapped] as the
 * most-recently-tapped note. Deleting is done from the top bar's trash icon,
 * which removes that note ([notesRefreshToken] bumps whenever that happens,
 * so this list reloads from [PageNoteStore] to reflect it). Notes persist
 * per-PDF via [PageNoteStore]. Long-pressing a page arms [onRenameRequested]
 * instead (renaming the whole PDF) — a separate gesture from the tap-driven
 * note chip, so it doesn't compete with note placement. Each page also gets
 * a [RemoteControlButton] chip, stacked just above the page-counter chip in
 * the same corner.
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

                    // Stacked in a Column (rather than two independent
                    // BottomEnd-aligned children) so the remote-control chip
                    // never overlaps the page-counter chip beneath it.
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        RemoteControlButton(modifier = Modifier.padding(bottom = 4.dp))
                        Text(
                            text = "$pageNumber / ${pageSource.pageCount}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

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

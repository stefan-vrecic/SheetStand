package com.example.a3pagepdf.viewer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Top control row for AutoScrollActivity: Open / Play-Pause / prev-next page
 * jump / page indicator / metronome beat lights + control / timer control /
 * metronome pause-resume / delete-last-note.
 */
@Composable
fun AutoScrollTopBar(
    pageSource: PdfPageSource,
    listState: LazyListState,
    currentVisiblePage: Int,
    autoScroll: AutoScrollState,
    timer: PageTimerState,
    metronome: MetronomeState,
    onDeleteLastNote: () -> Unit,
    resolveDeleteNoteText: () -> String?,
    onOpenPdf: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    // Holds the text of whichever note the trash icon would delete, resolved
    // right when it's tapped — non-null while the confirm dialog is showing.
    var pendingDeleteText by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompactButton(onClick = onOpenPdf) {
            Text("Open")
        }
        Spacer(modifier = Modifier.width(7.dp))
        CompactButton(onClick = {
            autoScroll.togglePlay(onPlayStarted = { timer.isActive = false })
        }) {
            Text(if (autoScroll.isPlaying) "Pause" else "Play")
        }
        if (autoScroll.delayCountdown != null) {
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = "Starting in ${autoScroll.delayCountdown}…",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        CompactButton(onClick = {
            autoScroll.stop()
            val targetIndex = (currentVisiblePage - 2).coerceAtLeast(0)
            coroutineScope.launch { listState.animateScrollToItem(targetIndex) }
        }) {
            Text("◀–")
        }
        Spacer(modifier = Modifier.width(7.dp))
        CompactButton(onClick = {
            autoScroll.stop()
            val targetIndex = currentVisiblePage.coerceAtMost(pageSource.pageCount - 1)
            coroutineScope.launch { listState.animateScrollToItem(targetIndex) }
        }) {
            Text("–▶")
        }
        Spacer(modifier = Modifier.width(14.dp))
        if (pageSource.pageCount > 0) {
            Text("$currentVisiblePage of ${pageSource.pageCount}", fontSize = 14.sp)
        }
        FavoriteStarButton(uri = pageSource.currentUri, mode = pageSource.mode)
        Spacer(modifier = Modifier.width(7.dp))
        SessionTimerButton()
        Spacer(modifier = Modifier.width(7.dp))

        // Deletes whichever note you last tapped (any page) — falls back to the
        // last-added note if none has been tapped yet. Confirms first since it
        // can't be undone.
        CompactButton(onClick = { pendingDeleteText = resolveDeleteNoteText() }) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete last-tapped note",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        if (metronome.isOn) {
            Spacer(modifier = Modifier.width(14.dp))
            MetronomeBeatLights(metronome)
        }

        Spacer(modifier = Modifier.weight(1f))

        MetronomeControl(metronome)
        Spacer(modifier = Modifier.width(7.dp))

        TimerControl(
            state = timer,
            pageCount = pageSource.pageCount,
            onActivate = { autoScroll.stop() }
        )

        if (metronome.isOn) {
            Spacer(modifier = Modifier.width(7.dp))
            CompactButton(onClick = { metronome.isPaused = !metronome.isPaused }) {
                Text(if (metronome.isPaused) "Start" else "Pause")
            }
        }
    }

    pendingDeleteText?.let { fullText ->
        val preview = if (fullText.length > 50) fullText.take(50) + "…" else fullText
        AlertDialog(
            onDismissRequest = { pendingDeleteText = null },
            title = { Text("Delete note?") },
            text = { Text("The last note you tapped: \"$preview\". This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteLastNote()
                    pendingDeleteText = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteText = null }) { Text("Cancel") }
            }
        )
    }
}

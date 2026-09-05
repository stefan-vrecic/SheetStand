package com.example.a3pagepdf

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.a3pagepdf.viewer.AudioSeekerEffect
import com.example.a3pagepdf.viewer.AudioSeekerExpandedRow
import com.example.a3pagepdf.viewer.AutoScrollEffect
import com.example.a3pagepdf.viewer.AutoScrollTopBar
import com.example.a3pagepdf.viewer.FavoritesStore
import com.example.a3pagepdf.viewer.MetronomeEffect
import com.example.a3pagepdf.viewer.MetronomeRadiantOverlay
import com.example.a3pagepdf.viewer.PageNote
import com.example.a3pagepdf.viewer.PageNoteStore
import com.example.a3pagepdf.viewer.PageTimerEffect
import com.example.a3pagepdf.viewer.PdfDisplayNames
import com.example.a3pagepdf.viewer.PdfNoteBaker
import com.example.a3pagepdf.viewer.PdfPageList
import com.example.a3pagepdf.viewer.PdfPageSource
import com.example.a3pagepdf.viewer.RenamePdfDialog
import com.example.a3pagepdf.viewer.SpeedControlRow
import com.example.a3pagepdf.viewer.pdfUriExtra
import com.example.a3pagepdf.viewer.rememberAudioSeekerState
import com.example.a3pagepdf.viewer.rememberAutoScrollState
import com.example.a3pagepdf.viewer.rememberMetronomeState
import com.example.a3pagepdf.viewer.rememberPageTimerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AutoScrollActivity : ComponentActivity() {

    private lateinit var pageSource: PdfPageSource

    private val openDocLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { pageSource.openPdf(it, isNewSelection = true) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageSource = PdfPageSource(contentResolver, this)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {

                        val listState = rememberLazyListState()
                        val currentVisiblePage by remember {
                            derivedStateOf { listState.firstVisibleItemIndex + 1 }
                        }

                        val autoScroll = rememberAutoScrollState()
                        val timer = rememberPageTimerState()
                        val metronome = rememberMetronomeState()
                        val audioSeeker = rememberAudioSeekerState()
                        AudioSeekerEffect(audioSeeker)

                        val context = LocalContext.current

                        // Hoisted so both the top bar's and each page's own "Note" button
                        // drive the exact same "add text" arming state.
                        var armedPage by remember { mutableStateOf<Int?>(null) }
                        LaunchedEffect(pageSource.currentUri) { armedPage = null }

                        // Whichever note was tapped most recently — that's what the trash
                        // icon deletes. Falls back to the last-added note if nothing's
                        // been tapped yet this session. Resets when the PDF changes so a
                        // stale id from a previous document can't linger.
                        var lastTappedNoteId by remember { mutableStateOf<String?>(null) }
                        LaunchedEffect(pageSource.currentUri) { lastTappedNoteId = null }

                        // Bumped whenever the top bar's trash icon deletes a note, so
                        // PdfPageList's own notes (loaded straight from PageNoteStore)
                        // reload and reflect the deletion.
                        var notesRefreshToken by remember { mutableStateOf(0) }

                        var showRenameDialog by remember { mutableStateOf(false) }

                        // Shared by the confirm-dialog preview and the actual delete, so
                        // they always agree on which note is in play.
                        fun resolveDeleteCandidate(): PageNote? {
                            val uri = pageSource.currentUri ?: return null
                            val notes = PageNoteStore.load(context, uri)
                            val tapped = lastTappedNoteId
                            return if (tapped != null) notes.firstOrNull { it.id == tapped } else notes.lastOrNull()
                        }

                        val coroutineScope = rememberCoroutineScope()
                        var isExportingWithNotes by remember { mutableStateOf(false) }

                        fun exportWithNotes() {
                            val uri = pageSource.currentUri
                            if (uri == null) {
                                Toast.makeText(context, "No PDF is open", Toast.LENGTH_SHORT).show()
                                return
                            }
                            if (isExportingWithNotes) return
                            isExportingWithNotes = true
                            coroutineScope.launch {
                                val notes = PageNoteStore.load(context, uri)
                                val baseName = (PdfDisplayNames.get(context, uri)
                                    ?: FavoritesStore.queryDisplayName(context, uri)).removeSuffix(".pdf")
                                val outFile = withContext(Dispatchers.IO) {
                                    PdfNoteBaker.bake(context, uri, "$baseName (with notes).pdf", notes)
                                }
                                isExportingWithNotes = false
                                if (outFile == null) {
                                    Toast.makeText(context, "Couldn't export PDF", Toast.LENGTH_SHORT).show()
                                } else {
                                    PdfNoteBaker.shareViaGmail(context, outFile)
                                }
                            }
                        }

                        AutoScrollTopBar(
                            pageSource = pageSource,
                            listState = listState,
                            currentVisiblePage = currentVisiblePage,
                            autoScroll = autoScroll,
                            timer = timer,
                            metronome = metronome,
                            audioSeeker = audioSeeker,
                            resolveDeleteNoteText = { resolveDeleteCandidate()?.text },
                            onDeleteLastNote = {
                                val uri = pageSource.currentUri
                                val candidate = resolveDeleteCandidate()
                                if (uri != null && candidate != null) {
                                    PageNoteStore.remove(context, uri, candidate.id)
                                }
                                lastTappedNoteId = null
                                notesRefreshToken++
                            },
                            onOpenPdf = { openDocLauncher.launch(arrayOf("application/pdf")) },
                            isExportingWithNotes = isExportingWithNotes,
                            onExportWithNotes = { exportWithNotes() }
                        )

                        // Own full-width line, not crammed into AutoScrollTopBar's already-packed
                        // row — the full player (scrub bar, speed, reload) is wide enough to wrap
                        // onto multiple lines if it tried to share it with everything else there.
                        if (audioSeeker.mediaPlayer != null) {
                            AudioSeekerExpandedRow(
                                state = audioSeeker,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            )
                        }

                        SpeedControlRow(autoScroll)

                        // Side-effect loops driving each feature
                        AutoScrollEffect(autoScroll, listState)
                        PageTimerEffect(timer, listState, currentVisiblePage, pageSource.pageCount)
                        MetronomeEffect(metronome)

                        // Box, not a bare PdfPageList call, so the radiant pulse can be
                        // layered centered over the page content itself — it's a plain
                        // Canvas with no gesture handlers, so it doesn't block scrolling,
                        // tapping, or note placement underneath it.
                        Box(modifier = Modifier.fillMaxSize()) {
                            PdfPageList(
                                pageSource = pageSource,
                                listState = listState,
                                armedPage = armedPage,
                                onArmedPageChange = { armedPage = it },
                                notesRefreshToken = notesRefreshToken,
                                onNoteTapped = { lastTappedNoteId = it },
                                onRenameRequested = {
                                    if (pageSource.currentUri != null) showRenameDialog = true
                                }
                            )
                            if (metronome.isOn && metronome.bigPulseEnabled) {
                                MetronomeRadiantOverlay(
                                    state = metronome,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        val renameUri = pageSource.currentUri
                        if (showRenameDialog && renameUri != null) {
                            RenamePdfDialog(
                                currentName = PdfDisplayNames.get(context, renameUri)
                                    ?: FavoritesStore.queryDisplayName(context, renameUri),
                                onDismiss = { showRenameDialog = false },
                                onConfirm = { newName ->
                                    PdfDisplayNames.set(context, renameUri, newName)
                                    showRenameDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }

        val favoriteUri = intent.pdfUriExtra()
        if (favoriteUri != null) {
            pageSource.openPdf(favoriteUri, isNewSelection = true)
        } else {
            pageSource.loadLastPdfIfAvailable()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pageSource.release()
    }
}
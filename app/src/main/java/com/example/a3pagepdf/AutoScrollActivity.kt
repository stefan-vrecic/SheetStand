package com.example.a3pagepdf

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.a3pagepdf.viewer.AutoScrollEffect
import com.example.a3pagepdf.viewer.AutoScrollTopBar
import com.example.a3pagepdf.viewer.MetronomeEffect
import com.example.a3pagepdf.viewer.PageNote
import com.example.a3pagepdf.viewer.PageNoteStore
import com.example.a3pagepdf.viewer.PageTimerEffect
import com.example.a3pagepdf.viewer.PdfPageList
import com.example.a3pagepdf.viewer.PdfPageSource
import com.example.a3pagepdf.viewer.SpeedControlRow
import com.example.a3pagepdf.viewer.pdfUriExtra
import com.example.a3pagepdf.viewer.rememberAutoScrollState
import com.example.a3pagepdf.viewer.rememberMetronomeState
import com.example.a3pagepdf.viewer.rememberPageTimerState

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

                        // Shared by the confirm-dialog preview and the actual delete, so
                        // they always agree on which note is in play.
                        fun resolveDeleteCandidate(): PageNote? {
                            val uri = pageSource.currentUri ?: return null
                            val notes = PageNoteStore.load(context, uri)
                            val tapped = lastTappedNoteId
                            return if (tapped != null) notes.firstOrNull { it.id == tapped } else notes.lastOrNull()
                        }

                        AutoScrollTopBar(
                            pageSource = pageSource,
                            listState = listState,
                            currentVisiblePage = currentVisiblePage,
                            autoScroll = autoScroll,
                            timer = timer,
                            metronome = metronome,
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
                            onOpenPdf = { openDocLauncher.launch(arrayOf("application/pdf")) }
                        )

                        SpeedControlRow(autoScroll)

                        // Side-effect loops driving each feature
                        AutoScrollEffect(autoScroll, listState)
                        PageTimerEffect(timer, listState, currentVisiblePage, pageSource.pageCount)
                        MetronomeEffect(metronome)

                        PdfPageList(
                            pageSource = pageSource,
                            listState = listState,
                            armedPage = armedPage,
                            onArmedPageChange = { armedPage = it },
                            notesRefreshToken = notesRefreshToken,
                            onNoteTapped = { lastTappedNoteId = it }
                        )
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
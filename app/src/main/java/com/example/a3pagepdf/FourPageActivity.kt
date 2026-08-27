package com.example.a3pagepdf

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.a3pagepdf.viewer.MetronomeBeatLights
import com.example.a3pagepdf.viewer.MetronomeControl
import com.example.a3pagepdf.viewer.MetronomeEffect
import com.example.a3pagepdf.viewer.MetronomeOverlay
import com.example.a3pagepdf.viewer.MultiPagePdfController
import com.example.a3pagepdf.viewer.FavoritesStore
import com.example.a3pagepdf.viewer.PdfDisplayNames
import com.example.a3pagepdf.viewer.PdfViewerMode
import com.example.a3pagepdf.viewer.PdfViewerTopBar
import com.example.a3pagepdf.viewer.RenamePdfDialog
import com.example.a3pagepdf.viewer.ZoomableFullscreenBox
import com.example.a3pagepdf.viewer.pdfUriExtra
import com.example.a3pagepdf.viewer.rememberMetronomeState
import com.example.a3pagepdf.viewer.setSystemBarsHidden
import androidx.compose.ui.platform.LocalContext

class FourPageActivity : ComponentActivity() {

    private lateinit var controller: MultiPagePdfController

    private val openDocLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { controller.openPdf(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = MultiPagePdfController(contentResolver, this, windowSize = 4, mode = PdfViewerMode.FOUR_PAGE)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                    Box(modifier = Modifier.fillMaxSize()) {

                        var isFullScreen by remember { mutableStateOf(false) }
                        val metronome = rememberMetronomeState()
                        MetronomeEffect(metronome)

                        val context = LocalContext.current
                        var showRenameDialog by remember { mutableStateOf(false) }

                        Column(modifier = Modifier.fillMaxSize()) {

                            if (!isFullScreen) {
                                PdfViewerTopBar(
                                    controller = controller,
                                    onOpenPdf = { openDocLauncher.launch(arrayOf("application/pdf")) }
                                ) {
                                    if (metronome.isOn) {
                                        Spacer(modifier = Modifier.width(16.dp))
                                        MetronomeBeatLights(metronome)
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    MetronomeControl(metronome)
                                    if (metronome.isOn) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(onClick = { metronome.isPaused = !metronome.isPaused }) {
                                            Text(if (metronome.isPaused) "Start" else "Pause")
                                        }
                                    }
                                }
                            }

                            // 2x2 grid:
                            // top-left = p1     top-right    = p3
                            // bottom-left = p2  bottom-right = p4
                            ZoomableFullscreenBox(
                                onTap = {
                                    isFullScreen = !isFullScreen
                                    setSystemBarsHidden(isFullScreen)
                                },
                                onLongPress = { if (controller.currentUri != null) showRenameDialog = true }
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        PageSlot(
                                            modifier = Modifier.weight(1f).fillMaxHeight(),
                                            bitmap = controller.bitmaps.getOrNull(0),
                                            label = "Page ${controller.start}"
                                        )
                                        PageSlot(
                                            modifier = Modifier.weight(1f).fillMaxHeight(),
                                            bitmap = controller.bitmaps.getOrNull(2),
                                            label = "Page ${controller.start + 2}"
                                        )
                                    }
                                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        PageSlot(
                                            modifier = Modifier.weight(1f).fillMaxHeight(),
                                            bitmap = controller.bitmaps.getOrNull(1),
                                            label = "Page ${controller.start + 1}"
                                        )
                                        PageSlot(
                                            modifier = Modifier.weight(1f).fillMaxHeight(),
                                            bitmap = controller.bitmaps.getOrNull(3),
                                            label = "Page ${controller.start + 3}"
                                        )
                                    }
                                }
                            }
                        }

                        if (isFullScreen && metronome.isOn) {
                            MetronomeOverlay(
                                state = metronome,
                                modifier = Modifier.align(Alignment.TopEnd)
                            )
                        }

                        val renameUri = controller.currentUri
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
            controller.openPdf(favoriteUri)
        } else {
            controller.loadLastPdfIfAvailable()
        }
    }

    @Composable
    private fun PageSlot(modifier: Modifier, bitmap: Bitmap?, label: String) {
        Box(
            modifier = modifier
                .background(Color.White)
                .border(width = 1.dp, color = Color.Black)
        ) {
            bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = label,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.release()
    }
}

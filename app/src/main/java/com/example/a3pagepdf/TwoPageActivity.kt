package com.example.a3pagepdf

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.a3pagepdf.viewer.MetronomeBeatLights
import com.example.a3pagepdf.viewer.MetronomeControl
import com.example.a3pagepdf.viewer.MetronomeEffect
import com.example.a3pagepdf.viewer.MetronomeOverlay
import com.example.a3pagepdf.viewer.MultiPagePdfController
import com.example.a3pagepdf.viewer.PdfViewerMode
import com.example.a3pagepdf.viewer.PdfViewerTopBar
import com.example.a3pagepdf.viewer.ZoomableFullscreenBox
import com.example.a3pagepdf.viewer.pdfUriExtra
import com.example.a3pagepdf.viewer.rememberMetronomeState
import com.example.a3pagepdf.viewer.setSystemBarsHidden

class TwoPageActivity : ComponentActivity() {

    private lateinit var controller: MultiPagePdfController

    private val openDocLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { controller.openPdf(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = MultiPagePdfController(contentResolver, this, windowSize = 2, mode = PdfViewerMode.TWO_PAGE)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {

                        var isFullScreen by remember { mutableStateOf(false) }
                        val metronome = rememberMetronomeState()
                        MetronomeEffect(metronome)

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

                            ZoomableFullscreenBox(onTap = {
                                isFullScreen = !isFullScreen
                                setSystemBarsHidden(isFullScreen)
                            }) {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    for (i in 0..1) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                        ) {
                                            controller.bitmaps.getOrNull(i)?.let { bmp ->
                                                Image(
                                                    bitmap = bmp.asImageBitmap(),
                                                    contentDescription = "Page ${controller.start + i}",
                                                    contentScale = ContentScale.Fit,
                                                    alignment = if (i == 0) Alignment.CenterEnd else Alignment.CenterStart,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
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

    override fun onDestroy() {
        super.onDestroy()
        controller.release()
    }
}

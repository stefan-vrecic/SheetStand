package com.example.a3pagepdf.viewer

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Shared state + logic for any "windowed" PDF page viewer (1, 2, 3, or 4
 * pages visible at once). Handles opening a PDF, rendering the current
 * window of pages to bitmaps, moving the window forward/backward, and
 * persisting "last opened PDF" separately per mode via [PdfPersistence].
 *
 * @param mode a short, stable identifier for this viewer mode (e.g.
 * "two_page"), so each mode remembers its own last-opened PDF independently.
 */
class MultiPagePdfController(
    private val contentResolver: ContentResolver,
    private val context: Context,
    val windowSize: Int,
    val mode: String
) {
    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null

    var pageCount by mutableStateOf(0)
        private set
    var start by mutableStateOf(1)
        private set
    var jumpIncrement by mutableStateOf(1)
        private set
    var currentUri by mutableStateOf<Uri?>(null)
        private set

    val bitmaps = mutableStateListOf<Bitmap?>().apply {
        repeat(windowSize) { add(null) }
    }

    val endPage: Int
        get() = start + minOf(windowSize, pageCount) - 1

    /** Cycles 1 -> 2 -> ... -> windowSize -> 1, matching each mode's old "Jump: N" button. */
    fun cycleJumpIncrement() {
        jumpIncrement = if (jumpIncrement >= windowSize) 1 else jumpIncrement + 1
    }

    /** Reopens whatever PDF this specific mode last had open, if any. Safe to call even if none was saved. */
    fun loadLastPdfIfAvailable() {
        val uri = PdfPersistence.load(context, mode) ?: return
        try {
            openPdf(uri, isNewSelection = false)
        } catch (e: Exception) {
            PdfPersistence.clear(context, mode)
        }
    }

    fun openPdf(uri: Uri, isNewSelection: Boolean = true) {
        if (isNewSelection) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // ignore
            }
            PdfPersistence.save(context, mode, uri)
        }

        pdfRenderer?.close()
        fileDescriptor?.close()

        fileDescriptor = contentResolver.openFileDescriptor(uri, "r")
        fileDescriptor?.let { fd ->
            pdfRenderer = PdfRenderer(fd)
            pageCount = pdfRenderer!!.pageCount
            start = 1
            currentUri = uri
            renderWindow()
        }
    }

    fun moveWindow(delta: Int) {
        if (pageCount == 0) return
        val effectiveWindow = minOf(windowSize, pageCount)
        val maxStart = pageCount - effectiveWindow + 1
        val newStart = (start + delta).coerceIn(1, maxStart)
        if (newStart != start) {
            start = newStart
            renderWindow()
        }
    }

    private fun renderWindow() {
        val renderer = pdfRenderer ?: return
        if (pageCount == 0) return
        val effectiveWindow = minOf(windowSize, pageCount)

        for (i in 0 until windowSize) {
            bitmaps[i] = if (i < effectiveWindow) renderPage(renderer, start + i) else null
        }
    }

    private fun renderPage(renderer: PdfRenderer, pageIndex1Based: Int): Bitmap {
        val page = renderer.openPage(pageIndex1Based - 1)
        val scale = 2
        val bmp = Bitmap.createBitmap(
            page.width * scale,
            page.height * scale,
            Bitmap.Config.ARGB_8888
        )
        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        return bmp
    }

    fun release() {
        pdfRenderer?.close()
        fileDescriptor?.close()
    }
}

package com.example.a3pagepdf.viewer

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Loads a PDF and renders individual pages to bitmaps on demand, with an
 * in-memory cache. Also persists the last-opened PDF's URI (under its own
 * "auto_scroll" mode key via PdfPersistence) so it can be reopened
 * automatically next launch.
 */
class PdfPageSource(
    private val contentResolver: ContentResolver,
    private val context: Context
) {
    val mode: String = PdfViewerMode.AUTO_SCROLL

    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null

    var pageCount by mutableStateOf(0)
        private set
    var currentUri by mutableStateOf<Uri?>(null)
        private set

    val bitmapCache = mutableStateMapOf<Int, Bitmap>()

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
        bitmapCache.clear()

        fileDescriptor = contentResolver.openFileDescriptor(uri, "r")
        fileDescriptor?.let { fd ->
            pdfRenderer = PdfRenderer(fd)
            pageCount = pdfRenderer!!.pageCount
            currentUri = uri
        }
    }

    fun ensurePageRendered(pageNumber: Int) {
        if (bitmapCache.containsKey(pageNumber)) return
        val renderer = pdfRenderer ?: return
        val page = renderer.openPage(pageNumber - 1)
        val scale = 2
        val bmp = Bitmap.createBitmap(
            page.width * scale,
            page.height * scale,
            Bitmap.Config.ARGB_8888
        )
        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        bitmapCache[pageNumber] = bmp
    }

    fun release() {
        pdfRenderer?.close()
        fileDescriptor?.close()
    }
}

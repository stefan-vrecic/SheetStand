package com.example.a3pagepdf.viewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * In-memory cache of first-page thumbnails for favourited PDFs, keyed by
 * URI string. Module-level (not tied to a Composable) so thumbnails survive
 * HomeActivity recompositions and don't get re-rendered every time the
 * favourites grid appears.
 */
object FavoriteThumbnailLoader {
    private const val THUMB_WIDTH = 240

    val cache = mutableStateMapOf<String, Bitmap>()

    /**
     * Returns the thumbnail for [uri] — a user-picked [CustomThumbnails] override
     * if one exists, otherwise the (cached, or freshly rendered) first page of
     * the PDF itself. Safe to call repeatedly.
     */
    suspend fun thumbnailFor(context: Context, uri: Uri): Bitmap? {
        val key = uri.toString()
        cache[key]?.let { return it }
        CustomThumbnails.get(context, uri)?.let {
            cache[key] = it
            return it
        }

        return withContext(Dispatchers.IO) {
            var fd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            try {
                fd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext null
                renderer = PdfRenderer(fd)
                if (renderer.pageCount == 0) return@withContext null

                val page = renderer.openPage(0)
                val scale = THUMB_WIDTH.toFloat() / page.width
                val height = (page.height * scale).toInt().coerceAtLeast(1)

                val bmp = Bitmap.createBitmap(THUMB_WIDTH, height, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                cache[key] = bmp
                bmp
            } catch (e: Exception) {
                null
            } finally {
                renderer?.close()
                fd?.close()
            }
        }
    }

    fun evict(uri: Uri) {
        cache.remove(uri.toString())
    }
}

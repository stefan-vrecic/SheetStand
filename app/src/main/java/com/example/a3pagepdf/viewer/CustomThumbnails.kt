package com.example.a3pagepdf.viewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * User-picked custom thumbnails for favourited PDFs (see FavoritesGrid's
 * "Add Thumbnail" long-press option), stored as small PNGs in app-private
 * storage — keyed by a hash of the PDF's URI, since URIs can contain
 * characters that aren't safe filenames. [FavoriteThumbnailLoader] checks
 * here first, before falling back to rendering the PDF's own first page.
 */
object CustomThumbnails {
    private const val DIR_NAME = "custom_thumbnails"
    private const val MAX_DIMENSION = 480

    private fun fileFor(context: Context, uri: Uri): File {
        val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
        return File(dir, "${uri.toString().hashCode()}.png")
    }

    fun get(context: Context, uri: Uri): Bitmap? {
        val file = fileFor(context, uri)
        if (!file.exists()) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            null
        }
    }

    /** Downscales [source] to a sane thumbnail size and persists it for [uri]. Returns the stored bitmap. */
    fun set(context: Context, uri: Uri, source: Bitmap): Bitmap {
        val longestSide = maxOf(source.width, source.height)
        val scale = MAX_DIMENSION.toFloat() / longestSide
        val bitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                source,
                (source.width * scale).toInt().coerceAtLeast(1),
                (source.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            source
        }
        try {
            FileOutputStream(fileFor(context, uri)).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            // Worst case the in-memory cache (set by the caller) still shows it
            // for this session; it just won't survive an app restart.
        }
        return bitmap
    }
}

package com.example.a3pagepdf.viewer

import android.content.Context
import android.net.Uri

/**
 * User-chosen display names for PDFs, keyed by URI, independent of the
 * underlying file. Renaming here does NOT touch the real document (that
 * would need DocumentsContract.renameDocument(), which isn't reliably
 * supported across every content provider a PDF might come from) — it just
 * overrides what the app calls it everywhere a name is shown. Read by
 * [FavoritesStore.load] so a rename immediately shows up on that PDF's
 * favourite tile too, without either side needing to know about the other.
 */
object PdfDisplayNames {
    private const val PREFS_NAME = "pdf_prefs"
    private const val KEY_PREFIX = "display_name_"

    fun get(context: Context, uri: Uri): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PREFIX + uri.toString(), null)
            ?.takeIf { it.isNotBlank() }

    fun set(context: Context, uri: Uri, name: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PREFIX + uri.toString(), name)
            .apply()
    }
}

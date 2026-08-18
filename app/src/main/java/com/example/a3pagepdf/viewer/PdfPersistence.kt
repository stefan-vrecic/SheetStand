package com.example.a3pagepdf.viewer

import android.content.Context
import android.net.Uri

/**
 * Persists "last opened PDF" separately per viewer mode (e.g. "two_page",
 * "three_page", "four_page", "auto_scroll"), so switching modes doesn't
 * clobber what was open in another mode. Also keeps a single "most recent
 * overall" entry under [GLOBAL_KEY], which HomeActivity's share-via-Gmail
 * button reads — it doesn't care which mode a PDF was opened in, just the
 * latest one overall.
 */
object PdfPersistence {
    private const val PREFS_NAME = "pdf_prefs"
    private const val GLOBAL_KEY = "last_pdf_uri"

    private fun modeKey(mode: String) = "last_pdf_uri_$mode"

    fun save(context: Context, mode: String, uri: Uri) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(modeKey(mode), uri.toString())
            .putString(GLOBAL_KEY, uri.toString())
            .apply()
    }

    fun load(context: Context, mode: String): Uri? {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(modeKey(mode), null) ?: return null
        return Uri.parse(stored)
    }

    fun clear(context: Context, mode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(modeKey(mode))
            .apply()
    }
}

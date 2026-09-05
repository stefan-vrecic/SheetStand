package com.example.a3pagepdf.viewer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject

/**
 * A single favourited PDF: the persisted-permission URI plus a display
 * name captured at the time it was added (so we don't need to re-query
 * the content resolver every time the grid renders).
 *
 * [mode] is which viewer it was favourited from (see [PdfViewerMode]) — so
 * tapping it in the grid can relaunch the same mode automatically. Null
 * when it was added directly from HomeActivity, which doesn't know a mode;
 * the grid asks the user once, then [FavoritesStore.setMode] fills it in.
 */
data class FavoriteItem(
    val uri: Uri,
    val name: String,
    val mode: String? = null
)

/**
 * Persists the user's favourite PDFs (used to back the horizontally-scrolling
 * favourites grid on HomeActivity) as a small JSON array in the same
 * "pdf_prefs" prefs file that PdfPersistence already uses. Uncapped — the
 * grid is a scrolling [androidx.compose.foundation.lazy.grid.LazyHorizontalGrid],
 * not a fixed layout, so there's no slot count to run out of.
 */
object FavoritesStore {
    private const val PREFS_NAME = "pdf_prefs"
    private const val KEY = "favorite_pdfs"

    fun load(context: Context): List<FavoriteItem> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return emptyList()

        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                val uriString = obj.optString("uri").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val uri = Uri.parse(uriString)
                FavoriteItem(
                    uri = uri,
                    // A rename (PdfDisplayNames) always wins over the name captured
                    // at add-time — this is how renaming "propagates" to the
                    // favourites grid without the rename flow needing to know
                    // whether this URI is even favourited, let alone touch this file.
                    name = PdfDisplayNames.get(context, uri) ?: obj.optString("name", "PDF"),
                    mode = obj.optString("mode").takeIf { it.isNotBlank() }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Returns false (and does nothing) if [uri] is already favourited. */
    fun add(context: Context, uri: Uri, name: String, mode: String? = null): Boolean {
        val current = load(context)
        if (current.any { it.uri == uri }) return false

        val effectiveName = PdfDisplayNames.get(context, uri) ?: name
        val updated = current + FavoriteItem(uri, effectiveName, mode)
        persist(context, updated)
        return true
    }

    fun remove(context: Context, uri: Uri) {
        val updated = load(context).filterNot { it.uri == uri }
        persist(context, updated)
    }

    fun clear(context: Context) {
        persist(context, emptyList())
    }

    /** Fills in (or overwrites) the viewer mode for an already-favourited [uri]. */
    fun setMode(context: Context, uri: Uri, mode: String) {
        val updated = load(context).map { if (it.uri == uri) it.copy(mode = mode) else it }
        persist(context, updated)
    }

    fun isFavorite(context: Context, uri: Uri): Boolean =
        load(context).any { it.uri == uri }

    private fun persist(context: Context, items: List<FavoriteItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("uri", item.uri.toString())
                    put("name", item.name)
                    item.mode?.let { put("mode", it) }
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, arr.toString())
            .apply()
    }

    /** Best-effort display name for a freshly-picked document Uri (e.g. from OpenDocument). */
    fun queryDisplayName(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            } ?: uri.lastPathSegment ?: "PDF"
        } catch (e: Exception) {
            uri.lastPathSegment ?: "PDF"
        }
    }
}

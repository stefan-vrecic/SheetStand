package com.example.a3pagepdf.viewer

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * A single hand-placed text note on one page of a PDF. Position is stored as
 * a fraction (0f..1f) of that page's rendered width/height, so it lands in
 * the same spot on the page regardless of screen size or zoom.
 */
data class PageNote(
    val id: String,
    val page: Int,
    val xFraction: Float,
    val yFraction: Float,
    val text: String,
    /** Size multiplier from dragging the note's resize handle. 1f = default size. */
    val scale: Float = 1f
)

/**
 * Persists page notes added via the auto-scroll viewer's pencil tool, keyed
 * by the owning PDF's URI, in the same "pdf_prefs" prefs file that
 * FavoritesStore/PdfPersistence already use. All of one PDF's notes are
 * stored together as a single JSON array under a per-URI key.
 */
object PageNoteStore {
    private const val PREFS_NAME = "pdf_prefs"
    private const val KEY_PREFIX = "page_notes_"

    fun load(context: Context, uri: Uri): List<PageNote> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PREFIX + uri.toString(), null) ?: return emptyList()

        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                PageNote(
                    id = obj.optString("id"),
                    page = obj.optInt("page"),
                    xFraction = obj.optDouble("x").toFloat(),
                    yFraction = obj.optDouble("y").toFloat(),
                    text = obj.optString("text"),
                    scale = obj.optDouble("scale", 1.0).toFloat()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Adds a note and returns the full, updated list of notes for [uri]. */
    fun add(
        context: Context,
        uri: Uri,
        page: Int,
        xFraction: Float,
        yFraction: Float,
        text: String
    ): List<PageNote> {
        val note = PageNote(
            id = System.currentTimeMillis().toString(),
            page = page,
            xFraction = xFraction,
            yFraction = yFraction,
            text = text
        )
        val updated = load(context, uri) + note
        persist(context, uri, updated)
        return updated
    }

    /** Removes a note by id and returns the full, updated list of notes for [uri]. */
    fun remove(context: Context, uri: Uri, noteId: String): List<PageNote> {
        val updated = load(context, uri).filterNot { it.id == noteId }
        persist(context, uri, updated)
        return updated
    }

    /** Replaces a note (matched by id) — used after dragging/resizing it — and returns the updated list. */
    fun update(context: Context, uri: Uri, note: PageNote): List<PageNote> {
        val updated = load(context, uri).map { if (it.id == note.id) note else it }
        persist(context, uri, updated)
        return updated
    }

    private fun persist(context: Context, uri: Uri, notes: List<PageNote>) {
        val arr = JSONArray()
        notes.forEach { note ->
            arr.put(
                JSONObject().apply {
                    put("id", note.id)
                    put("page", note.page)
                    put("x", note.xFraction.toDouble())
                    put("y", note.yFraction.toDouble())
                    put("text", note.text)
                    put("scale", note.scale.toDouble())
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PREFIX + uri.toString(), arr.toString())
            .apply()
    }
}

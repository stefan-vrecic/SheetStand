package com.example.a3pagepdf.viewer

import android.content.Context

/**
 * Persists the four full URLs [RemoteControlButton]'s chips hit — "PC"
 * (play), "⏸" (pause), "−" (minus), "+" (plus) — e.g. Keyboard Maestro
 * web-server trigger links. There's no reliable way to auto-discover an
 * arbitrary PC on an arbitrary local network, so these are entered once by
 * the user (via each chip's long-press) rather than found automatically —
 * same SharedPreferences file/key-per-concern pattern as [PdfDisplayNames].
 */
object RemoteControlPrefs {
    private const val PREFS_NAME = "pdf_prefs"
    private const val KEY_PLAY_ADDRESS = "remote_control_address"
    private const val KEY_PAUSE_ADDRESS = "remote_control_address_pause"
    private const val KEY_MINUS_ADDRESS = "remote_control_address_minus"
    private const val KEY_PLUS_ADDRESS = "remote_control_address_plus"

    fun getPlayAddress(context: Context): String? = getRaw(context, KEY_PLAY_ADDRESS)
    fun setPlayAddress(context: Context, address: String) = setRaw(context, KEY_PLAY_ADDRESS, address)

    fun getPauseAddress(context: Context): String? = getRaw(context, KEY_PAUSE_ADDRESS)
    fun setPauseAddress(context: Context, address: String) = setRaw(context, KEY_PAUSE_ADDRESS, address)

    fun getMinusAddress(context: Context): String? = getRaw(context, KEY_MINUS_ADDRESS)
    fun setMinusAddress(context: Context, address: String) = setRaw(context, KEY_MINUS_ADDRESS, address)

    fun getPlusAddress(context: Context): String? = getRaw(context, KEY_PLUS_ADDRESS)
    fun setPlusAddress(context: Context, address: String) = setRaw(context, KEY_PLUS_ADDRESS, address)

    fun suggestedPauseAddress(context: Context): String = suggestedAddress(context, 0)
    fun suggestedMinusAddress(context: Context): String = suggestedAddress(context, 2)
    fun suggestedPlusAddress(context: Context): String = suggestedAddress(context, 3)

    /**
     * A starting point for a chip's dialog when nothing's saved for it yet.
     * The motivating setup (one Keyboard Maestro macro, triggered with a
     * different `value=N` per chip: 1 play, 0 pause, 2 minus, 3 plus) shares
     * everything but that one digit across all four chips, so seed the field
     * from the play address with [value] swapped in instead of leaving it
     * blank — still fully editable if a real setup needs something else
     * entirely.
     */
    private fun suggestedAddress(context: Context, value: Int): String {
        val play = getPlayAddress(context) ?: return ""
        return play.replace("value=1", "value=$value")
    }

    private fun getRaw(context: Context, key: String): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(key, null)?.takeIf { it.isNotBlank() }
    }

    private fun setRaw(context: Context, key: String, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(key, value.trim())
            .apply()
    }
}

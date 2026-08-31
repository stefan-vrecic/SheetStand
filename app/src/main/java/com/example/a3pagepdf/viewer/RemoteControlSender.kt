package com.example.a3pagepdf.viewer

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "RemoteControl"
private const val CONNECT_TIMEOUT_MS = 2000
private const val READ_TIMEOUT_MS = 2000

/**
 * Fires a single HTTP GET at [url] — the full URL the user saved (see
 * [RemoteControlPrefs]), not just a host. This app has no opinion on what
 * that URL does on the other end (the motivating case is a Keyboard Maestro
 * web-server trigger, e.g. `http://<mac-ip>:4490/action.html?MyMacro=1`, but
 * it could just as well be any other local HTTP endpoint) — it just hits it.
 *
 * HTTP-over-TCP was chosen over a raw UDP packet specifically for
 * reliability: TCP actually retries and confirms delivery at the transport
 * layer, where UDP would silently drop packets over flaky Wi-Fi with no way
 * to know. The response itself is still ignored — this stays fire-and-forget
 * from the UI's point of view (no toast/snackbar on failure), it's just the
 * underlying send that's more trustworthy.
 */
suspend fun sendRemoteToggle(url: String) {
    withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            // Bare "host:port" (no scheme) still works — defaults to http —
            // but a full URL (path/query and all, e.g. a Keyboard Maestro
            // trigger link) is passed through untouched.
            val fullUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "http://$url"
            connection = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }
            connection.responseCode // actually issues the request; result isn't needed
        } catch (e: Exception) {
            Log.w(TAG, "Remote-control request to $url failed", e)
        } finally {
            connection?.disconnect()
        }
    }
}

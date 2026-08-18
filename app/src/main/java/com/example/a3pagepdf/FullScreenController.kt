package com.example.a3pagepdf

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Hides/shows the system bars in response to a tap, for the page-viewer activities
 * that support a fullscreen mode (Two/Three/FourPageActivity).
 */
class FullScreenController(private val activity: ComponentActivity) {

    var isFullScreen by mutableStateOf(false)
        private set

    fun toggle() {
        isFullScreen = !isFullScreen
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (isFullScreen) {
            controller.hide(android.view.WindowInsets.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(android.view.WindowInsets.Type.systemBars())
        }
    }
}

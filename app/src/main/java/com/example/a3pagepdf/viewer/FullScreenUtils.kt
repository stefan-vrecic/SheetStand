package com.example.a3pagepdf.viewer

import android.app.Activity
import android.view.WindowInsets
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Hides or shows the system status/navigation bars. Extracted from the
 * identical toggleFullScreen() body that used to live separately in
 * TwoPageActivity, ThreePageActivity, and FourPageActivity.
 */
fun Activity.setSystemBarsHidden(hidden: Boolean) {
    val controller = WindowCompat.getInsetsController(window, window.decorView)
    if (hidden) {
        controller.hide(WindowInsets.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    } else {
        controller.show(WindowInsets.Type.systemBars())
    }
}

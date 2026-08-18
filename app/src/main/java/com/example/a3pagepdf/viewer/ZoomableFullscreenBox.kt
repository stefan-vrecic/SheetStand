package com.example.a3pagepdf.viewer

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Wraps [content] with pinch-to-zoom, pan, and single-tap-to-toggle-fullscreen
 * gestures — this was identical code duplicated across TwoPageActivity,
 * ThreePageActivity, and FourPageActivity.
 */
@Composable
fun ZoomableFullscreenBox(
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) }
            .transformable(state = transformState)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY
            )
    ) {
        content()
    }
}

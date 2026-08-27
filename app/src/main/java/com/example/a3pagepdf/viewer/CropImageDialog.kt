package com.example.a3pagepdf.viewer

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt

/** Width/height ratio the favourites grid renders thumbnails at — see FavoritesGrid's tile Surface. */
const val FAVORITE_THUMBNAIL_ASPECT = 0.72f

/**
 * Minimal crop tool shown between picking an image (FavoritesGrid's "Add
 * Thumbnail") and actually saving it: drag the box to reposition, the slider
 * to resize, "Use Photo" to confirm. Deliberately not a full editor (no
 * rotation, no free-aspect resize) — just enough to frame a photo before it
 * becomes a tile thumbnail.
 *
 * The preview container is forced to [source]'s own aspect ratio (no
 * ContentScale letterboxing), so container-space maps to bitmap-space by a
 * single uniform scale factor — that's what keeps the final pixel-crop math
 * in [confirmCrop] simple.
 */
@Composable
fun CropImageDialog(
    source: Bitmap,
    onDismiss: () -> Unit,
    onConfirm: (Bitmap) -> Unit
) {
    val density = LocalDensity.current
    var containerSizePx by remember { mutableStateOf(IntSize.Zero) }
    var cropWidthPx by remember { mutableStateOf(0f) }
    var cropOffset by remember { mutableStateOf(Offset.Zero) }
    var sizeFraction by remember { mutableStateOf(0.9f) }

    val cropHeightPx = cropWidthPx / FAVORITE_THUMBNAIL_ASPECT

    fun clampOffset(offset: Offset, width: Float, height: Float): Offset {
        val maxX = (containerSizePx.width - width).coerceAtLeast(0f)
        val maxY = (containerSizePx.height - height).coerceAtLeast(0f)
        return Offset(offset.x.coerceIn(0f, maxX), offset.y.coerceIn(0f, maxY))
    }

    fun applySizeFraction(fraction: Float) {
        if (containerSizePx.width == 0) return
        val maxW = containerSizePx.width.toFloat()
        val maxH = containerSizePx.height.toFloat()
        var w = maxW * fraction
        var h = w / FAVORITE_THUMBNAIL_ASPECT
        if (h > maxH) {
            h = maxH
            w = h * FAVORITE_THUMBNAIL_ASPECT
        }
        val center = cropOffset + Offset(cropWidthPx / 2f, cropHeightPx / 2f)
        cropWidthPx = w
        cropOffset = clampOffset(center - Offset(w / 2f, h / 2f), w, h)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Crop thumbnail")
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(source.width.toFloat() / source.height.toFloat())
                        .onSizeChanged { newSize ->
                            containerSizePx = newSize
                            if (cropWidthPx == 0f) applySizeFraction(sizeFraction)
                        }
                ) {
                    Image(
                        bitmap = source.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxWidth().aspectRatio(source.width.toFloat() / source.height.toFloat())
                    )

                    if (cropWidthPx > 0f) {
                        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(source.width.toFloat() / source.height.toFloat())) {
                            val scrim = Color.Black.copy(alpha = 0.55f)
                            val boxTop = cropOffset.y
                            val boxBottom = cropOffset.y + cropHeightPx
                            val boxLeft = cropOffset.x
                            val boxRight = cropOffset.x + cropWidthPx
                            drawRect(scrim, topLeft = Offset(0f, 0f), size = Size(size.width, boxTop))
                            drawRect(scrim, topLeft = Offset(0f, boxBottom), size = Size(size.width, size.height - boxBottom))
                            drawRect(scrim, topLeft = Offset(0f, boxTop), size = Size(boxLeft, cropHeightPx))
                            drawRect(scrim, topLeft = Offset(boxRight, boxTop), size = Size(size.width - boxRight, cropHeightPx))
                            drawRect(
                                color = Color.White,
                                topLeft = cropOffset,
                                size = Size(cropWidthPx, cropHeightPx),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // Invisible drag target the size of the crop box itself —
                        // separate from the Canvas above (which is purely visual)
                        // so drag handling doesn't fight with the draw calls.
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(cropOffset.x.roundToInt(), cropOffset.y.roundToInt()) }
                                .size(
                                    width = with(density) { cropWidthPx.toDp() },
                                    height = with(density) { cropHeightPx.toDp() }
                                )
                                .pointerInput(containerSizePx, cropWidthPx) {
                                    detectDragGestures { change, drag ->
                                        change.consume()
                                        cropOffset = clampOffset(cropOffset + drag, cropWidthPx, cropHeightPx)
                                    }
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Size", fontSize = 12.sp)
                Slider(
                    value = sizeFraction,
                    onValueChange = {
                        sizeFraction = it
                        applySizeFraction(it)
                    },
                    valueRange = 0.3f..1f
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = {
                        onConfirm(confirmCrop(source, containerSizePx, cropOffset, cropWidthPx, cropHeightPx))
                    }) { Text("Use Photo") }
                }
            }
        }
    }
}

/** Maps the on-screen crop box back to bitmap pixels and cuts it out. */
private fun confirmCrop(
    source: Bitmap,
    containerSizePx: IntSize,
    cropOffset: Offset,
    cropWidthPx: Float,
    cropHeightPx: Float
): Bitmap {
    if (containerSizePx.width == 0 || containerSizePx.height == 0) return source
    val scaleX = source.width / containerSizePx.width.toFloat()
    val scaleY = source.height / containerSizePx.height.toFloat()
    val left = (cropOffset.x * scaleX).roundToInt().coerceIn(0, source.width - 1)
    val top = (cropOffset.y * scaleY).roundToInt().coerceIn(0, source.height - 1)
    val width = (cropWidthPx * scaleX).roundToInt().coerceIn(1, source.width - left)
    val height = (cropHeightPx * scaleY).roundToInt().coerceIn(1, source.height - top)
    return Bitmap.createBitmap(source, left, top, width, height)
}

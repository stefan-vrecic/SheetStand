package com.example.a3pagepdf.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard M3 Button defaults: 24.dp/8.dp content padding, 40.dp min height, 58.dp min width.
 * This trims all of those down by roughly 15% for a more compact control bar,
 * plus another ~10% on top of that (size and text) so a full row of these —
 * Open/Play/prev/next/BPM/clock — fits without crowding.
 */
@Composable
fun CompactButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minWidth = 44.dp, minHeight = 31.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
    ) {
        ProvideTextStyle(LocalTextStyle.current.copy(fontSize = LocalTextStyle.current.fontSize * 0.9f)) {
            content()
        }
    }
}

/**
 * A further ~33% size reduction from stock M3 Button (58.dp/40.dp min
 * size, 24.dp/8.dp content padding) — its own tier, not built on top of
 * [CompactButton]. Used for PdfViewerTopBar's Open PDF / Jump / Prev / Next
 * buttons, freed up specifically to give AudioSeekerControl more breathing
 * room in that same row.
 */
@Composable
fun SmallActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minWidth = 39.dp, minHeight = 27.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 5.dp)
    ) {
        ProvideTextStyle(LocalTextStyle.current.copy(fontSize = LocalTextStyle.current.fontSize * 0.8f)) {
            content()
        }
    }
}

@Composable
fun ClockIcon(modifier: Modifier = Modifier, sizeDp: Dp = 16.dp, color: Color = Color.White) {
    Canvas(modifier = modifier.size(sizeDp)) {
        val strokeWidth = 2f
        val radius = (sizeDp.toPx() / 2f) - strokeWidth
        val center = Offset(sizeDp.toPx() / 2f, sizeDp.toPx() / 2f)
        drawCircle(color = color, radius = radius, center = center, style = Stroke(width = strokeWidth))
        // hour hand (pointing up)
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x, center.y - radius * 0.5f),
            strokeWidth = strokeWidth
        )
        // minute hand (pointing right)
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x + radius * 0.65f, center.y),
            strokeWidth = strokeWidth
        )
    }
}

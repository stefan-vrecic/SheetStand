package com.example.a3pagepdf.viewer

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Top control row shared by every windowed PDF viewer mode: Open PDF /
 * Jump-increment cycle / Prev / Next / page indicator. [trailingContent]
 * is an optional slot appended after everything else (right-aligned by
 * its own Spacer(weight) if it wants to be), for mode-specific extras —
 * e.g. TwoPageActivity uses it for the metronome controls.
 */
@Composable
fun PdfViewerTopBar(
    controller: MultiPagePdfController,
    onOpenPdf: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SmallActionButton(onClick = onOpenPdf) {
            Text("Open PDF")
        }
        Spacer(modifier = Modifier.width(4.dp))
        SmallActionButton(onClick = { controller.cycleJumpIncrement() }) {
            Text("Jump: ${controller.jumpIncrement}")
        }
        Spacer(modifier = Modifier.width(8.dp))
        SmallActionButton(onClick = { controller.moveWindow(-controller.jumpIncrement) }) {
            Text("◀ Prev")
        }
        Spacer(modifier = Modifier.width(4.dp))
        SmallActionButton(onClick = { controller.moveWindow(controller.jumpIncrement) }) {
            Text("Next ▶")
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (controller.pageCount > 0) {
            val windowSize = minOf(controller.windowSize, controller.pageCount)
            Text(
                if (windowSize == 1) "Page ${controller.start} of ${controller.pageCount}"
                else "Pages ${controller.start}-${controller.endPage} of ${controller.pageCount}"
            )
        }
        FavoriteStarButton(uri = controller.currentUri, mode = controller.mode)
        Spacer(modifier = Modifier.width(4.dp))
        SessionTimerButton()

        trailingContent()
    }
}

package com.example.a3pagepdf.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The Home screen's mode-picker card stack + "Share PDF via Gmail" button.
 * Pulled out of HomeActivity itself (which otherwise ends up owning this UI,
 * the favourites-card wrapper, *and* the "Open with" external-PDF flow all in
 * one class) — this composable is pure UI, driven entirely by the callbacks
 * passed in, so HomeActivity just wires each one to a startActivity() call.
 */
@Composable
fun HomeModeMenu(
    onOpenTwoPage: () -> Unit,
    onOpenThreePage: () -> Unit,
    onOpenFourPage: () -> Unit,
    onOpenAutoScroll: () -> Unit,
    onShareViaGmail: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        ModeCard(
            badgeText = "2",
            title = "2-Page Mode",
            subtitle = "Side-by-side spread",
            onClick = onOpenTwoPage
        )
        Spacer(modifier = Modifier.height(14.dp))

        ModeCard(
            badgeText = "3",
            title = "3-Page Mode",
            subtitle = "Three pages across",
            onClick = onOpenThreePage
        )
        Spacer(modifier = Modifier.height(14.dp))

        ModeCard(
            badgeText = "4",
            title = "4-Page Mode",
            subtitle = "2x2 grid layout",
            onClick = onOpenFourPage
        )
        Spacer(modifier = Modifier.height(14.dp))

        ModeCard(
            badgeIcon = Icons.Default.PlayArrow,
            title = "Auto-Scroll Mode",
            subtitle = "Hands-free continuous scroll",
            onClick = onOpenAutoScroll
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onShareViaGmail,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9800),
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share PDF via Gmail")
        }
    }
}

/** One row in [HomeModeMenu] — a badge (icon or number), title/subtitle, and a chevron. */
@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    badgeText: String? = null,
    badgeIcon: ImageVector? = null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (badgeIcon != null) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else if (badgeText != null) {
                    Text(
                        text = badgeText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "›",
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

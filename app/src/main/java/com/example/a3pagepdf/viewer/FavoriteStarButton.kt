package com.example.a3pagepdf.viewer

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Star toggle for favouriting whatever PDF is currently open in a viewer's
 * top bar. Fully self-contained — reads/writes [FavoritesStore] straight off
 * [uri] — so it drops into any top bar (PdfViewerTopBar, AutoScrollTopBar,
 * ...) without the hosting Activity needing to know favourites exist.
 *
 * [uri] is null when no PDF is open yet; the button just disables itself.
 * [mode] (see [PdfViewerMode]) is recorded on the favourite so HomeActivity
 * can relaunch it in the same viewer without asking.
 */
@Composable
fun FavoriteStarButton(uri: Uri?, mode: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isFavorite by remember(uri) {
        mutableStateOf(uri?.let { FavoritesStore.isFavorite(context, it) } ?: false)
    }

    IconButton(
        onClick = {
            val current = uri ?: return@IconButton
            if (isFavorite) {
                FavoritesStore.remove(context, current)
                isFavorite = false
            } else {
                val name = FavoritesStore.queryDisplayName(context, current)
                if (FavoritesStore.add(context, current, name, mode)) {
                    isFavorite = true
                } else {
                    Toast.makeText(context, "Favourites is full", Toast.LENGTH_SHORT).show()
                }
            }
        },
        enabled = uri != null,
        modifier = modifier.size(40.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = if (isFavorite) "Remove from Favourites" else "Add to Favourites",
            tint = when {
                isFavorite -> Color(0xFFFFC107)
                uri == null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            },
            modifier = Modifier.size(22.dp)
        )
    }
}

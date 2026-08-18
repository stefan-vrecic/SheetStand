package com.example.a3pagepdf.viewer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Label shown for each option in the "which viewer?" dialog, paired with its [PdfViewerMode] value. */
private val MODE_OPTIONS = listOf(
    "2-Page Mode" to PdfViewerMode.TWO_PAGE,
    "3-Page Mode" to PdfViewerMode.THREE_PAGE,
    "4-Page Mode" to PdfViewerMode.FOUR_PAGE,
    "Auto-Scroll Mode" to PdfViewerMode.AUTO_SCROLL
)

/**
 * A fixed 2-row x 4-column grid of favourited PDFs, meant to sit at the
 * bottom of HomeActivity. Filled slots show a first-page thumbnail and open
 * the PDF on tap; the first empty slot is always an "add" tile that launches
 * a document picker (wired up by the caller via [onAddClick]). Long-press on
 * a filled tile asks to remove it from favourites; "Clear all" asks to wipe
 * every favourite. Tapping a tile whose [FavoriteItem.mode] is unknown (it
 * was added from HomeActivity directly, not starred from a viewer) asks
 * which viewer to open it in via [onOpenWithMode].
 *
 * Stateless/host-driven on purpose so HomeActivity owns the favourites list
 * and persistence — this component only renders it and reports intent.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritesGrid(
    favorites: List<FavoriteItem>,
    onOpen: (FavoriteItem) -> Unit,
    onOpenWithMode: (FavoriteItem, String) -> Unit,
    onAddClick: () -> Unit,
    onRemove: (FavoriteItem) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 4,
    rows: Int = 2
) {
    val totalSlots = columns * rows
    var pendingRemoval by remember { mutableStateOf<FavoriteItem?>(null) }
    var pendingModeChoice by remember { mutableStateOf<FavoriteItem?>(null) }
    var confirmClearAll by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
            Text(
                text = "My Favourites",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (favorites.isNotEmpty()) {
                TextButton(
                    onClick = { confirmClearAll = true },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text(
                        text = "Clear all",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        for (row in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (row < rows - 1) 12.dp else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    Box(modifier = Modifier.weight(1f)) {
                        when {
                            index < favorites.size -> {
                                val item = favorites[index]
                                FavoriteTile(
                                    item = item,
                                    onClick = {
                                        if (item.mode == null) pendingModeChoice = item else onOpen(item)
                                    },
                                    onLongClick = { pendingRemoval = item }
                                )
                            }
                            index == favorites.size && favorites.size < totalSlots -> AddFavoriteTile(onClick = onAddClick)
                            else -> EmptyFavoriteSlot()
                        }
                    }
                }
            }
        }
    }

    pendingRemoval?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove from Favourites?") },
            text = { Text(item.name) },
            confirmButton = {
                TextButton(onClick = {
                    onRemove(item)
                    pendingRemoval = null
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) { Text("Cancel") }
            }
        )
    }

    pendingModeChoice?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingModeChoice = null },
            title = { Text("Open in which mode?") },
            text = {
                Column {
                    MODE_OPTIONS.forEach { (label, mode) ->
                        TextButton(
                            onClick = {
                                onOpenWithMode(item, mode)
                                pendingModeChoice = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pendingModeChoice = null }) { Text("Cancel") }
            }
        )
    }

    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text("Clear all favourites?") },
            text = { Text("This removes all ${favorites.size} favourited PDFs. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearAll()
                    confirmClearAll = false
                }) { Text("Clear all") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAll = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteTile(
    item: FavoriteItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    var thumbnail by remember(item.uri) { mutableStateOf(FavoriteThumbnailLoader.cache[item.uri.toString()]) }

    LaunchedEffect(item.uri) {
        if (thumbnail == null) {
            thumbnail = FavoriteThumbnailLoader.thumbnailFor(context, item.uri)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .aspectRatio(0.72f)
        ) {
            val bmp = thumbnail
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.4f)
                    )
                }
            }
        }
        Text(
            text = item.name,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        )
    }
}

@Composable
private fun AddFavoriteTile(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .aspectRatio(0.72f)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add favourite",
                tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun EmptyFavoriteSlot() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.08f))
    )
}

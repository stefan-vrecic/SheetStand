package com.example.a3pagepdf.viewer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TILE_WIDTH = 132.dp
private val TILE_ROW_HEIGHT = 220.dp

// Named (rather than a literal repeated in both the LazyHorizontalGrid's
// verticalArrangement and its height calculation below) so the two can never
// drift out of sync — a mismatch there would either clip row 2 or leave
// unexplained extra blank space under it.
private val ROW_SPACING = 8.dp

/**
 * Reorders [items] so that feeding the result into a column-major grid
 * (e.g. LazyHorizontalGrid with GridCells.Fixed([rows])) reads, row by row
 * left-to-right, in [items]' original order — i.e. undoes the grid's native
 * "fill this column top-to-bottom, then move to the next one" placement, so
 * a pre-sorted (e.g. alphabetised) list still *looks* sorted when scanned
 * the way a person actually reads a grid.
 */
private fun <T> toRowMajorReadingOrder(items: List<T>, rows: Int): List<T> {
    if (items.size <= 1 || rows <= 1) return items
    val columns = (items.size + rows - 1) / rows
    return List(items.size) { k ->
        val col = k / rows
        val row = k % rows
        items[row * columns + col]
    }
}

/**
 * A horizontally-scrolling, fixed-2-row grid of favourited PDFs (alphabetised
 * by name), meant to sit at the bottom of HomeActivity. There's no hard cap —
 * favouriting more just adds another column and the grid scrolls to reach it,
 * rather than the old fixed-8-slot layout silently refusing to add a 9th.
 * Filled tiles show a first-page thumbnail and open the PDF on tap; the last
 * tile is always an "add" tile that launches a document picker (wired up by
 * the caller via [onAddClick]). Long-press on a filled tile offers a choice of
 * Rename (via [onRename] — see [PdfDisplayNames], this also updates whatever's
 * shown in the viewer that PDF was opened from), Add Thumbnail (picks an image
 * to override the auto-rendered first-page thumbnail — see [CustomThumbnails]),
 * or removing it from favourites; "Clear all" asks to wipe every favourite. Tapping a tile
 * whose [FavoriteItem.mode] is unknown (it was added from HomeActivity
 * directly, not starred from a viewer) asks which viewer to open it in via
 * [onOpenWithMode].
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
    onRename: (FavoriteItem, String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
    rows: Int = 2
) {
    val context = LocalContext.current

    // Long-press lands here first (a choice), rather than jumping straight to
    // the remove confirmation — Rename/Add Thumbnail/Remove each pick their
    // own pending-item state instead.
    var pendingLongPressItem by remember { mutableStateOf<FavoriteItem?>(null) }
    var pendingRenameItem by remember { mutableStateOf<FavoriteItem?>(null) }
    var pendingThumbnailItem by remember { mutableStateOf<FavoriteItem?>(null) }
    var pendingRemoval by remember { mutableStateOf<FavoriteItem?>(null) }
    var pendingModeChoice by remember { mutableStateOf<FavoriteItem?>(null) }
    var confirmClearAll by remember { mutableStateOf(false) }

    // Holds the decoded (pre-crop) image + which favourite it's for, from the
    // moment the picker returns until the crop dialog is dismissed/confirmed.
    var pendingCrop by remember { mutableStateOf<Pair<FavoriteItem, Bitmap>?>(null) }

    // Fires once the user picks an image from "Add Thumbnail" — decodes it and
    // hands it to CropImageDialog rather than saving it straight away.
    val thumbnailPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { imageUri ->
        val item = pendingThumbnailItem
        pendingThumbnailItem = null
        if (imageUri == null || item == null) return@rememberLauncherForActivityResult
        val decoded = try {
            context.contentResolver.openInputStream(imageUri)?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        }
        if (decoded != null) {
            pendingCrop = item to decoded
        }
    }

    // Display-only ordering — doesn't touch persisted (add-order) storage, so
    // nothing else that reads FavoritesStore needs to change. Also reordered
    // for the grid's *native* column-major fill (LazyHorizontalGrid fills
    // top-to-bottom within a column before starting the next one): feeding it
    // a plain alphabetical list would make row 0 read every-other-item
    // instead of the first N names in order. toRowMajorReadingOrder() undoes
    // that so scanning left-to-right, row by row, actually reads alphabetically.
    val sortedFavorites = toRowMajorReadingOrder(favorites.sortedBy { it.name.lowercase() }, rows)

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Text(
                text = "Favourites",
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

        LazyHorizontalGrid(
            rows = GridCells.Fixed(rows),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
            modifier = Modifier
                .fillMaxWidth()
                .height(TILE_ROW_HEIGHT * rows + ROW_SPACING * (rows - 1))
        ) {
            items(sortedFavorites, key = { it.uri.toString() }) { item ->
                FavoriteTile(
                    item = item,
                    onClick = {
                        if (item.mode == null) pendingModeChoice = item else onOpen(item)
                    },
                    onLongClick = { pendingLongPressItem = item },
                    modifier = Modifier.width(TILE_WIDTH).height(TILE_ROW_HEIGHT)
                )
            }
            item {
                AddFavoriteTile(onClick = onAddClick, modifier = Modifier.width(TILE_WIDTH).height(TILE_ROW_HEIGHT))
            }
        }
    }

    pendingLongPressItem?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingLongPressItem = null },
            title = { Text(item.name) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            pendingRenameItem = item
                            pendingLongPressItem = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Rename", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                    }
                    TextButton(
                        onClick = {
                            pendingThumbnailItem = item
                            pendingLongPressItem = null
                            thumbnailPicker.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Thumbnail", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                    }
                    TextButton(
                        onClick = {
                            pendingRemoval = item
                            pendingLongPressItem = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Remove from Favourites", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pendingLongPressItem = null }) { Text("Cancel") }
            }
        )
    }

    pendingCrop?.let { (item, source) ->
        CropImageDialog(
            source = source,
            onDismiss = { pendingCrop = null },
            onConfirm = { cropped ->
                val stored = CustomThumbnails.set(context, item.uri, cropped)
                FavoriteThumbnailLoader.cache[item.uri.toString()] = stored
                pendingCrop = null
            }
        )
    }

    pendingRenameItem?.let { item ->
        RenamePdfDialog(
            currentName = item.name,
            onDismiss = { pendingRenameItem = null },
            onConfirm = { newName ->
                onRename(item, newName)
                pendingRenameItem = null
            }
        )
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
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Read directly here (not stashed inside a remember{} initializer) so this
    // tile recomposes whenever this URI's entry in the cache changes — e.g.
    // "Add Thumbnail" writing a new bitmap straight into it. A remember{}
    // capture would've snapshotted the value once and never noticed that.
    val cachedThumbnail = FavoriteThumbnailLoader.cache[item.uri.toString()]
    var loadedThumbnail by remember(item.uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(item.uri) {
        if (cachedThumbnail == null) {
            loadedThumbnail = FavoriteThumbnailLoader.thumbnailFor(context, item.uri)
        }
    }

    val thumbnail = cachedThumbnail ?: loadedThumbnail

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
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
            fontWeight = FontWeight.Bold,
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
private fun AddFavoriteTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    // The fixed width/height from the grid call site is applied to this outer
    // Box, not straight to the Surface — otherwise aspectRatio has no room to
    // work (both dimensions already pinned) and the "+" square stretches to
    // fill the *entire* row height instead of matching the other tiles'
    // thumbnail-only height. Top-aligning the Surface here leaves blank space
    // below it, matching where a FavoriteTile's label sits.
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
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
}

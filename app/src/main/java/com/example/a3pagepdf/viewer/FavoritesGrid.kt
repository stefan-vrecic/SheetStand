package com.example.a3pagepdf.viewer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
 * Filled tiles ([FavoriteTile]) show a first-page thumbnail and open the PDF
 * on tap; the last tile ([AddFavoriteTile]) launches a document picker (wired
 * up by the caller via [onAddClick]). Long-press on a filled tile offers a
 * one-off "Open in [mode]" per viewer mode (via [onOpenOnceWithMode] — doesn't
 * touch the favourite's stored default mode, just this one open), plus
 * Rename, Add Thumbnail, or Remove — see [FavoriteActionDialogs], which owns
 * that entire flow (this composable just forwards the triggers into a
 * [FavoriteActionsState]). "Clear all" asks to wipe every favourite.
 * Tapping a tile whose [FavoriteItem.mode] is unknown (it was added from
 * HomeActivity directly, not starred from a viewer) asks which viewer to
 * open it in via [onOpenWithMode].
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
    onOpenOnceWithMode: (FavoriteItem, String) -> Unit,
    onAddClick: () -> Unit,
    onRemove: (FavoriteItem) -> Unit,
    onRename: (FavoriteItem, String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
    rows: Int = 2
) {
    val actionsState = rememberFavoriteActionsState()

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
                    onClick = { actionsState.confirmClearAll = true },
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
                        if (item.mode == null) actionsState.pendingModeChoice = item else onOpen(item)
                    },
                    onLongClick = { actionsState.pendingLongPressItem = item },
                    modifier = Modifier.width(TILE_WIDTH).height(TILE_ROW_HEIGHT)
                )
            }
            item {
                AddFavoriteTile(onClick = onAddClick, modifier = Modifier.width(TILE_WIDTH).height(TILE_ROW_HEIGHT))
            }
        }
    }

    FavoriteActionDialogs(
        state = actionsState,
        favoritesCount = favorites.size,
        onOpenWithMode = onOpenWithMode,
        onOpenOnceWithMode = onOpenOnceWithMode,
        onRemove = onRemove,
        onRename = onRename,
        onClearAll = onClearAll
    )
}

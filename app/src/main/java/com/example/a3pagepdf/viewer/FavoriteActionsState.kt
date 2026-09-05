package com.example.a3pagepdf.viewer

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Owns every "what's the long-press flow currently doing" bit of state for
 * FavoritesGrid — which dialog (if any) is showing, and which favourite it's
 * for. Pulled out into its own state holder (matching the pattern already
 * used for MetronomeState/AutoScrollState) so that state can be created here
 * via [rememberFavoriteActionsState] and shared between FavoritesGrid (which
 * triggers it — long-press, a mode-unknown tap, "Clear all") and
 * [FavoriteActionDialogs] (which renders whatever's currently pending),
 * without FavoritesGrid itself having to own five separate dialog states.
 */
class FavoriteActionsState {
    /** Long-press lands here first (a choice), rather than jumping straight to a single action. */
    var pendingLongPressItem by mutableStateOf<FavoriteItem?>(null)
    var pendingRenameItem by mutableStateOf<FavoriteItem?>(null)
    var pendingThumbnailItem by mutableStateOf<FavoriteItem?>(null)
    var pendingRemoval by mutableStateOf<FavoriteItem?>(null)
    var pendingModeChoice by mutableStateOf<FavoriteItem?>(null)
    var confirmClearAll by mutableStateOf(false)

    /** The decoded (pre-crop) image + which favourite it's for, from when the picker returns until the crop dialog resolves. */
    var pendingCrop by mutableStateOf<Pair<FavoriteItem, Bitmap>?>(null)
}

@Composable
fun rememberFavoriteActionsState(): FavoriteActionsState = remember { FavoriteActionsState() }

package com.example.a3pagepdf.viewer

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign

/**
 * Renders whichever dialog [state] currently has pending — the long-press
 * action menu (Rename / Add Thumbnail / Remove) and everything each of those
 * three can lead to, plus the mode-unknown-tap chooser and "Clear all"
 * confirmation. Split out of FavoritesGrid so that file can stay focused on
 * just the grid layout; this owns the entire action-flow instead.
 */
@Composable
fun FavoriteActionDialogs(
    state: FavoriteActionsState,
    favoritesCount: Int,
    onOpenWithMode: (FavoriteItem, String) -> Unit,
    onRemove: (FavoriteItem) -> Unit,
    onRename: (FavoriteItem, String) -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current

    // Fires once the user picks an image from "Add Thumbnail" — decodes it and
    // hands it to CropImageDialog rather than saving it straight away.
    val thumbnailPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { imageUri ->
        val item = state.pendingThumbnailItem
        state.pendingThumbnailItem = null
        if (imageUri == null || item == null) return@rememberLauncherForActivityResult
        val decoded = try {
            context.contentResolver.openInputStream(imageUri)?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        }
        if (decoded != null) {
            state.pendingCrop = item to decoded
        }
    }

    state.pendingLongPressItem?.let { item ->
        AlertDialog(
            onDismissRequest = { state.pendingLongPressItem = null },
            title = { Text(item.name) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            state.pendingRenameItem = item
                            state.pendingLongPressItem = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Rename", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                    }
                    TextButton(
                        onClick = {
                            state.pendingThumbnailItem = item
                            state.pendingLongPressItem = null
                            thumbnailPicker.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Thumbnail", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                    }
                    TextButton(
                        onClick = {
                            state.pendingRemoval = item
                            state.pendingLongPressItem = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Remove from Favourites", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { state.pendingLongPressItem = null }) { Text("Cancel") }
            }
        )
    }

    state.pendingCrop?.let { (item, source) ->
        CropImageDialog(
            source = source,
            onDismiss = { state.pendingCrop = null },
            onConfirm = { cropped ->
                val stored = CustomThumbnails.set(context, item.uri, cropped)
                FavoriteThumbnailLoader.cache[item.uri.toString()] = stored
                state.pendingCrop = null
            }
        )
    }

    state.pendingRenameItem?.let { item ->
        RenamePdfDialog(
            currentName = item.name,
            onDismiss = { state.pendingRenameItem = null },
            onConfirm = { newName ->
                onRename(item, newName)
                state.pendingRenameItem = null
            }
        )
    }

    state.pendingRemoval?.let { item ->
        AlertDialog(
            onDismissRequest = { state.pendingRemoval = null },
            title = { Text("Remove from Favourites?") },
            text = { Text(item.name) },
            confirmButton = {
                TextButton(onClick = {
                    onRemove(item)
                    state.pendingRemoval = null
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { state.pendingRemoval = null }) { Text("Cancel") }
            }
        )
    }

    state.pendingModeChoice?.let { item ->
        ModePickerDialog(
            onDismiss = { state.pendingModeChoice = null },
            onSelect = { mode ->
                onOpenWithMode(item, mode)
                state.pendingModeChoice = null
            }
        )
    }

    if (state.confirmClearAll) {
        AlertDialog(
            onDismissRequest = { state.confirmClearAll = false },
            title = { Text("Clear all favourites?") },
            text = { Text("This removes all $favoritesCount favourited PDFs. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearAll()
                    state.confirmClearAll = false
                }) { Text("Clear all") }
            },
            dismissButton = {
                TextButton(onClick = { state.confirmClearAll = false }) { Text("Cancel") }
            }
        )
    }
}

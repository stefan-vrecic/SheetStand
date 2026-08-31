package com.example.a3pagepdf.viewer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

/**
 * Shared "Open in which mode?" chooser, built off [MODE_OPTIONS]. Used by two
 * otherwise-unrelated flows — a favourite whose mode isn't known yet
 * (FavoritesGrid) and a PDF opened externally via the system's "Open with"
 * chooser (HomeActivity) — pulled out into one place so those two dialogs
 * can't drift apart from each other.
 */
@Composable
fun ModePickerDialog(onDismiss: () -> Unit, onSelect: (mode: String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open in which mode?") },
        text = {
            Column {
                MODE_OPTIONS.forEach { (label, mode) ->
                    TextButton(
                        onClick = { onSelect(mode) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

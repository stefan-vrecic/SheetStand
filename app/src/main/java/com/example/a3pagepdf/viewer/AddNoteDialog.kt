package com.example.a3pagepdf.viewer

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

/**
 * Text-entry dialog for a freshly-placed note, used by [PdfPageList]. Its own
 * composable (was inlined before) to fix two real gaps in the old version:
 * the field never grabbed focus/the keyboard on open — so "tap page, dialog
 * opens, start typing" silently did nothing until you *also* tapped the
 * field, which read as the whole feature being broken — and "Add" stayed
 * tappable (and silently no-op'd) with empty/whitespace-only text instead of
 * just being disabled.
 */
@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (trimmedText: String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val trimmed = text.trim()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add text") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Note") },
                modifier = Modifier.focusRequester(focusRequester)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(trimmed) },
                enabled = trimmed.isNotEmpty()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

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
 * Text-entry dialog for the full URL one of [RemoteControlButton]'s chips
 * hits (see [RemoteControlPrefs]) — shown either on long-press (to
 * reconfigure) or on a plain tap when nothing's been entered yet.
 * Deliberately takes a whole URL rather than just a host — e.g. a Keyboard
 * Maestro Public Web trigger link
 * (`http://<mac-ip>:4490/action.html?macro=<uuid>&value=1` — the `macro`
 * key takes the macro's UUID, not its name) bakes the macro identifier into
 * the path/query, so there's no single fixed path this app could append on
 * its own.
 *
 * [currentAddress] and [initialText] are deliberately separate: the former
 * is what's actually saved (used only to decide whether Save should be
 * enabled — editing back to the same value shouldn't count as a change),
 * the latter is what the field opens with. They differ specifically for the
 * pause chip, which prefills a *suggested* URL (see
 * [RemoteControlPrefs.suggestedPauseAddress]) when nothing's saved yet — if
 * both params were the same, accepting that suggestion unmodified would
 * look like "no change" and leave Save disabled.
 */
@Composable
fun RemoteControlAddressDialog(
    currentAddress: String,
    onDismiss: () -> Unit,
    onConfirm: (address: String) -> Unit,
    initialText: String = currentAddress,
    title: String = "Remote-control URL"
) {
    var text by remember { mutableStateOf(initialText) }
    val trimmed = text.trim()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("URL") },
                placeholder = { Text("192.168.1.23:4490/action.html?macro=44BF2EFD-CDF2-4C66-89C7-157EA01A2668&value=1") },
                singleLine = true,
                modifier = Modifier.focusRequester(focusRequester)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(trimmed) },
                enabled = trimmed.isNotEmpty() && trimmed != currentAddress
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

package com.example.a3pagepdf.viewer

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Row of four small chips (styled like PdfPageList's "Note" chip, not
 * Material IconButtons — this app only pulls in material-icons-core, and
 * none of its icons read as "remote control") that each hit their own
 * independently-saved URL on the same local network: "PC" (play), "⏸"
 * (pause) — matching the play/pause glyph convention already used in
 * AutoScrollTopBar — "−" (minus), "+" (plus). The motivating case is a
 * single Keyboard Maestro macro triggered with a different `value=N` per
 * chip (1/0/2/3), but this app has no opinion on what's on the other end of
 * any of them; see [sendRemoteToggle].
 *
 * [visible] is a plain parameter rather than its own persisted setting for
 * now, per explicit ask — there's a single call site (PdfPageList, in
 * Auto-Scroll mode only; other modes may get it later), so a real settings
 * toggle would be premature. Wiring one in later just means threading a
 * stored Boolean into this param instead of the current default of `true`.
 *
 * Self-contained: each chip owns its own address-entry dialog and
 * reads/writes [RemoteControlPrefs] directly, same shape as
 * FavoriteStarButton.
 */
@Composable
fun RemoteControlButton(
    visible: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        RemoteControlChip(
            label = "PC",
            dialogTitle = "Play URL",
            getAddress = RemoteControlPrefs::getPlayAddress,
            setAddress = RemoteControlPrefs::setPlayAddress,
            suggestedAddress = { "" }
        )
        RemoteControlChip(
            label = "⏸",
            dialogTitle = "Pause URL",
            getAddress = RemoteControlPrefs::getPauseAddress,
            setAddress = RemoteControlPrefs::setPauseAddress,
            suggestedAddress = RemoteControlPrefs::suggestedPauseAddress
        )
        RemoteControlChip(
            label = "−",
            dialogTitle = "Minus URL",
            getAddress = RemoteControlPrefs::getMinusAddress,
            setAddress = RemoteControlPrefs::setMinusAddress,
            suggestedAddress = RemoteControlPrefs::suggestedMinusAddress
        )
        RemoteControlChip(
            label = "+",
            dialogTitle = "Plus URL",
            getAddress = RemoteControlPrefs::getPlusAddress,
            setAddress = RemoteControlPrefs::setPlusAddress,
            suggestedAddress = RemoteControlPrefs::suggestedPlusAddress
        )
    }
}

/** One chip shared by both halves of [RemoteControlButton] — differs only in which pref key it reads/writes. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RemoteControlChip(
    label: String,
    dialogTitle: String,
    getAddress: (Context) -> String?,
    setAddress: (Context, String) -> Unit,
    suggestedAddress: (Context) -> String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAddressDialog by remember { mutableStateOf(false) }

    Text(
        text = label,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(4.dp)
            )
            .combinedClickable(
                onClick = {
                    val address = getAddress(context)
                    if (address == null) {
                        // Nothing configured yet — a plain tap can't do
                        // anything useful, so go straight to asking for one
                        // instead of silently no-op'ing.
                        showAddressDialog = true
                    } else {
                        scope.launch { sendRemoteToggle(address) }
                    }
                },
                onLongClick = { showAddressDialog = true }
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )

    if (showAddressDialog) {
        val saved = getAddress(context) ?: ""
        RemoteControlAddressDialog(
            currentAddress = saved,
            initialText = saved.ifEmpty { suggestedAddress(context) },
            title = dialogTitle,
            onDismiss = { showAddressDialog = false },
            onConfirm = { address ->
                setAddress(context, address)
                showAddressDialog = false
            }
        )
    }
}

package com.example.a3pagepdf.viewer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * @param onActivate called right before the timer is turned on, so callers
 * (e.g. the auto-scroll controls) can stop any conflicting behavior first.
 */
@Composable
fun TimerControl(state: PageTimerState, pageCount: Int, onActivate: () -> Unit) {
    if (state.isActive) {
        Text(
            text = formatTime(state.timeRemaining),
            fontSize = 14.sp,
            modifier = Modifier.padding(end = 8.dp)
        )
    }

    Box {
        CompactButton(onClick = { state.menuExpanded = true }) {
            ClockIcon(color = MaterialTheme.colorScheme.onPrimary)
            if (state.isActive) {
                Spacer(modifier = Modifier.width(4.dp))
                Text("●")
            }
        }
        DropdownMenu(
            expanded = state.menuExpanded,
            onDismissRequest = { state.menuExpanded = false }
        ) {
            CompactButton(
                onClick = {
                    if (!state.isActive) onActivate()
                    state.isActive = !state.isActive
                    state.menuExpanded = false
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(if (state.isActive) "Stop Timer" else "Start Timer")
            }

            HorizontalDivider()

            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .width(200.dp)
                    .padding(8.dp)
            ) {
                for (page in 1..pageCount) {
                    val currentValue = state.pageDurations[page]?.toString() ?: ""

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text("Page $page:", modifier = Modifier.width(70.dp))
                        OutlinedTextField(
                            value = currentValue,
                            onValueChange = { newText ->
                                val seconds = newText.filter { it.isDigit() }.toIntOrNull()
                                if (seconds != null) {
                                    state.setPageDuration(page, seconds, pageCount)
                                } else if (newText.isEmpty()) {
                                    state.clearPageDuration(page)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            suffix = { Text("s") },
                            singleLine = true,
                            modifier = Modifier.width(110.dp)
                        )
                    }
                }
            }
        }
    }
}

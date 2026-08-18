package com.example.a3pagepdf.viewer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MetronomeControl(state: MetronomeState) {
    Box {
        CompactButton(onClick = { state.menuExpanded = true }) {
            Text(if (state.isOn) "BPM ●" else "BPM ▾")
        }
        DropdownMenu(
            expanded = state.menuExpanded,
            onDismissRequest = { state.menuExpanded = false }
        ) {
            Column(modifier = Modifier.padding(12.dp).width(220.dp)) {
                CompactButton(onClick = {
                    state.isOn = !state.isOn
                    state.isPaused = false
                    state.menuExpanded = false
                }) {
                    Text(if (state.isOn) "Stop Metronome" else "Start Metronome")
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("BPM: ${state.bpm.toInt()}", fontSize = 14.sp)
                Slider(
                    value = state.bpm,
                    onValueChange = { state.onBpmSliderChanged(it) },
                    valueRange = 40f..240f
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Time Signature", fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.timeSigNumerator,
                        onValueChange = { txt ->
                            state.timeSigNumerator = txt.filter { it.isDigit() }.take(2)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.width(70.dp)
                    )
                    Text(" / ", fontSize = 18.sp, modifier = Modifier.padding(horizontal = 4.dp))
                    OutlinedTextField(
                        value = state.timeSigDenominator,
                        onValueChange = { txt ->
                            state.timeSigDenominator = txt.filter { it.isDigit() }.take(2)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.width(70.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                CompactButton(
                    onClick = { state.registerTap() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(state.tapFeedback)
                }
            }
        }
    }
}

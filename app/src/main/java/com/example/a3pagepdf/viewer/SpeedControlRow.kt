package com.example.a3pagepdf.viewer

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Speed input for the pixel auto-scroll, plus the 3s-delay checkbox, tempo multiplier, and boundary-jump px field. */
@Composable
fun SpeedControlRow(state: AutoScrollState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Speed:", fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            BasicTextField(
                value = state.speedText,
                onValueChange = { txt ->
                    val filtered = txt.filter { it.isDigit() }.take(3)
                    state.speedText = filtered
                    // Only push a fully-typed, in-range value into `speed` (and
                    // thus the slider/scroll loop) — a bare "1" mid-type
                    // shouldn't yank the slider to its 10f floor.
                    filtered.toFloatOrNull()?.let { parsed ->
                        state.speed = parsed.coerceIn(10f, 300f)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.width(36.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text("px/s", fontSize = 14.sp)
        Slider(
            value = state.speed,
            onValueChange = {
                state.speed = it
                state.speedText = it.toInt().toString()
            },
            valueRange = 10f..300f,
            modifier = Modifier
                .width(300.dp)
                .padding(horizontal = 8.dp)
        )
        Checkbox(
            checked = state.delayEnabled,
            onCheckedChange = { state.delayEnabled = it }
        )
        Text("3s Delay?", fontSize = 14.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Text("Tempo:", fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            BasicTextField(
                value = state.tempoMultiplierText,
                onValueChange = { txt ->
                    state.tempoMultiplierText = txt.filter { it.isDigit() || it == '.' }.take(4)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.width(36.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        // Quiet readout of the actual scroll rate `speed * tempoMultiplier`
        // resolves to — without it, telling "180 @ 0.8" apart from "144 @ 1.0"
        // takes doing the multiplication in your head.
        Text(
            "→ ${(state.speed * state.tempoMultiplier).toInt()} px/s",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text("Jump (px):", fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            BasicTextField(
                value = state.jumpPxText,
                onValueChange = { txt ->
                    state.jumpPxText = txt.filter { it.isDigit() }.take(4)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.width(36.dp)
            )
        }
    }
}

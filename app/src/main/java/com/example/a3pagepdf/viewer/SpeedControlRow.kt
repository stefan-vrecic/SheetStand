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

/** Speed slider for the pixel auto-scroll, plus the 3s-delay checkbox and boundary-jump px field. */
@Composable
fun SpeedControlRow(state: AutoScrollState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Speed: ${state.speed.toInt()} px/s", fontSize = 14.sp)
        Slider(
            value = state.speed,
            onValueChange = { state.speed = it },
            valueRange = 10f..300f,
            modifier = Modifier
                .width(360.dp)
                .padding(horizontal = 8.dp)
        )
        Checkbox(
            checked = state.delayEnabled,
            onCheckedChange = { state.delayEnabled = it }
        )
        Text("3s Delay?", fontSize = 14.sp)
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

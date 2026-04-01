package com.tracydz.patterns.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracydz.patterns.model.WindData

@Composable
fun WindBar(
    wind: WindData,
    onWindChange: (WindData) -> Unit,
    isLoading: Boolean,
    onAutoFetch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fieldStyle = TextStyle(
        color = Color.White,
        fontSize = 16.sp,
        textAlign = TextAlign.Center
    )
    val fieldModifier = Modifier
        .width(52.dp)
        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
        .padding(horizontal = 6.dp, vertical = 8.dp)

    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
            .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            Icons.Default.Air, contentDescription = "Wind",
            tint = Color.White, modifier = Modifier.size(20.dp)
        )

        if (wind.speedKts > 0 && wind.directionFrom > 0) {
            Icon(
                Icons.Default.Navigation,
                contentDescription = "Wind direction",
                tint = Color(0xFF4FC3F7),
                modifier = Modifier
                    .size(22.dp)
                    .rotate(wind.directionFrom.toFloat() + 180f)
            )
        }

        BasicTextField(
            value = if (wind.speedKts > 0) wind.speedKts.toInt().toString() else "",
            onValueChange = { text ->
                val filtered = text.filter { it.isDigit() }.take(3)
                onWindChange(wind.copy(speedKts = filtered.toDoubleOrNull() ?: 0.0))
            },
            modifier = fieldModifier,
            textStyle = fieldStyle,
            singleLine = true,
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            decorationBox = { inner ->
                if (wind.speedKts <= 0) {
                    Text("spd", style = fieldStyle.copy(color = Color.Gray))
                }
                inner()
            }
        )

        Text("kts", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
        Text("from", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)

        BasicTextField(
            value = if (wind.directionFrom > 0) wind.directionFrom.toInt().toString() else "",
            onValueChange = { text ->
                val filtered = text.filter { it.isDigit() }.take(3)
                onWindChange(wind.copy(directionFrom = filtered.toDoubleOrNull() ?: 0.0))
            },
            modifier = fieldModifier,
            textStyle = fieldStyle,
            singleLine = true,
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            decorationBox = { inner ->
                if (wind.directionFrom <= 0) {
                    Text("dir", style = fieldStyle.copy(color = Color.Gray))
                }
                inner()
            }
        )

        Text("°", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)

        IconButton(onClick = onAutoFetch, enabled = !isLoading) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White, strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Refresh, contentDescription = "Auto-fetch wind", tint = Color.White)
            }
        }
    }
}

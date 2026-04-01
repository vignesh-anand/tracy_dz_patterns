package com.tracydz.patterns.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkScheme = darkColorScheme(
    primary = SkyBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = DarkSky,
    surface = SurfaceDark,
)

@Composable
fun DZPatternsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = Typography,
        content = content
    )
}

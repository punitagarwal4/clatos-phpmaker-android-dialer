package com.clatos.dialer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Clatos brand palette (blue, matching the launcher icon).
private val BrandBlue = Color(0xFF0B5FFF)
private val BrandBlueDark = Color(0xFF0A3FB0)
private val BrandTeal = Color(0xFF00B8A9)

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    secondary = BrandTeal,
    primaryContainer = Color(0xFFD8E2FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondaryContainer = Color(0xFFCFEFEA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFADC6FF),
    onPrimary = Color(0xFF002E69),
    secondary = BrandTeal,
    primaryContainer = BrandBlueDark,
    onPrimaryContainer = Color(0xFFD8E2FF),
    secondaryContainer = Color(0xFF0E4D47),
)

@Composable
fun ClatosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

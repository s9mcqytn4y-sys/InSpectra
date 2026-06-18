package com.primaraya.inspectra.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LightColorScheme = lightColorScheme(
    primary = CorporateBlue,
    secondary = SafetyAmber,
    background = SurfaceLight,
    surface = Color.White
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    secondary = SafetyAmber,
    background = SurfaceDark,
    surface = Color(0xFF1E293B)
)

@Composable
fun InSpectraTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

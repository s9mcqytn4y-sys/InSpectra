package com.primaraya.inspectra.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueContainer,
    onPrimary = PrimaryBlue,
    secondary = SecondaryOrangeContainer,
    onSecondary = TextDark,
    tertiary = SuccessGreenContainer,
    onTertiary = TextDark,
    background = TextDark,
    onBackground = BackgroundLight,
    surface = TextDark,
    onSurface = BackgroundLight,
    error = ErrorRed,
    errorContainer = ErrorContainer
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = PrimaryBlueContainer,
    onPrimaryContainer = PrimaryBlue,
    secondary = SecondaryOrange,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = SecondaryOrangeContainer,
    onSecondaryContainer = TextDark,
    tertiary = SuccessGreen,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = SuccessGreenContainer,
    onTertiaryContainer = TextDark,
    background = BackgroundLight,
    surface = CardBackground,
    onBackground = TextDark,
    onSurface = TextDark,
    surfaceVariant = PrimaryBlueContainer,
    onSurfaceVariant = TextGray,
    error = ErrorRed,
    errorContainer = ErrorContainer,
    onErrorContainer = TextDark,
    outline = OutlineDark,
    outlineVariant = OutlineLight
)

@Composable
fun InSpectraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

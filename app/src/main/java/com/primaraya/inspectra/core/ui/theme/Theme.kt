package com.primaraya.inspectra.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,
    primaryContainer = BlueContainer,
    onPrimaryContainer = OnBlueContainer,
    secondary = AmberAccent,
    onSecondary = Color.Black,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = OnAmberContainer,
    tertiary = EmeraldSuccess,
    onTertiary = Color.White,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = OnEmeraldContainer,
    background = SlateBg,
    onBackground = SlateWhite, // Higher contrast
    surface = SlateBase,
    onSurface = SlateWhite, // Higher contrast
    surfaceVariant = SlateCard,
    onSurfaceVariant = SlateBright, // Higher contrast
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = SlateBg,
    surfaceContainer = SlateBase,
    surfaceContainerHigh = SlateCard,
    surfaceContainerHighest = SlateElevated,
    error = StatusNg,
    onError = Color.White,
    errorContainer = StatusNgContainer,
    onErrorContainer = OnStatusNg,
    outline = SlateOutline,
    outlineVariant = SlateElevated
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimaryDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = AmberDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = EmeraldSuccess,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightTextSecondary,
    error = StatusNg,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    outline = Color(0xFF94A3B8),
    outlineVariant = LightOutline
)

/**
 * Tema utama InSpectra.
 * Default ke Dark mode (Industrial Dark) untuk lingkungan pabrik.
 * Light mode tersedia untuk preferensi pengguna.
 */
@Composable
fun InSpectraTheme(
    darkTheme: Boolean = true, // Force dark untuk factory environment
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

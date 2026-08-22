package com.example.vild.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Dream color scheme — deep indigo nights, violet moonlight and aurora accents.
 * Every screen floats over [DreamBackground], so surfaces stay dark and translucent.
 */
private val DreamColorScheme = darkColorScheme(
    // Primary — violet moonlight (filled buttons)
    primary = Violet,
    onPrimary = Void,
    primaryContainer = Indigo,
    onPrimaryContainer = MoonLavender,

    // Secondary — aurora teal (confirmations, night mode)
    secondary = AuroraTeal,
    onSecondary = Void,
    secondaryContainer = Color(0xFF1E4A44),
    onSecondaryContainer = AuroraTeal,

    // Tertiary — dream pink
    tertiary = DreamPink,
    onTertiary = Void,
    tertiaryContainer = Color(0xFF4A2438),
    onTertiaryContainer = DreamPink,

    // Error — a soft ember rather than harsh red
    error = Color(0xFFF0A6A6),
    onError = Void,
    errorContainer = Color(0xFF4A2424),
    onErrorContainer = Color(0xFFF0A6A6),

    // Background & Surface
    background = Void,
    onBackground = MoonLavender,
    surface = DeepIndigo,
    onSurface = MoonLavender,
    surfaceVariant = Indigo,
    onSurfaceVariant = Mist,

    // Outline
    outline = Color(0xFF5A4A8F),
    outlineVariant = Color(0xFF33265C),

    // Inverse
    inverseSurface = MoonLavender,
    inverseOnSurface = Void,
    inversePrimary = Indigo,

    // Scrim
    scrim = Black,
)

@Composable
fun VILDTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DreamColorScheme,
        typography = Typography,
        content = content,
    )
}

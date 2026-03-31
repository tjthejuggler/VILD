package com.example.vild.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Fully greyscale color scheme – every UI element is black/white/grey.
 * The background image is the only element that keeps its original colours
 * (handled at the composable level, not here).
 */
private val GreyscaleColorScheme = darkColorScheme(
    // Primary – used by filled Buttons (container = primary, text = onPrimary)
    primary = Grey40,
    onPrimary = White,
    primaryContainer = Grey30,
    onPrimaryContainer = White,

    // Secondary – used by Night-mode button
    secondary = Grey30,
    onSecondary = White,
    secondaryContainer = Grey30,
    onSecondaryContainer = White,

    // Tertiary
    tertiary = Grey30,
    onTertiary = White,
    tertiaryContainer = Grey30,
    onTertiaryContainer = White,

    // Error (greyscale – no red)
    error = Grey40,
    onError = White,
    errorContainer = Grey30,
    onErrorContainer = White,

    // Background & Surface
    background = Grey10,
    onBackground = White,
    surface = Grey15,
    onSurface = White,
    surfaceVariant = Grey20,
    onSurfaceVariant = White,

    // Outline
    outline = Grey60,
    outlineVariant = Grey40,

    // Inverse
    inverseSurface = White,
    inverseOnSurface = Black,
    inversePrimary = Grey40,

    // Scrim
    scrim = Black,
)

@Composable
fun VILDTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = GreyscaleColorScheme,
        typography = Typography,
        content = content,
    )
}

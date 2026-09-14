package com.you.reelblocker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ReelBlockerColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = DarkBg,
    primaryContainer = DarkCard,
    onPrimaryContainer = CyanPrimary,
    secondary = VioletAccent,
    onSecondary = DarkBg,
    secondaryContainer = DarkCardHover,
    onSecondaryContainer = VioletAccent,
    tertiary = EmeraldActive,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder
)

@Composable
fun FirstAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ReelBlockerColorScheme,
        typography = Typography,
        content = content
    )
}

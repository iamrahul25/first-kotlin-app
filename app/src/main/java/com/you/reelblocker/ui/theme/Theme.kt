package com.you.reelblocker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val FocusBlockColorScheme = lightColorScheme(
    primary = Forest,
    onPrimary = CardWhite,
    primaryContainer = Mint,
    onPrimaryContainer = ForestDeep,
    secondary = Purple,
    onSecondary = CardWhite,
    background = Cream,
    onBackground = Ink,
    surface = CardWhite,
    onSurface = Ink,
    surfaceVariant = StepperBg,
    onSurfaceVariant = Slate,
    outline = CardBorder,
    error = Red
)

@Composable
fun FocusBlockTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FocusBlockColorScheme,
        typography = Typography,
        content = content
    )
}

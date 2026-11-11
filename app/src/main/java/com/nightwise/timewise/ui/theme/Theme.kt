package com.nightwise.timewise.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val TimeWiseColorScheme = lightColorScheme(
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
fun TimeWiseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TimeWiseColorScheme,
        typography = Typography,
        content = content
    )
}

package com.dynamicisland.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Strictly Dark theme only as specified in requirements
private val DarkColorPalette = darkColorScheme(
    primary = NothingRed,
    onPrimary = Color.White,
    primaryContainer = NothingRedDim,
    onPrimaryContainer = Color.White,
    secondary = NothingWhite,
    onSecondary = PureBlack,
    background = PureBlack,
    onBackground = NothingWhite,
    surface = DarkSurface,
    onSurface = NothingWhite,
    surfaceVariant = DarkSurfaceCard,
    onSurfaceVariant = NothingGrayText,
    outline = DarkBorder
)

@Composable
fun DynamicIslandTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorPalette,
        typography = DynamicIslandTypography,
        content = content
    )
}

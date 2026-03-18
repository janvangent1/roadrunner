package com.roadrunner.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary              = OrangePrimary,
    onPrimary            = BackgroundDark,
    primaryContainer     = OrangeDark,
    onPrimaryContainer   = OrangeLight,
    secondary            = OrangeLight,
    onSecondary          = BackgroundDark,
    background           = BackgroundDark,
    onBackground         = OnBackgroundDark,
    surface              = SurfaceDark,
    onSurface            = OnSurfaceDark,
    surfaceVariant       = SurfaceVariant,
    onSurfaceVariant     = SubtleText,
    outline              = OutlineColor,
    error                = ErrorColor,
    onError              = BackgroundDark,
)

@Composable
fun RoadrunnerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}

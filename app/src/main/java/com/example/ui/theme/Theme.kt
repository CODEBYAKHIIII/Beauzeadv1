package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val WavesColorScheme = lightColorScheme(
    primary = EmeraldInk,
    onPrimary = OnPrimary,
    primaryContainer = EmeraldLight,
    onPrimaryContainer = OnPrimary,
    secondary = AccentCyan,
    onSecondary = TextPrimary,
    background = BackgroundColor,
    onBackground = TextPrimary,
    surface = SurfaceColor,
    onSurface = TextPrimary,
    error = DangerRed,
    onError = OnPrimary
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WavesColorScheme,
        typography = Typography,
        content = content
    )
}

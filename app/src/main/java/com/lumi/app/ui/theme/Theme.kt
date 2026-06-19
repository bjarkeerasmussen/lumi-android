package com.lumi.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Warm, calm "skincare" palette — soft clay/rose on a warm off-white.
private val Clay = Color(0xFFB5715A)
private val ClayDark = Color(0xFF8F5946)
private val Rose = Color(0xFFE8B5A0)
private val Sand = Color(0xFFF7F3F0)
private val Ink = Color(0xFF2E2622)

private val LightColors = lightColorScheme(
    primary = Clay,
    onPrimary = Color.White,
    primaryContainer = Rose,
    onPrimaryContainer = Color(0xFF4A2C20),
    secondary = Color(0xFF7A9A86),
    background = Sand,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEDE3DC),
    onSurfaceVariant = Color(0xFF6B5E56)
)

private val DarkColors = darkColorScheme(
    primary = Rose,
    onPrimary = Color(0xFF3A241C),
    primaryContainer = ClayDark,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF9BBBA6),
    background = Color(0xFF1C1714),
    onBackground = Color(0xFFF0E7E1),
    surface = Color(0xFF26201C),
    onSurface = Color(0xFFF0E7E1),
    surfaceVariant = Color(0xFF3A322D),
    onSurfaceVariant = Color(0xFFCDBFB6)
)

@Composable
fun LumiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = LumiTypography,
        content = content
    )
}

package com.lumi.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Lumi uses the Nightfall "night" palette: deep indigo surfaces, periwinkle
// accent, warm-sand secondary, soft lavender ink. Always dark, for a calm,
// consistent look that matches the Nightfall app.
private val Bg = Color(0xFF1C1736)
private val Card = Color(0xFF272042)
private val TabSurface = Color(0xFF231D42)
private val Ink = Color(0xFFF3EEFC)
private val Muted = Color(0xFFA79FC4)
private val Accent = Color(0xFFB6A6F2)      // periwinkle
private val Accent2 = Color(0xFFE8C79F)     // warm sand
private val OnAccent = Color(0xFF1C1736)
private val HeroB = Color(0xFF3A2B63)

private val LumiColors = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    primaryContainer = HeroB,
    onPrimaryContainer = Ink,
    secondary = Accent2,
    onSecondary = OnAccent,
    secondaryContainer = Color(0xFF3A3258),
    onSecondaryContainer = Ink,
    background = Bg,
    onBackground = Ink,
    surface = Card,
    onSurface = Ink,
    surfaceVariant = TabSurface,
    onSurfaceVariant = Muted,
    outline = Color(0xFF4A4368),
    // Keep a legible error treatment for the Skin Check red-flag card.
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

@Composable
fun LumiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LumiColors,
        typography = LumiTypography,
        content = content
    )
}

package com.example.enkan_haban.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light sky blue is the main brand color (bars, highlights); white is the alternate.
private val SkyBlueLight = Color(0xFFA8DCF7)
private val SkyBlueTint = Color(0xFFE3F4FD)
// Deeper sky blue for buttons and text on white, where the light tone would be unreadable.
private val SkyBlue = Color(0xFF2B8ACB)
private val Navy = Color(0xFF0B3954)
val DealRed = Color(0xFFD93025)

private val LightColors = lightColorScheme(
    primary = SkyBlue,
    onPrimary = Color.White,
    primaryContainer = SkyBlueLight,
    onPrimaryContainer = Navy,
    secondary = Color.White,
    onSecondary = Navy,
    secondaryContainer = SkyBlueTint,
    onSecondaryContainer = Navy,
    background = Color.White,
    surface = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = SkyBlueLight,
    onPrimary = Navy,
    primaryContainer = Color(0xFF1D4F6E),
    onPrimaryContainer = SkyBlueTint,
    secondary = Color.White,
    onSecondary = Navy,
    secondaryContainer = Color(0xFF2A4658),
    onSecondaryContainer = SkyBlueTint,
)

@Composable
fun EnkanHabanTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

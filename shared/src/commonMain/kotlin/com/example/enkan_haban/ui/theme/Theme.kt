package com.example.enkan_haban.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Brand palette ────────────────────────────────────────────────────────────
// Sky blue stays the brand color; navy gives depth, gold is the accent for
// ratings and highlights, red is reserved for deals.
val SkyBlueLight = Color(0xFFA8DCF7)
val SkyBlueTint = Color(0xFFE3F4FD)
val SkyBlueBright = Color(0xFF3FA9E8)
val SkyBlue = Color(0xFF2B8ACB)
val Navy = Color(0xFF0B3954)
val NavyDeep = Color(0xFF072536)
val Gold = Color(0xFFF5B83D)
val DealRed = Color(0xFFE5484D)
val PageBackground = Color(0xFFF4F8FB)

/** Header and top-bar gradient: bright sky → brand blue → navy. */
val HeaderGradient = Brush.linearGradient(listOf(SkyBlueBright, SkyBlue, Navy))

/** Soft two-tone background for each category's product image placeholder and tile. */
fun categoryColors(categoryId: String?): List<Color> = when (categoryId) {
    "phones" -> listOf(Color(0xFFE0EEFF), Color(0xFFB9D6FF))
    "electronics" -> listOf(Color(0xFFE6E4FF), Color(0xFFC6C0FF))
    "fashion" -> listOf(Color(0xFFFFE4EE), Color(0xFFFFC2D6))
    "home" -> listOf(Color(0xFFFFF0DB), Color(0xFFFFD9A8))
    "vehicles" -> listOf(Color(0xFFE2F5F1), Color(0xFFB4E6DA))
    "property" -> listOf(Color(0xFFE9F6DF), Color(0xFFC8E8AE))
    "beauty" -> listOf(Color(0xFFFCE6FA), Color(0xFFF2BFEC))
    "groceries" -> listOf(Color(0xFFFFF6D6), Color(0xFFFFE594))
    else -> listOf(SkyBlueTint, SkyBlueLight)
}

private val LightColors = lightColorScheme(
    primary = SkyBlue,
    onPrimary = Color.White,
    primaryContainer = SkyBlueLight,
    onPrimaryContainer = Navy,
    secondary = Navy,
    onSecondary = Color.White,
    secondaryContainer = SkyBlueTint,
    onSecondaryContainer = Navy,
    tertiary = Gold,
    onTertiary = NavyDeep,
    background = PageBackground,
    onBackground = NavyDeep,
    surface = Color.White,
    onSurface = NavyDeep,
    surfaceVariant = Color(0xFFEAF1F7),
    onSurfaceVariant = Color(0xFF5B6B78),
    outlineVariant = Color(0xFFDCE6EE),
)

private val DarkColors = darkColorScheme(
    primary = SkyBlueLight,
    onPrimary = Navy,
    primaryContainer = Color(0xFF1D4F6E),
    onPrimaryContainer = SkyBlueTint,
    secondary = SkyBlueLight,
    onSecondary = Navy,
    secondaryContainer = Color(0xFF2A4658),
    onSecondaryContainer = SkyBlueTint,
    tertiary = Gold,
    onTertiary = NavyDeep,
    background = Color(0xFF0A1620),
    surface = Color(0xFF12222E),
    surfaceVariant = Color(0xFF1C2F3D),
)

// Bolder headings and tighter tracking give the modern, confident look.
// The system fonts (Noto Sans Ethiopic on Android, Kefa on iOS) render Tigrinya.
private val Base = Typography()
private val AppTypography = Typography(
    displaySmall = Base.displaySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.Bold),
    titleSmall = Base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge,
    bodyMedium = Base.bodyMedium,
    bodySmall = Base.bodySmall,
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = Base.labelMedium.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = Base.labelSmall.copy(fontWeight = FontWeight.Medium),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun EnkanHabanTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

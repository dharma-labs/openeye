package com.example.openmind.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Semantic tokens. Every screen reads these instead of hard-coded colours. */
data class EyeColors(
    val ink: Color,
    val surfaceOne: Color,
    val surfaceTwo: Color,
    val hairline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentDim: Color,
    val accentSoft: Color,
    val danger: Color,
    val cyan: Color,
)

internal val DarkEyeColors = EyeColors(
    ink = Color(0xFF08090B),
    surfaceOne = Color(0xFF14171B),
    surfaceTwo = Color(0xFF1B1F24),
    hairline = Color(0xFF262B31),
    textPrimary = Color(0xFFF4F6F8),
    textSecondary = Color(0xFF9299A2),
    textTertiary = Color(0xFF646B74),
    accent = Color(0xFFC9F24D),
    accentDim = Color(0xFF8EA833),
    accentSoft = Color(0xFF1D2412),
    danger = Color(0xFFFF6B6B),
    cyan = Color(0xFF6EE7D7),
)

internal val LightEyeColors = EyeColors(
    ink = Color(0xFFF6F7F2),
    surfaceOne = Color(0xFFFFFFFF),
    surfaceTwo = Color(0xFFEDF0E6),
    hairline = Color(0xFFDFE3D7),
    textPrimary = Color(0xFF11140B),
    textSecondary = Color(0xFF5D6457),
    textTertiary = Color(0xFF8B9284),
    accent = Color(0xFF5E7A12),
    accentDim = Color(0xFF6F8A24),
    accentSoft = Color(0xFFEAF2D4),
    danger = Color(0xFFC2453F),
    cyan = Color(0xFF2B7F76),
)

val LocalEyeColors = staticCompositionLocalOf { DarkEyeColors }

val Ink: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalEyeColors.current.ink

val SurfaceOne: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalEyeColors.current.surfaceOne

val SurfaceTwo: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalEyeColors.current.surfaceTwo

val Hairline: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalEyeColors.current.hairline

val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalEyeColors.current.textPrimary

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalEyeColors.current.textSecondary

val TextTertiary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalEyeColors.current.textTertiary

val Accent: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalEyeColors.current.accent

val AccentDim: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalEyeColors.current.accentDim

val AccentSoft: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalEyeColors.current.accentSoft

val Danger: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalEyeColors.current.danger

val Cyan: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalEyeColors.current.cyan

private val EyeTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 54.sp,
        lineHeight = 58.sp,
        letterSpacing = (-1.8).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 38.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1.0).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 1.1.sp,
    ),
)

private fun schemeFor(palette: EyeColors, dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = palette.accent,
        onPrimary = palette.ink,
        primaryContainer = palette.accentSoft,
        onPrimaryContainer = palette.accent,
        secondary = palette.cyan,
        onSecondary = palette.ink,
        background = palette.ink,
        onBackground = palette.textPrimary,
        surface = palette.surfaceOne,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.surfaceTwo,
        onSurfaceVariant = palette.textSecondary,
        outline = palette.hairline,
        error = palette.danger,
    )
} else {
    lightColorScheme(
        primary = palette.accent,
        onPrimary = Color.White,
        primaryContainer = palette.accentSoft,
        onPrimaryContainer = palette.accent,
        secondary = palette.cyan,
        onSecondary = Color.White,
        background = palette.ink,
        onBackground = palette.textPrimary,
        surface = palette.surfaceOne,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.surfaceTwo,
        onSurfaceVariant = palette.textSecondary,
        outline = palette.hairline,
        error = palette.danger,
    )
}

@Composable
fun OpenEyeTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkEyeColors else LightEyeColors
    androidx.compose.runtime.CompositionLocalProvider(LocalEyeColors provides palette) {
        MaterialTheme(
            colorScheme = schemeFor(palette, darkTheme),
            typography = EyeTypography,
            content = content,
        )
    }
}
/** Kept only so the unreachable template demo screen still compiles. */
@Composable
fun OpenMindTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    OpenEyeTheme(darkTheme = darkTheme, content = content)
}
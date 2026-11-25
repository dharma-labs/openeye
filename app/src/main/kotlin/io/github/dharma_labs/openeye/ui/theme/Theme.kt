package io.github.dharma_labs.openeye.ui.theme

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

/**
 * Semantic tokens. Every screen reads these instead of hard-coded colours, and there are only two
 * families in here: the surfaces a card can sit on, and the three text weights. Colour itself is
 * rationed - one accent, and the rest only ever means a state - because on a wellbeing app, colour
 * that does not mean something is just another thing competing for the eye.
 */
data class EyeColors(
    val ink: Color,
    val surfaceOne: Color,
    val surfaceTwo: Color,
    val surfaceThree: Color,
    val hairline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val onAccent: Color,
    val accentDim: Color,
    val accentSoft: Color,
    val danger: Color,
    val warn: Color,
    val cyan: Color,
    val shadow: Color,
)

internal val DarkEyeColors = EyeColors(
    ink = Color(0xFF07080A),
    surfaceOne = Color(0xFF101317),
    surfaceTwo = Color(0xFF161A1F),
    surfaceThree = Color(0xFF1D232A),
    hairline = Color(0xFF242B33),
    textPrimary = Color(0xFFF2F5F7),
    textSecondary = Color(0xFF98A1AC),
    textTertiary = Color(0xFF616A75),
    accent = Color(0xFFC9F24D),
    onAccent = Color(0xFF0A0D03),
    accentDim = Color(0xFF8EA833),
    accentSoft = Color(0xFF181E0E),
    danger = Color(0xFFFF6B6B),
    warn = Color(0xFFF2B14D),
    cyan = Color(0xFF6EE7D7),
    shadow = Color(0xFF000000),
)

internal val LightEyeColors = EyeColors(
    ink = Color(0xFFF6F7F2),
    surfaceOne = Color(0xFFFFFFFF),
    surfaceTwo = Color(0xFFF1F3EA),
    surfaceThree = Color(0xFFE7EADC),
    hairline = Color(0xFFDDE1D2),
    textPrimary = Color(0xFF101308),
    textSecondary = Color(0xFF575E4F),
    textTertiary = Color(0xFF848C7B),
    accent = Color(0xFF506D0F),
    onAccent = Color(0xFFFFFFFF),
    accentDim = Color(0xFF6F8A24),
    accentSoft = Color(0xFFEAF2D4),
    danger = Color(0xFFB23B36),
    warn = Color(0xFF8A6212),
    cyan = Color(0xFF256F68),
    shadow = Color(0xFF3A4227),
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

/** The third step of the ladder: a card that lives inside another card. */
val SurfaceThree: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalEyeColors.current.surfaceThree

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

/** What goes on top of the accent. Not always ink and not always white - the pair is chosen so the
 *  label passes contrast on both palettes. */
val OnAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalEyeColors.current.onAccent

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

/** Past the number, not over it. Amber means "you are at the edge", red only means "it is blocked". */
val Warn: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalEyeColors.current.warn

val Cyan: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalEyeColors.current.cyan

val Shadow: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalEyeColors.current.shadow

/**
 * Two rules in this scale.
 *
 * Numbers use tabular figures. A counter that changes width every second makes the whole layout
 * twitch, and a twitching layout is read as an unstable app - the opposite of what a wellbeing tool
 * is selling.
 *
 * Every size step is a real step. The old scale had 15, 15.5, 17 and 21 sp sitting next to each
 * other, which is not a hierarchy, it is noise; you cannot see the difference and the page stops
 * telling you where to look first.
 */
private val Tabular = TextStyle(fontFeatureSettings = "tnum")

private val EyeTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 60.sp,
        lineHeight = 60.sp,
        letterSpacing = (-2.6).sp,
        fontFeatureSettings = "tnum",
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 42.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1.8).sp,
        fontFeatureSettings = "tnum",
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 34.sp,
        letterSpacing = (-1.2).sp,
        fontFeatureSettings = "tnum",
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 21.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum",
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp,
        fontFeatureSettings = "tnum",
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = "tnum",
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 19.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.5.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.0.sp,
    ),
)

private fun schemeFor(palette: EyeColors, dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = palette.accent,
        onPrimary = palette.onAccent,
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
        onPrimary = palette.onAccent,
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

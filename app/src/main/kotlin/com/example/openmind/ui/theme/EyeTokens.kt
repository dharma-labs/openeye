package com.example.openmind.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The four things that make a UI read as designed rather than assembled: one spacing rhythm, one
 * radius family, one elevation ladder and one motion curve. Screens are not allowed to invent their
 * own numbers - when every gap on every page comes from the same ladder, the app looks like one app.
 */
object EyeSpace {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 44.dp

    /** Side gutter. One value, used by every screen, so text edges line up across tabs. */
    val screen = 20.dp

    /** Vertical gap between two cards. Slightly larger than the padding inside a card, which is
     *  what makes separate things read as separate things (Gestalt proximity). */
    val between = 14.dp
}

object EyeShape {
    val chip = 100.dp
    val field = 14.dp
    val control = 18.dp
    val card = 24.dp
    val cardLarge = 30.dp
    val sheet = 36.dp
    val hero = 34.dp
}

/**
 * Elevation is a ladder of four. A card that is inside another card is one step lower than the one
 * holding it, and the step is the only depth cue a dark UI can actually show, so the numbers are
 * kept far enough apart to be visible on a black screen at arm's length.
 */
object EyeElevation {
    /** Screen background - no shadow. */
    val flat = 0.dp

    /** A card sitting on the background. */
    val raised = 10.dp

    /** A card inside a card, or a bar that floats over content. */
    val floating = 18.dp

    /** Something the person has to answer before going on. */
    val modal = 30.dp

    /** Shadow opacity. On a dark surface a black shadow is invisible; a tinted one is not. */
    val ambientAlpha = 0.5f
    val spotAlpha = 0.62f
}

object EyeMotion {
    /** Material 3's standard curve: leaves fast, arrives soft. Everything that moves on screen
     *  uses this or one of the two below, never a linear ramp, which reads as mechanical. */
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** For things entering the screen - decelerating only. */
    val Decelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** For a press: short and slightly overshooting so a button feels like it has weight. */
    val Press = CubicBezierEasing(0.3f, 0f, 0.2f, 1f)

    const val instant = 90
    const val quick = 150
    const val normal = 240
    const val slow = 420
    const val hero = 620

    fun <T> enter() = tween<T>(normal, easing = Decelerate)
}

/** The smallest tap target the app will ship. Anything drawn smaller than this gets padded out. */
val MinTouch = 44.dp

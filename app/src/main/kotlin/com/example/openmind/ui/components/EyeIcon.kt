package com.example.openmind.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.openmind.ui.theme.TextPrimary
import kotlin.math.cos
import kotlin.math.sin

/**
 * The app's icon set, drawn rather than imported.
 *
 * Text arrows used to do this job. A "→" is not an icon: it inherits the text baseline, it resizes
 * with the font, and on some devices it renders as emoji. A wellbeing app that cannot draw its own
 * back arrow is asking a lot for someone's attention.
 *
 * Every glyph sits on a 24dp grid at one stroke weight with round caps, so a nav bar and a list row
 * agree. Outlined means "this is a thing you could do"; filled means "this is the state you are in".
 */
@Composable
fun EyeIcon(
    type: String,
    modifier: Modifier = Modifier,
    tint: Color = TextPrimary,
    size: Dp = 22.dp,
    filled: Boolean = false,
) {
    Canvas(modifier = modifier.size(size)) {
        val s = 1.8.dp.toPx()
        val w = this.size.width
        val h = this.size.height

        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(tint, Offset(x1, y1), Offset(x2, y2), s, StrokeCap.Round)

        fun dot(fx: Float, fy: Float, r: Float = s * 0.9f) =
            drawCircle(tint, r, Offset(w * fx, h * fy))

        fun arc(cx: Float, cy: Float, r: Float, start: Float, sweep: Float) {
            drawArc(
                color = tint,
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2, r * 2),
                style = Stroke(s, cap = StrokeCap.Round),
            )
        }

        fun ring(fx: Float, fy: Float, r: Float) =
            drawCircle(tint, r, Offset(w * fx, h * fy), style = Stroke(s))

        fun poly(vararg pts: Pair<Float, Float>) {
            val path = Path()
            pts.forEachIndexed { i, (fx, fy) ->
                if (i == 0) path.moveTo(w * fx, h * fy) else path.lineTo(w * fx, h * fy)
            }
            drawPath(path, tint, style = Stroke(s, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        fun shape(path: Path) {
            if (filled) drawPath(path, tint) else drawPath(path, tint, style = Stroke(s, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        when (type) {
            "back" -> {
                poly(0.42f to 0.24f, 0.18f to 0.5f, 0.42f to 0.76f)
                line(w * 0.18f, h * 0.5f, w * 0.84f, h * 0.5f)
            }
            "forward" -> {
                poly(0.58f to 0.24f, 0.82f to 0.5f, 0.58f to 0.76f)
                line(w * 0.16f, h * 0.5f, w * 0.82f, h * 0.5f)
            }
            "chevron" -> poly(0.3f to 0.38f, 0.5f to 0.6f, 0.7f to 0.38f)
            "chevron_up" -> poly(0.3f to 0.62f, 0.5f to 0.4f, 0.7f to 0.62f)
            "close" -> {
                line(w * 0.28f, h * 0.28f, w * 0.72f, h * 0.72f)
                line(w * 0.72f, h * 0.28f, w * 0.28f, h * 0.72f)
            }
            "plus" -> {
                line(w * 0.5f, h * 0.24f, w * 0.5f, h * 0.76f)
                line(w * 0.24f, h * 0.5f, w * 0.76f, h * 0.5f)
            }
            "minus" -> line(w * 0.26f, h * 0.5f, w * 0.74f, h * 0.5f)
            "check" -> poly(0.2f to 0.52f, 0.42f to 0.72f, 0.8f to 0.3f)
            "settings" -> {
                // Two rails and two knobs. A gear turns into a blob at 16dp; sliders stay sliders.
                line(w * 0.2f, h * 0.36f, w * 0.8f, h * 0.36f)
                line(w * 0.2f, h * 0.64f, w * 0.8f, h * 0.64f)
                drawCircle(tint, radius = s * 1.5f, center = Offset(w * 0.64f, h * 0.36f))
                drawCircle(tint, radius = s * 1.5f, center = Offset(w * 0.36f, h * 0.64f))
            }
            "shield" -> shape(Path().apply {
                moveTo(w * 0.5f, h * 0.16f)
                lineTo(w * 0.8f, h * 0.28f)
                lineTo(w * 0.8f, h * 0.52f)
                cubicTo(w * 0.8f, h * 0.74f, w * 0.64f, h * 0.83f, w * 0.5f, h * 0.87f)
                cubicTo(w * 0.36f, h * 0.83f, w * 0.2f, h * 0.74f, w * 0.2f, h * 0.52f)
                lineTo(w * 0.2f, h * 0.28f)
                close()
            })
            "clock" -> {
                ring(0.5f, 0.5f, w * 0.32f)
                line(w * 0.5f, h * 0.5f, w * 0.5f, h * 0.3f)
                line(w * 0.5f, h * 0.5f, w * 0.66f, h * 0.56f)
            }
            "chart" -> {
                fun bar(fx: Float, frac: Float) = drawLine(
                    color = tint,
                    start = Offset(w * fx, h * (0.8f - frac * 0.58f)),
                    end = Offset(w * fx, h * 0.8f),
                    strokeWidth = 2.6.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                bar(0.24f, 0.42f)
                bar(0.5f, 0.78f)
                bar(0.76f, 0.28f)
            }
            "bell" -> {
                shape(Path().apply {
                    moveTo(w * 0.26f, h * 0.66f)
                    cubicTo(w * 0.33f, h * 0.58f, w * 0.33f, h * 0.5f, w * 0.33f, h * 0.42f)
                    cubicTo(w * 0.33f, h * 0.25f, w * 0.4f, h * 0.2f, w * 0.5f, h * 0.2f)
                    cubicTo(w * 0.6f, h * 0.2f, w * 0.67f, h * 0.25f, w * 0.67f, h * 0.42f)
                    cubicTo(w * 0.67f, h * 0.5f, w * 0.67f, h * 0.58f, w * 0.74f, h * 0.66f)
                    close()
                })
                dot(0.5f, 0.75f, w * 0.06f)
            }
            "moon" -> {
                arc(w * 0.5f, h * 0.5f, w * 0.34f, 10f, 240f)
                arc(w * 0.647f, h * 0.325f, w * 0.30f, 51f, 158f)
            }
            "sun" -> {
                ring(0.5f, 0.5f, w * 0.17f)
                repeat(8) { i ->
                    val a = Math.toRadians((i * 45).toDouble())
                    val dx = (cos(a) * w * 0.33f).toFloat()
                    val dy = (sin(a) * w * 0.33f).toFloat()
                    line(w * 0.5f + dx * 0.7f, h * 0.5f + dy * 0.7f, w * 0.5f + dx, h * 0.5f + dy)
                }
            }
            "lock" -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.26f, h * 0.46f),
                    size = Size(w * 0.48f, h * 0.34f),
                    cornerRadius = CornerRadius(s * 2f, s * 2f),
                    style = Stroke(s),
                )
                shape(Path().apply {
                    moveTo(w * 0.36f, h * 0.46f)
                    lineTo(w * 0.36f, h * 0.34f)
                    cubicTo(w * 0.36f, h * 0.18f, w * 0.64f, h * 0.18f, w * 0.64f, h * 0.34f)
                    lineTo(w * 0.64f, h * 0.46f)
                })
            }
            "share" -> {
                dot(0.24f, 0.5f, w * 0.085f)
                dot(0.74f, 0.26f, w * 0.085f)
                dot(0.74f, 0.74f, w * 0.085f)
                line(w * 0.32f, h * 0.45f, w * 0.66f, h * 0.3f)
                line(w * 0.32f, h * 0.55f, w * 0.66f, h * 0.7f)
            }
            "refresh" -> {
                drawArc(
                    color = tint,
                    startAngle = 45f,
                    sweepAngle = 275f,
                    useCenter = false,
                    topLeft = Offset(w * 0.2f, h * 0.2f),
                    size = Size(w * 0.6f, h * 0.6f),
                    style = Stroke(s, cap = StrokeCap.Round),
                )
                poly(0.78f to 0.2f, 0.8f to 0.38f, 0.62f to 0.36f)
            }
            "play" -> shape(Path().apply {
                moveTo(w * 0.34f, h * 0.26f)
                lineTo(w * 0.76f, h * 0.5f)
                lineTo(w * 0.34f, h * 0.74f)
                close()
            })
            "stop" -> drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.31f, h * 0.31f),
                size = Size(w * 0.38f, h * 0.38f),
                cornerRadius = CornerRadius(s, s),
                style = Stroke(s, join = StrokeJoin.Round),
            )
            "target" -> {
                ring(0.5f, 0.5f, w * 0.32f)
                ring(0.5f, 0.5f, w * 0.16f)
                dot(0.5f, 0.5f, s * 0.8f)
            }
            "calendar" -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.2f, h * 0.26f),
                    size = Size(w * 0.6f, h * 0.52f),
                    cornerRadius = CornerRadius(s * 1.6f, s * 1.6f),
                    style = Stroke(s),
                )
                line(w * 0.2f, h * 0.42f, w * 0.8f, h * 0.42f)
                line(w * 0.36f, h * 0.18f, w * 0.36f, h * 0.32f)
                line(w * 0.64f, h * 0.18f, w * 0.64f, h * 0.32f)
            }
            "spark" -> shape(Path().apply {
                moveTo(w * 0.5f, h * 0.16f)
                cubicTo(w * 0.56f, h * 0.42f, w * 0.58f, h * 0.44f, w * 0.84f, h * 0.5f)
                cubicTo(w * 0.58f, h * 0.56f, w * 0.56f, h * 0.58f, w * 0.5f, h * 0.84f)
                cubicTo(w * 0.44f, h * 0.58f, w * 0.42f, h * 0.56f, w * 0.16f, h * 0.5f)
                cubicTo(w * 0.42f, h * 0.44f, w * 0.44f, h * 0.42f, w * 0.5f, h * 0.16f)
                close()
            })
            "info" -> {
                ring(0.5f, 0.5f, w * 0.32f)
                line(w * 0.5f, h * 0.46f, w * 0.5f, h * 0.68f)
                dot(0.5f, 0.33f, s * 0.75f)
            }
            "alert" -> {
                ring(0.5f, 0.5f, w * 0.32f)
                line(w * 0.5f, h * 0.3f, w * 0.5f, h * 0.54f)
                dot(0.5f, 0.7f, s * 0.75f)
            }
            "heart" -> shape(Path().apply {
                moveTo(w * 0.5f, h * 0.76f)
                cubicTo(w * 0.2f, h * 0.56f, w * 0.2f, h * 0.28f, w * 0.38f, h * 0.26f)
                cubicTo(w * 0.47f, h * 0.25f, w * 0.5f, h * 0.34f, w * 0.5f, h * 0.34f)
                cubicTo(w * 0.5f, h * 0.34f, w * 0.53f, h * 0.25f, w * 0.62f, h * 0.26f)
                cubicTo(w * 0.8f, h * 0.28f, w * 0.8f, h * 0.56f, w * 0.5f, h * 0.76f)
                close()
            })
            "wave" -> shape(Path().apply {
                moveTo(w * 0.12f, h * 0.6f)
                cubicTo(w * 0.3f, h * 0.6f, w * 0.3f, h * 0.34f, w * 0.5f, h * 0.34f)
                cubicTo(w * 0.7f, h * 0.34f, w * 0.7f, h * 0.6f, w * 0.88f, h * 0.6f)
            })
            else -> ring(0.5f, 0.5f, w * 0.3f)
        }
    }
}

/** A glyph on a rounded tile - the leading mark for every list row in the app. */
@Composable
fun IconTile(
    type: String,
    modifier: Modifier = Modifier,
    tint: Color = TextPrimary,
    background: Color = Color.Unspecified,
    size: Dp = 40.dp,
    filled: Boolean = false,
) {
    val tile = if (background == Color.Unspecified) tint.copy(alpha = 0.12f) else background
    Box(
        modifier = modifier
            .size(size)
            .background(tile, RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        EyeIcon(type = type, tint = tint, size = size * 0.5f, filled = filled)
    }
}

package com.example.openmind.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openmind.data.BlockMode
import com.example.openmind.data.Platform
import com.example.openmind.ui.theme.Accent
import com.example.openmind.ui.theme.AccentDim
import com.example.openmind.ui.theme.Hairline
import com.example.openmind.ui.theme.Ink
import com.example.openmind.ui.theme.SurfaceOne
import com.example.openmind.ui.theme.SurfaceTwo
import com.example.openmind.ui.theme.TextPrimary
import com.example.openmind.ui.theme.TextSecondary
import com.example.openmind.ui.theme.TextTertiary

@Composable
fun OMCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(20.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .background(SurfaceOne, RoundedCornerShape(26.dp))
            .border(1.dp, Hairline, RoundedCornerShape(26.dp))
            .padding(padding),
        content = content,
    )
}

/** Deliberately not uppercased: the whole app speaks in lowercase, and shouty labels read as nagging. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = TextTertiary,
        modifier = modifier,
    )
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(SurfaceOne, CircleShape)
                        .border(1.dp, Hairline, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("←", fontSize = 18.sp, color = TextPrimary)
                }
                Spacer(Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                if (subtitle != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            trailing?.invoke()
        }
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Hairline))
}

@Composable
fun Pill(text: String, color: Color = Color.Unspecified, filled: Boolean = false) {
    val pillColor = if (color == Color.Unspecified) Accent else color
    Box(
        modifier = Modifier
            .background(if (filled) pillColor else Color.Transparent, RoundedCornerShape(100.dp))
            .border(1.dp, if (filled) pillColor else Hairline, RoundedCornerShape(100.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (filled) Ink else pillColor,
        )
    }
}

@Composable
fun Monogram(platform: Platform, size: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(
                Brush.linearGradient(
                    listOf(Color(platform.tint).copy(alpha = 0.22f), Color(platform.tint).copy(alpha = 0.06f)),
                ),
                RoundedCornerShape(size * 0.32f),
            )
            .border(1.dp, Color(platform.tint).copy(alpha = 0.28f), RoundedCornerShape(size * 0.32f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = platform.monogram,
            color = Color(platform.tint).copy(alpha = 0.95f),
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.34f).sp,
            letterSpacing = (-0.5).sp,
        )
    }
}

@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Column(
        modifier = modifier
            .background(SurfaceOne, RoundedCornerShape(22.dp))
            .border(1.dp, Hairline, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = if (accent) Accent else TextPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    color: Color = Color.Unspecified,
    track: Color = Color.Unspecified,
    content: @Composable BoxScope.() -> Unit,
) {
    val ringColor = if (color == Color.Unspecified) Accent else color
    val ringTrack = if (track == Color.Unspecified) Hairline else track
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "ring",
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = ringTrack,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (animated > 0.001f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

@Composable
fun ModeSelector(
    selected: BlockMode,
    onSelect: (BlockMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        BlockMode.STRICT to "strict",
        BlockMode.INTENTIONAL to "gentle",
        BlockMode.OFF to "off",
    )
    Row(
        modifier = modifier
            .background(Ink, RoundedCornerShape(100.dp))
            .border(1.dp, Hairline, RoundedCornerShape(100.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (mode, label) ->
            val active = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        when {
                            active && mode == BlockMode.STRICT -> Accent
                            active -> SurfaceTwo
                            else -> Color.Transparent
                        },
                        RoundedCornerShape(100.dp),
                    )
                    .clickable { onSelect(mode) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        active && mode == BlockMode.STRICT -> Ink
                        active -> TextPrimary
                        else -> TextTertiary
                    },
                )
            }
        }
    }
}

@Composable
fun OMSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Ink,
            checkedTrackColor = Accent,
            uncheckedThumbColor = TextSecondary,
            uncheckedTrackColor = SurfaceTwo,
            uncheckedBorderColor = Hairline,
        ),
    )
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Color.Unspecified,
) {
    val buttonColor = if (color == Color.Unspecified) Accent else color
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(if (enabled) buttonColor else SurfaceTwo, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 17.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) Ink else TextTertiary,
        )
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = TextPrimary,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent, RoundedCornerShape(18.dp))
            .border(1.dp, Hairline, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
fun ToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        Spacer(Modifier.width(12.dp))
        if (trailing != null) trailing() else OMSwitch(checked, onCheckedChange)
    }
}

@Composable
fun BarChart(
    values: List<Int>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    barColor: Color = Color.Unspecified,
) {
    val resolvedBar = if (barColor == Color.Unspecified) Accent else barColor
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            if (values.isEmpty()) return@Canvas
            val gap = (size.width / (values.size * 3f)).coerceIn(2.dp.toPx(), 10.dp.toPx())
            val barWidth = (size.width - gap * (values.size - 1)) / values.size
            values.forEachIndexed { index, value ->
                val fraction = value.toFloat() / max.toFloat()
                val height = (size.height * (0.12f + 0.88f * fraction)).coerceAtMost(size.height)
                val x = index * (barWidth + gap)
                drawRoundRect(
                    color = if (index == values.lastIndex) resolvedBar else resolvedBar.copy(alpha = 0.22f),
                    topLeft = Offset(x, size.height - height),
                    size = Size(barWidth, height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun WeekdayChip(letter: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(if (selected) Accent else SurfaceOne, CircleShape)
            .border(1.dp, if (selected) Accent else Hairline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Ink else TextSecondary,
        )
    }
}

@Composable
fun NavGlyph(type: String, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
        val stroke = 1.6.dp.toPx()
        when (type) {
            "today" -> {
                drawCircle(tint, radius = size.minDimension * 0.36f, style = Stroke(stroke))
                drawCircle(tint, radius = size.minDimension * 0.09f)
            }
            "shields" -> {
                val path = Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.1f)
                    lineTo(size.width * 0.86f, size.height * 0.28f)
                    lineTo(size.width * 0.86f, size.height * 0.54f)
                    cubicTo(
                        size.width * 0.86f, size.height * 0.78f,
                        size.width * 0.66f, size.height * 0.9f,
                        size.width * 0.5f, size.height * 0.94f,
                    )
                    cubicTo(
                        size.width * 0.34f, size.height * 0.9f,
                        size.width * 0.14f, size.height * 0.78f,
                        size.width * 0.14f, size.height * 0.54f,
                    )
                    lineTo(size.width * 0.14f, size.height * 0.28f)
                    close()
                }
                drawPath(path, tint, style = Stroke(stroke))
            }
            "focus" -> {
                drawCircle(tint, radius = size.minDimension * 0.36f, style = Stroke(stroke))
                drawCircle(tint, radius = size.minDimension * 0.18f, style = Stroke(stroke))
            }
            "insights" -> {
                val barWidth = size.width * 0.16f
                listOf(0.34f, 0.62f, 0.44f).forEachIndexed { index, h ->
                    val x = size.width * (0.16f + index * 0.28f)
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(x, size.height * (0.9f - h)),
                        size = Size(barWidth, size.height * h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f),
                    )
                }
            }
            else -> {
                repeat(2) { index ->
                    val y = size.height * (0.34f + index * 0.32f)
                    drawLine(tint, Offset(size.width * 0.12f, y), Offset(size.width * 0.88f, y), stroke, StrokeCap.Round)
                    drawCircle(tint, radius = 2.6.dp.toPx(), center = Offset(size.width * (if (index == 0) 0.66f else 0.34f), y))
                }
            }
        }
    }
}
/**
 * The Openeye mark: one solid lens with the way out cut through it as negative space. Same
 * geometry as the launcher icon, so the app and the home screen agree at 20dp.
 */
@Composable
fun EyeMark(
    modifier: Modifier = Modifier.size(24.dp),
    color: Color = Color.Unspecified,
) {
    val markColor = if (color == Color.Unspecified) Accent else color
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val mark = Path().apply {
            moveTo(w * 0.06f, h * 0.50f)
            cubicTo(w * 0.25f, h * 0.19f, w * 0.75f, h * 0.19f, w * 0.94f, h * 0.50f)
            cubicTo(w * 0.75f, h * 0.81f, w * 0.25f, h * 0.81f, w * 0.06f, h * 0.50f)
            close()
            moveTo(w * 0.50f, h * 0.315f)
            lineTo(w * 0.645f, h * 0.50f)
            lineTo(w * 0.575f, h * 0.50f)
            lineTo(w * 0.575f, h * 0.665f)
            lineTo(w * 0.425f, h * 0.665f)
            lineTo(w * 0.425f, h * 0.50f)
            lineTo(w * 0.355f, h * 0.50f)
            close()
            fillType = PathFillType.EvenOdd
        }
        drawPath(mark, color = markColor)
    }
}

@Composable
fun ActionCard(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .background(if (emphasized) Accent else SurfaceOne, RoundedCornerShape(18.dp))
            .border(1.dp, if (emphasized) Accent else Hairline, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (emphasized) Ink else TextPrimary,
        )
    }
}

@Composable
fun MiniMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(3.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
    }
}

@Composable
fun HourlyChart(values: LongArray, modifier: Modifier = Modifier) {
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val activeColor = Accent
    val idleColor = Hairline.copy(alpha = 0.55f)
    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(92.dp)) {
            val gap = 2.dp.toPx()
            val barWidth = ((size.width - gap * 23) / 24f).coerceAtLeast(1f)
            values.forEachIndexed { index, value ->
                val fraction = value.toFloat() / max.toFloat()
                val height = (size.height * (0.06f + 0.94f * fraction)).coerceAtMost(size.height)
                val x = index * (barWidth + gap)
                drawRoundRect(
                    color = if (value > 0L) activeColor else idleColor,
                    topLeft = Offset(x, size.height - height),
                    size = Size(barWidth, height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("12a", "6a", "12p", "6p", "12a").forEach { label ->
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            }
        }
    }
}
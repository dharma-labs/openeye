package io.github.dharma_labs.openeye.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dharma_labs.openeye.data.BlockMode
import io.github.dharma_labs.openeye.data.Platform
import io.github.dharma_labs.openeye.ui.theme.Accent
import io.github.dharma_labs.openeye.ui.theme.AccentDim
import io.github.dharma_labs.openeye.ui.theme.AccentSoft
import io.github.dharma_labs.openeye.ui.theme.EyeElevation
import io.github.dharma_labs.openeye.ui.theme.EyeMotion
import io.github.dharma_labs.openeye.ui.theme.EyeShape
import io.github.dharma_labs.openeye.ui.theme.EyeSpace
import io.github.dharma_labs.openeye.ui.theme.Hairline
import io.github.dharma_labs.openeye.ui.theme.Ink
import io.github.dharma_labs.openeye.ui.theme.OnAccent
import io.github.dharma_labs.openeye.ui.theme.Shadow
import io.github.dharma_labs.openeye.ui.theme.SurfaceOne
import io.github.dharma_labs.openeye.ui.theme.SurfaceThree
import io.github.dharma_labs.openeye.ui.theme.SurfaceTwo
import io.github.dharma_labs.openeye.ui.theme.TextPrimary
import io.github.dharma_labs.openeye.ui.theme.TextSecondary
import io.github.dharma_labs.openeye.ui.theme.TextTertiary

/**
 * A thing you can tap, that tells you it heard you before it moves on.
 *
 * Ripple is switched off on purpose. On a screen this dense a grey splash under a card reads as a
 * glitch; a 2.5% squeeze reads as a button with a hinge in it. The press is also asymmetric - it
 * contracts in 90ms and releases over 240ms, which is the timing that makes an interface feel
 * responsive rather than twitchy.
 */
@Composable
fun Modifier.eyePress(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.975f else 1f,
        animationSpec = tween(
            durationMillis = if (pressed) EyeMotion.instant else EyeMotion.normal,
            easing = EyeMotion.Press,
        ),
        label = "press",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = if (enabled) 1f else 0.55f
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}

/**
 * The card. Depth here is a shadow plus one hairline, not a border all round: the hairline is what
 * keeps a card readable on a bright screen outdoors, where a shadow disappears entirely.
 */
@Composable
fun OMCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(EyeSpace.xl),
    raised: Boolean = true,
    surface: Color = SurfaceOne,
    glow: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(EyeShape.card)
    val depth = if (raised) {
        Modifier.shadow(
            elevation = EyeElevation.raised,
            shape = shape,
            ambientColor = Shadow,
            spotColor = Shadow,
            clip = false,
        )
    } else {
        Modifier
    }
    // Applied after the fill, so the wash sits on top of the surface and under the content. The clip
    // keeps it inside the rounded corners: a radial gradient has no shape of its own and a square of
    // light on a black screen reads as a rendering bug, not as depth.
    val wash = if (glow != null) Modifier.clip(shape).eyeGlow(glow) else Modifier
    Box(
        modifier = modifier
            .then(depth)
            .background(surface, shape)
            .then(wash)
            .border(1.dp, Hairline.copy(alpha = if (raised) 0.7f else 1f), shape)
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

/** A section label with something you can do about it, on the same line. */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SectionLabel(text, Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelMedium,
                color = AccentDim,
                modifier = Modifier.eyePress(onClick = onAction).padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
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
                IconCircle(onClick = onBack, icon = "back")
                Spacer(Modifier.width(EyeSpace.md))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                if (subtitle != null) {
                    Spacer(Modifier.height(EyeSpace.xs))
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            trailing?.invoke()
        }
    }
}

/** The round icon button. 44dp of hit area, 38dp of visible circle - the ring is where your thumb
 *  lands, the padding is where it can wander. */
@Composable
fun IconCircle(
    onClick: () -> Unit,
    icon: String,
    modifier: Modifier = Modifier,
    tint: Color = TextPrimary,
    size: Dp = 40.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(EyeElevation.flat, CircleShape)
            .background(SurfaceOne, CircleShape)
            .border(1.dp, Hairline, CircleShape)
            .eyePress(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        EyeIcon(type = icon, tint = tint, size = size * 0.48f)
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Hairline.copy(alpha = 0.6f)))
}

@Composable
fun Pill(text: String, color: Color = Color.Unspecified, filled: Boolean = false) {
    val pillColor = if (color == Color.Unspecified) Accent else color
    Box(
        modifier = Modifier
            .background(if (filled) pillColor else Color.Transparent, RoundedCornerShape(EyeShape.chip))
            .border(1.dp, if (filled) pillColor else Hairline, RoundedCornerShape(EyeShape.chip))
            .padding(horizontal = EyeSpace.md, vertical = EyeSpace.xs + 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (filled) OnAccent else pillColor,
        )
    }
}

@Composable
fun Monogram(platform: Platform, size: Dp = 44.dp) {
    val tint = Color(platform.tint)
    Box(
        modifier = Modifier
            .size(size)
            .background(
                Brush.linearGradient(listOf(tint.copy(alpha = 0.26f), tint.copy(alpha = 0.08f))),
                RoundedCornerShape(size * 0.3f),
            )
            .border(1.dp, tint.copy(alpha = 0.22f), RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = platform.monogram,
            color = tint.copy(alpha = 0.95f),
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
            .background(SurfaceTwo, RoundedCornerShape(EyeShape.control))
            .padding(horizontal = EyeSpace.lg, vertical = EyeSpace.lg),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = if (accent) Accent else TextPrimary,
        )
        Spacer(Modifier.height(EyeSpace.xs))
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

/**
 * Three or four choices that belong together, with one moving highlight instead of three recoloured
 * boxes. The highlight slides, so the tap and the change are visibly the same event - which is the
 * part that makes a control feel trustworthy rather than sticky.
 */
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
    SegmentedControl(
        options = options.map { it.second },
        selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0),
        onSelect = { onSelect(options[it].first) },
        activeColor = when (selected) {
            BlockMode.STRICT -> Accent
            else -> SurfaceThree
        },
        onActive = selected == BlockMode.STRICT,
        modifier = modifier,
    )
}

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = SurfaceThree,
    onActive: Boolean = false,
) {
    val count = options.size.coerceAtLeast(1)
    val target = selectedIndex.coerceIn(0, count - 1)
    val slide by animateFloatAsState(
        targetValue = target.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.85f),
        label = "segment",
    )
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(Ink, RoundedCornerShape(EyeShape.chip))
            .border(1.dp, Hairline, RoundedCornerShape(EyeShape.chip))
            .padding(3.dp),
    ) {
        val track = maxWidth
        val cell = (track - 6.dp) / count
        Box(
            modifier = Modifier
                .offset(x = cell * slide)
                .width(cell)
                .height(40.dp)
                .background(activeColor, RoundedCornerShape(EyeShape.chip)),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            options.forEachIndexed { index, label ->
                val active = index == target
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .eyePress { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = when {
                            active && onActive -> OnAccent
                            active -> TextPrimary
                            else -> TextTertiary
                        },
                        maxLines = 1,
                    )
                }
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
            checkedThumbColor = OnAccent,
            checkedTrackColor = Accent,
            uncheckedThumbColor = TextSecondary,
            uncheckedTrackColor = SurfaceTwo,
            uncheckedBorderColor = Hairline,
        ),
    )
}

/**
 * The one button on a screen. 56dp tall: the thumb covers what it cannot see, and a button you have
 * to aim at is a button that gets missed in a moment of weakness - which is the only moment this app
 * is ever asked to work.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Color.Unspecified,
    icon: String? = null,
) {
    val buttonColor = if (color == Color.Unspecified) Accent else color
    val shape = RoundedCornerShape(EyeShape.control + 2.dp)
    val depth = if (enabled) {
        Modifier.shadow(
            elevation = EyeElevation.floating,
            shape = shape,
            ambientColor = Shadow,
            spotColor = Shadow,
            clip = false,
        )
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(depth)
            .background(if (enabled) buttonColor else SurfaceTwo, shape)
            .eyePress(enabled = enabled, onClick = onClick)
            .padding(vertical = 17.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(EyeSpace.sm)) {
            if (icon != null) EyeIcon(type = icon, tint = if (enabled) OnAccent else TextTertiary, size = 18.dp)
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                color = if (enabled) OnAccent else TextTertiary,
            )
        }
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = TextPrimary,
    icon: String? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceOne, RoundedCornerShape(EyeShape.control + 2.dp))
            .border(1.dp, Hairline, RoundedCornerShape(EyeShape.control + 2.dp))
            .eyePress(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(EyeSpace.sm)) {
            if (icon != null) EyeIcon(type = icon, tint = color, size = 18.dp)
            Text(text, style = MaterialTheme.typography.titleLarge, color = color)
        }
    }
}

/**
 * Every setting is a row with a leading mark, a name, one line of plain reason, and the control.
 *
 * The icon is not decoration. A row you can recognise by its shape before you can read it is a row
 * you can find again in a second, which is the difference between a settings screen and a list.
 */
@Composable
fun ToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    icon: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = EyeSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            IconTile(
                type = icon,
                tint = if (checked) Accent else TextSecondary,
                size = 36.dp,
            )
            Spacer(Modifier.width(EyeSpace.md))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        Spacer(Modifier.width(EyeSpace.md))
        if (trailing != null) trailing() else OMSwitch(checked, onCheckedChange)
    }
}

/** A row that goes somewhere: name on the left, chevron on the right, the whole thing tappable. */
@Composable
fun NavRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: String? = null,
    value: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .eyePress(onClick = onClick)
            .padding(horizontal = EyeSpace.lg, vertical = EyeSpace.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            IconTile(type = icon, tint = TextSecondary, size = 36.dp)
            Spacer(Modifier.width(EyeSpace.md))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        if (value != null) {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
        }
        Spacer(Modifier.width(EyeSpace.sm))
        EyeIcon(type = "chevron", tint = TextTertiary, size = 16.dp)
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
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                )
            }
        }
        Spacer(Modifier.height(EyeSpace.md))
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
            .size(44.dp)
            .background(if (selected) Accent else SurfaceOne, CircleShape)
            .border(1.dp, if (selected) Accent else Hairline, CircleShape)
            .eyePress(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) OnAccent else TextSecondary,
        )
    }
}

/** Older call sites still ask for a glyph by name; every one of them is now a real vector. */
@Composable
fun NavGlyph(
    type: String,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    filled: Boolean = false,
) {
    val icon = when (type) {
        "today" -> "sun"
        "shields" -> "shield"
        "focus" -> "target"
        "insights" -> "chart"
        "settings" -> "settings"
        else -> "settings"
    }
    EyeIcon(type = icon, tint = tint, modifier = modifier, size = size, filled = filled)
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
    icon: String? = null,
) {
    Box(
        modifier = modifier
            .height(54.dp)
            .background(if (emphasized) Accent else SurfaceOne, RoundedCornerShape(EyeShape.control))
            .border(1.dp, if (emphasized) Accent else Hairline, RoundedCornerShape(EyeShape.control))
            .eyePress(onClick = onClick)
            .padding(horizontal = EyeSpace.lg),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(EyeSpace.sm)) {
            if (icon != null) EyeIcon(type = icon, tint = if (emphasized) OnAccent else TextSecondary, size = 17.dp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (emphasized) OnAccent else TextPrimary,
            )
        }
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
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                )
            }
        }
        Spacer(Modifier.height(EyeSpace.sm))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("12a", "6a", "12p", "6p", "12a").forEach { label ->
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            }
        }
    }
}

/**
 * Nothing measured yet, said once, with the reason and the way forward on the same card. An empty
 * screen that only says "no data" is the screen people quit on.
 */
@Composable
fun EmptyState(
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
    icon: String = "spark",
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    OMCard(
        modifier = modifier.fillMaxWidth(),
        padding = PaddingValues(EyeSpace.xxl),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(AccentSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                EyeIcon(type = icon, tint = AccentDim, size = 24.dp)
            }
            Spacer(Modifier.height(EyeSpace.md))
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(EyeSpace.xs))
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 280.dp),
            )
            if (action != null && onAction != null) {
                Spacer(Modifier.height(EyeSpace.lg))
                Box(
                    modifier = Modifier
                        .background(Accent, RoundedCornerShape(EyeShape.chip))
                        .eyePress(onClick = onAction)
                        .padding(horizontal = EyeSpace.lg, vertical = EyeSpace.md),
                ) {
                    Text(action, style = MaterialTheme.typography.labelLarge, color = OnAccent)
                }
            }
        }
    }
}

/** A disclosure that opens: label, count or hint, and a chevron that turns with the state. */
@Composable
fun DisclosureRow(
    label: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .eyePress(onClick = onClick)
            .padding(vertical = EyeSpace.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary,
            modifier = Modifier.weight(1f),
        )
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            Spacer(Modifier.width(EyeSpace.md))
        }
        val rotation by animateFloatAsState(
            targetValue = if (expanded) 180f else 0f,
            animationSpec = tween(EyeMotion.normal, easing = EyeMotion.Standard),
            label = "disclosure",
        )
        EyeIcon(
            type = "chevron",
            tint = AccentDim,
            size = 18.dp,
            modifier = Modifier.graphicsLayer { rotationZ = rotation },
        )
    }
}

/** Content that appears because you asked, arriving with a short slide rather than a jump cut. */
@Composable
fun Revealed(expanded: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = expanded,
        modifier = modifier,
        enter = fadeIn(tween(EyeMotion.normal, easing = EyeMotion.Decelerate)) +
            scaleIn(initialScale = 0.97f, animationSpec = tween(EyeMotion.normal, easing = EyeMotion.Decelerate)),
        exit = fadeOut(tween(EyeMotion.quick)) + scaleOut(targetScale = 0.98f),
    ) {
        content()
    }
}

/**
 * The number a screen is about. One per screen, always the largest thing on it: the eye goes to the
 * biggest mark on a page before it reads a word, so the biggest mark has to be the true one.
 */
@Composable
fun HeroNumber(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    caption: String? = null,
    emphasis: Boolean = false,
) {
    Column(modifier = modifier) {
        SectionLabel(label)
        Spacer(Modifier.height(EyeSpace.sm))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.displayLarge,
                color = if (emphasis) Accent else TextPrimary,
            )
            if (unit != null) {
                Spacer(Modifier.width(EyeSpace.sm))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary,
                    modifier = Modifier.padding(bottom = EyeSpace.lg),
                )
            }
        }
        if (caption != null) {
            Spacer(Modifier.height(EyeSpace.xs))
            Text(caption, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

/** A soft wash of light behind a card, used once per screen at most. Depth without a second border. */
@Composable
fun Modifier.eyeGlow(color: Color = Accent): Modifier =
    drawWithContent {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.07f), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 0.1f),
                radius = size.width * 0.9f,
            ),
        )
        drawContent()
    }

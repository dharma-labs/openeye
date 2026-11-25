package com.example.openmind.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.openmind.data.BlockEvent
import com.example.openmind.data.PlatformCatalog
import com.example.openmind.data.Settings
import com.example.openmind.data.WatchRecord
import com.example.openmind.domain.AttentionStats
import com.example.openmind.domain.AttentionSummary
import com.example.openmind.domain.Coach
import com.example.openmind.domain.SurfaceAttention
import com.example.openmind.domain.TimeTranslation
import com.example.openmind.domain.Voice
import com.example.openmind.ui.PermissionState
import com.example.openmind.ui.SummaryShare
import com.example.openmind.ui.components.Divider
import com.example.openmind.ui.components.DisclosureRow
import com.example.openmind.ui.components.EyeIcon
import com.example.openmind.ui.components.EyeMark
import com.example.openmind.ui.components.HourlyChart
import com.example.openmind.ui.components.IconCircle
import com.example.openmind.ui.components.MiniMetric
import com.example.openmind.ui.components.Monogram
import com.example.openmind.ui.components.OMCard
import com.example.openmind.ui.components.Pill
import com.example.openmind.ui.components.PrimaryButton
import com.example.openmind.ui.components.ProgressRing
import com.example.openmind.ui.components.SectionHeader
import com.example.openmind.ui.components.SectionLabel
import com.example.openmind.ui.components.eyePress
import com.example.openmind.ui.theme.Accent
import com.example.openmind.ui.theme.AccentDim
import com.example.openmind.ui.theme.AccentSoft
import com.example.openmind.ui.theme.EyeMotion
import com.example.openmind.ui.theme.EyeShape
import com.example.openmind.ui.theme.EyeSpace
import com.example.openmind.ui.theme.Hairline
import com.example.openmind.ui.theme.Ink
import com.example.openmind.ui.theme.MinTouch
import com.example.openmind.ui.theme.SurfaceOne
import com.example.openmind.ui.theme.SurfaceThree
import com.example.openmind.ui.theme.TextPrimary
import com.example.openmind.ui.theme.TextSecondary
import com.example.openmind.ui.theme.TextTertiary
import com.example.openmind.ui.theme.Warn
import com.example.openmind.ui.todayEpochDay
import java.time.LocalDate
import java.time.LocalTime

/**
 * Today, built to be read in one second.
 *
 * One ring, one number, one sentence, one button. The ring carries the only judgement the app makes:
 * how far toward the number you chose while you were calm. Colour does that work so no sentence has
 * to. Everything explanatory folds under "today in detail" - nothing deleted, only put one tap away,
 * so the first paint stays quiet enough to actually look at.
 */
@Composable
fun TodayScreen(
    settings: Settings,
    events: List<BlockEvent>,
    watch: List<WatchRecord>,
    permissions: PermissionState,
    onOpenFocus: () -> Unit,
    onOpenShields: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenPlan: () -> Unit,
    onEnableAccessibility: () -> Unit,
    onLogFeeling: (String) -> Unit,
    onDismissLapse: () -> Unit,
    onStartReentry: () -> Unit,
) {
    val context = LocalContext.current
    val day = LocalDate.now().toEpochDay()

    val summary = remember(watch, day) { AttentionStats.forDay(watch, day) }
    val surfaces = remember(watch, day) { AttentionStats.bySurface(watch, day, day) }
    val hourly = remember(watch, day) { AttentionStats.hourly(watch, day) }
    val hourlyList = remember(hourly) { hourly.toList() }
    val weekSummary = remember(watch, day) { AttentionStats.summarize(watch, day - 6, day) }
    val weekDaily = remember(watch, day) { AttentionStats.daily(watch, (day - 6..day).toList()) }
    val blocksToday = remember(events, day) {
        events.count { it.outcome == "BLOCKED" && todayEpochDay(it.epochMs) == day }
    }
    val message = remember(summary, settings) {
        Coach.todayMessage(summary.millis, summary.exposures, settings, settings.promptCursor)
    }
    val rate = TimeTranslation.atThisRate(summary.millis)
    val reveal = Coach.feelingReveal(settings.feelings)
    val checkedInToday = settings.feelings.any { it.epochDay == day }
    val armed = settings.plans.count { it.enabled }
    val hasData = summary.millis > 0L
    val cta = message.cta

    val rule = settings.contract.stopRuleMinutes
    val ruleMillis = rule * 60_000L
    val over = hasData && rule > 0 && summary.millis >= ruleMillis
    val fraction = when {
        !hasData -> 0f
        rule > 0 -> summary.millis.toFloat() / ruleMillis.toFloat()
        else -> 1f
    }
    // Amber, never red. The number is a fact; the ring is weather, not a verdict.
    // With no stop rule there is nothing to be a fraction of, so the ring stays empty and quiet:
    // accent only ever means "measured against the number you chose".
    val ringColor = when {
        !hasData || rule <= 0 -> Hairline
        fraction >= 1f -> Warn
        else -> Accent
    }
    val ringProgress = if (rule > 0) fraction else 0f
    val status = when {
        !hasData -> "nothing yet"
        rule <= 0 -> "no limit set"
        over -> "${formatShort(summary.millis - ruleMillis)} past"
        else -> "${formatShort(ruleMillis - summary.millis)} left"
    }

    var details by rememberSaveable { mutableStateOf(false) }
    // Read in this scope, not inside the LazyColumn DSL, so toggling it recomposes the screen.
    val showDetails = details

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(EyeSpace.screen, EyeSpace.md, EyeSpace.screen, EyeSpace.xxxl),
        verticalArrangement = Arrangement.spacedBy(EyeSpace.between),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EyeMark(modifier = Modifier.size(22.dp), color = Accent)
                Spacer(Modifier.width(EyeSpace.md))
                Text(
                    "openeye",
                    style = MaterialTheme.typography.labelLarge,
                    color = Accent,
                    modifier = Modifier.weight(1f),
                )
                IconCircle(onClick = onOpenSettings, icon = "settings", tint = TextSecondary)
            }
        }

        item {
            OMCard(
                modifier = Modifier.fillMaxWidth(),
                padding = PaddingValues(EyeSpace.xl),
                glow = Accent,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(
                        progress = ringProgress,
                        modifier = Modifier.size(148.dp),
                        strokeWidth = 11.dp,
                        color = ringColor,
                        track = SurfaceThree,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (hasData) TimeTranslation.hoursText(summary.millis) else "0m",
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary,
                                maxLines = 1,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (rule > 0) "of ${rule}m" else "in feeds",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary,
                            )
                        }
                    }
                    Spacer(Modifier.width(EyeSpace.md))
                    Column(modifier = Modifier.weight(1f)) {
                        SectionLabel("today")
                        Spacer(Modifier.height(EyeSpace.md))
                        Pill(text = status, color = if (over) Warn else if (hasData) Accent else TextTertiary)
                        Spacer(Modifier.height(EyeSpace.md))
                        Text(
                            text = if (hasData) TimeTranslation.oneThing(summary.millis).detail else message.secondary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        if (!permissions.accessibility) {
            item { SetupCard(onEnableAccessibility = onEnableAccessibility) }
        } else if (cta != null) {
            item {
                PrimaryButton(
                    text = cta,
                    icon = when (message.target) {
                        "plan" -> "target"
                        "focus" -> "clock"
                        "reentry" -> "spark"
                        else -> "chart"
                    },
                    onClick = when (message.target) {
                        "plan" -> onOpenPlan
                        "focus" -> onOpenFocus
                        "reentry" -> onStartReentry
                        else -> onOpenInsights
                    },
                )
            }
        }

        if (over && settings.lapseHandledDay != day) {
            item {
                LapseCard(
                    onAnswer = { cause ->
                        onLogFeeling(cause)
                        onDismissLapse()
                    },
                    onSkip = onDismissLapse,
                    onBreathe = onStartReentry,
                )
            }
        }

        item {
            SectionHeader(
                text = "what you watched",
                action = if (surfaces.size > 4) "all numbers" else null,
                onAction = onOpenInsights,
            )
        }
        item { WatchRows(surfaces) }
        item {
            Text(
                "time is measured. clip counts are estimates.",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
        }

        item {
            Chips(
                armed = armed,
                onOpenPlan = onOpenPlan,
                onOpenFocus = onOpenFocus,
                onOpenSchedule = onOpenSchedule,
                onOpenShields = onOpenShields,
            )
        }

        item {
            FeelingCheck(
                checkedInToday = checkedInToday,
                reveal = reveal,
                onPick = onLogFeeling,
            )
        }

        item {
            DisclosureRow(
                label = if (showDetails) "hide the detail" else "today in detail",
                expanded = showDetails,
                onClick = { details = !details },
            )
        }

        if (showDetails) {
            item {
                DetailCard(
                    secondary = if (hasData) message.secondary else null,
                    rate = rate,
                    summary = summary,
                    blocksToday = blocksToday,
                )
            }
            item {
                OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(EyeSpace.xl)) {
                    Column {
                        SectionLabel("when it happened")
                        Spacer(Modifier.height(EyeSpace.lg))
                        HourlyChart(hourly)
                    }
                }
            }
            item {
                SummaryCard(
                    daySummary = summary,
                    dayHourly = hourlyList,
                    weekSummary = weekSummary,
                    weekDaily = weekDaily,
                    onShare = { picked, bars, label -> SummaryShare.shareAsync(context, picked, bars, label) },
                )
            }
        }

        item {
            Text(
                "all of this stayed on your phone.",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Setup is one card with one button in it. The old version explained the switch in two sentences and
 * buried the switch; someone who has to read to find a button will not find the button.
 */
@Composable
private fun SetupCard(onEnableAccessibility: () -> Unit) {
    OMCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(EyeSpace.xl),
        surface = AccentSoft,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EyeIcon("shield", tint = Accent, size = 20.dp)
                Spacer(Modifier.width(EyeSpace.md))
                Text(
                    "one switch to start",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(EyeSpace.sm))
            Text(
                "in the list: openeye → use openeye's service.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(EyeSpace.lg))
            PrimaryButton(text = "open the switch", icon = "forward", onClick = onEnableAccessibility)
        }
    }
}

/**
 * Where the time went. The bar is the same measured number as the text, scaled against the biggest
 * feed today - shape before reading, so the ranking is legible without decoding a single digit.
 */
@Composable
private fun WatchRows(surfaces: List<SurfaceAttention>) {
    if (surfaces.isEmpty()) {
        OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(EyeSpace.xl), raised = false) {
            Text(
                "no feeds detected today.",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
        }
        return
    }
    val shown = surfaces.take(4)
    val max = surfaces.maxOf { it.millis }.coerceAtLeast(1L)

    OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(vertical = EyeSpace.xs)) {
        Column {
            shown.forEachIndexed { index, item ->
                val platform = PlatformCatalog.byId(item.platformId)
                val surface = PlatformCatalog.surface(item.platformId, item.surfaceId)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = EyeSpace.lg, vertical = EyeSpace.md),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (platform != null) {
                            Monogram(platform, size = 28.dp)
                            Spacer(Modifier.width(EyeSpace.md))
                        }
                        Text(
                            text = "${platform?.name ?: item.platformId} · ${surface?.label ?: item.surfaceId}",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(EyeSpace.md))
                        Text(
                            formatShort(item.millis),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                    }
                    Spacer(Modifier.height(EyeSpace.sm))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .background(SurfaceThree, RoundedCornerShape(EyeShape.chip)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((item.millis.toFloat() / max.toFloat()).coerceIn(0.03f, 1f))
                                .height(5.dp)
                                .background(Accent, RoundedCornerShape(EyeShape.chip)),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${item.exposures} clips",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                    )
                }
                if (index != shown.lastIndex) Divider(modifier = Modifier.padding(horizontal = EyeSpace.lg))
            }
        }
    }
}

/**
 * Four shortcuts, one row, no scrolling. A row that clips mid-word reads as a broken layout and
 * people stop tapping it; equal weights keep every destination visible at once. State is carried by
 * colour rather than by extra words: an armed plan makes its own chip glow.
 */
@Composable
private fun Chips(
    armed: Int,
    onOpenPlan: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenShields: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(EyeSpace.sm),
    ) {
        Chip("plans", "lock", onOpenPlan, armed > 0, Modifier.weight(1f))
        Chip("focus", "clock", onOpenFocus, modifier = Modifier.weight(1f))
        Chip("quiet", "moon", onOpenSchedule, modifier = Modifier.weight(1f))
        Chip("nudges", "bell", onOpenShields, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Chip(
    label: String,
    icon: String,
    onClick: () -> Unit,
    active: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(EyeShape.chip)
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (active) AccentSoft else SurfaceOne, shape)
            .border(1.dp, if (active) Accent.copy(alpha = 0.45f) else Hairline, shape)
            .eyePress(onClick = onClick)
            .padding(horizontal = EyeSpace.md, vertical = EyeSpace.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(EyeSpace.xs),
    ) {
        EyeIcon(icon, tint = if (active) Accent else AccentDim, size = 16.dp)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) Accent else TextSecondary,
            maxLines = 1,
        )
    }
}

/**
 * The check-in stays on the main view: it is the one thing here that only the person can give.
 * Four words show and the rest are one tap away - seven choices at once is a decision, and a decision
 * is what people skip.
 */
@Composable
private fun FeelingCheck(checkedInToday: Boolean, reveal: String?, onPick: (String) -> Unit) {
    var more by rememberSaveable { mutableStateOf(false) }
    Column {
        SectionLabel(Voice.feelPrompt)
        Spacer(Modifier.height(EyeSpace.md))
        val words = if (more) Voice.feelWords else Voice.feelWords.take(4)
        words.chunked(4).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(EyeSpace.sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { word ->
                    FeelChip(word, checkedInToday, Modifier.weight(1f)) { onPick(word) }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(EyeSpace.sm))
        }
        if (!checkedInToday && Voice.feelWords.size > 4) {
            Text(
                text = if (more) "fewer words" else "more words",
                style = MaterialTheme.typography.labelMedium,
                color = AccentDim,
                modifier = Modifier
                    .clip(RoundedCornerShape(EyeShape.field))
                    .eyePress(onClick = { more = !more })
                    .padding(vertical = EyeSpace.xs),
            )
        }
        val line = reveal ?: if (checkedInToday) "logged. the pattern builds." else null
        if (line != null) {
            Spacer(Modifier.height(EyeSpace.xs))
            Text(line, style = MaterialTheme.typography.bodySmall, color = AccentDim)
        }
    }
}

@Composable
private fun FeelChip(word: String, done: Boolean, modifier: Modifier = Modifier, onPick: () -> Unit) {
    val chipBackground by animateColorAsState(
        targetValue = if (done) SurfaceOne else AccentSoft,
        animationSpec = tween(EyeMotion.normal),
        label = "feelpill",
    )
    Box(
        modifier = modifier
            .height(MinTouch)
            .clip(RoundedCornerShape(EyeShape.chip))
            .background(chipBackground, RoundedCornerShape(EyeShape.chip))
            .border(1.dp, Hairline, RoundedCornerShape(EyeShape.chip))
            .eyePress(enabled = !done, onClick = onPick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            word,
            style = MaterialTheme.typography.labelMedium,
            color = if (done) TextTertiary else TextSecondary,
            maxLines = 1,
        )
    }
}

/**
 * The slip. Normalise, get curious, offer something small. Nothing here says "reset tomorrow",
 * because the day a plan breaks is exactly the day an app usually loses the person.
 */
@Composable
private fun LapseCard(
    onAnswer: (String) -> Unit,
    onSkip: () -> Unit,
    onBreathe: () -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(EyeSpace.xl)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EyeIcon("heart", tint = Warn, size = 18.dp)
                Spacer(Modifier.width(EyeSpace.md))
                Text(
                    Voice.pick(Voice.lapse, LocalTime.now().hour),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(EyeSpace.lg))
            if (open) {
                Text(Voice.lapseQuestion, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(EyeSpace.md))
                lapseRows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(EyeSpace.sm),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        row.forEach { cause ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(MinTouch)
                                    .clip(RoundedCornerShape(EyeShape.chip))
                                    .background(SurfaceThree, RoundedCornerShape(EyeShape.chip))
                                    .border(1.dp, Hairline, RoundedCornerShape(EyeShape.chip))
                                    .eyePress(onClick = { onAnswer(cause) }),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    cause,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                    maxLines = 1,
                                )
                            }
                        }
                        repeat(lapseRowSize - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(EyeSpace.sm))
                }
                Text(
                    "10 focused minutes takes the night back.",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentDim,
                    modifier = Modifier
                        .clip(RoundedCornerShape(EyeShape.field))
                        .eyePress(onClick = onBreathe)
                        .padding(vertical = EyeSpace.sm),
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(EyeSpace.sm)) {
                    Text(
                        "look at it",
                        style = MaterialTheme.typography.labelLarge,
                        color = Ink,
                        modifier = Modifier
                            .clip(RoundedCornerShape(EyeShape.chip))
                            .background(Accent, RoundedCornerShape(EyeShape.chip))
                            .eyePress(onClick = { open = true })
                            .padding(horizontal = EyeSpace.lg, vertical = EyeSpace.md),
                    )
                    Text(
                        "drop it",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(EyeShape.chip))
                            .border(1.dp, Hairline, RoundedCornerShape(EyeShape.chip))
                            .eyePress(onClick = onSkip)
                            .padding(horizontal = EyeSpace.lg, vertical = EyeSpace.md),
                    )
                }
            }
        }
    }
}

/**
 * Everything that used to be four cards, in one place: the rotating coach line, the yearly rate,
 * the session shape, and how often a shield actually fired.
 */
@Composable
private fun DetailCard(secondary: String?, rate: String?, summary: AttentionSummary, blocksToday: Int) {
    OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(EyeSpace.xl)) {
        Column {
            if (secondary != null) {
                Text(secondary, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.height(EyeSpace.md))
            }
            if (rate != null) {
                Text(rate, style = MaterialTheme.typography.bodySmall, color = AccentDim)
                Spacer(Modifier.height(EyeSpace.md))
            }
            if (blocksToday > 0) {
                Text(
                    "pulled you out $blocksToday ${if (blocksToday == 1) "time" else "times"} today.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(EyeSpace.lg))
            }
            Row {
                MiniMetric("${summary.exposures}", "clips est.", Modifier.weight(1f))
                MiniMetric("${summary.transitions}", "swipes", Modifier.weight(1f))
                MiniMetric("${summary.sessions}", "visits", Modifier.weight(1f))
                MiniMetric(formatShort(summary.longestSessionMs), "longest", Modifier.weight(1f))
            }
            Spacer(Modifier.height(EyeSpace.md))
            Text(
                TimeTranslation.clipPace(summary.millis, summary.exposures)
                    ?: "auto-advance without a real swipe isn't counted, so this under-reports.",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
        }
    }
}

@Composable
private fun SummaryCard(
    daySummary: AttentionSummary,
    dayHourly: List<Long>,
    weekSummary: AttentionSummary,
    weekDaily: List<Long>,
    onShare: (AttentionSummary, List<Long>, String) -> Unit,
) {
    var weekly by rememberSaveable { mutableStateOf(true) }
    val shown = if (weekly) weekSummary else daySummary
    val bars = if (weekly) weekDaily else dayHourly
    val barsLabel = if (weekly) "last 7 days" else "today by hour"
    // Hoisted out of the draw lambda: the canvas repaints, the list should not be rebuilt each frame.
    val shownBars = remember(bars) { bars.takeLast(24) }

    OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(EyeSpace.xl)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(if (weekly) "this week" else "today", modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(EyeSpace.xs)) {
                    SummaryToggle("day", selected = !weekly) { weekly = false }
                    SummaryToggle("week", selected = weekly) { weekly = true }
                }
            }
            Spacer(Modifier.height(EyeSpace.lg))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    formatShort(shown.millis),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                )
                Spacer(Modifier.width(EyeSpace.sm))
                Text(
                    if (weekly) "last 7 days" else "today",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    modifier = Modifier.padding(bottom = EyeSpace.sm),
                )
            }
            Spacer(Modifier.height(EyeSpace.lg))
            Row {
                SummaryStat("${shown.exposures}", "clips", Modifier.weight(1f))
                SummaryStat(formatShort(shown.averageItemMs), "per clip", Modifier.weight(1f))
                SummaryStat("${shown.swipes}", "swipes", Modifier.weight(1f))
                SummaryStat("${shown.touches}", "taps", Modifier.weight(1f))
            }
            Spacer(Modifier.height(EyeSpace.lg))
            SummaryBars(shownBars)
            Spacer(Modifier.height(EyeSpace.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    barsLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "share it",
                    style = MaterialTheme.typography.labelLarge,
                    color = Accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(EyeShape.field))
                        .eyePress(onClick = { onShare(shown, shownBars, barsLabel) })
                        .padding(EyeSpace.sm),
                )
            }
        }
    }
}

@Composable
private fun SummaryBars(bars: List<Long>) {
    val activeColor = Accent
    val idleColor = Hairline.copy(alpha = 0.5f)
    Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) {
        val max = (bars.maxOrNull() ?: 0L).coerceAtLeast(1L)
        val count = bars.size.coerceAtLeast(1)
        val step = size.width / count
        val barWidth = (step * 0.5f).coerceAtLeast(2f)
        val corner = barWidth / 2f
        bars.forEachIndexed { index, value ->
            val fraction = value.toFloat() / max.toFloat()
            val barHeight = (size.height * (0.08f + 0.92f * fraction)).coerceAtMost(size.height)
            drawRoundRect(
                color = if (value > 0L) activeColor else idleColor,
                topLeft = Offset(index * step + (step - barWidth) / 2f, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(corner, corner),
            )
        }
    }
}

@Composable
private fun SummaryToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    val toggleBackground by animateColorAsState(
        targetValue = if (selected) Accent else SurfaceOne,
        animationSpec = tween(EyeMotion.quick),
        label = "summarytoggle",
    )
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) Ink else TextSecondary,
        modifier = Modifier
            .clip(RoundedCornerShape(EyeShape.chip))
            .background(toggleBackground, RoundedCornerShape(EyeShape.chip))
            .border(1.dp, if (selected) Accent else Hairline, RoundedCornerShape(EyeShape.chip))
            .eyePress(onClick = onClick)
            .padding(horizontal = EyeSpace.md, vertical = 6.dp),
    )
}

@Composable
private fun SummaryStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
    }
}

/** Chunked once, at class load: these pools are immutable, so per-recomposition chunking is waste. */
private const val lapseRowSize = 3
private val lapseRows: List<List<String>> = Voice.lapseLadder.chunked(lapseRowSize)

private fun formatShort(millis: Long): String {
    val minutes = millis / 60_000L
    return when {
        millis <= 0L -> "0m"
        minutes < 1 -> "<1m"
        minutes < 60 -> "${minutes}m"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }
}

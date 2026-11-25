package com.example.openmind.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
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
import com.example.openmind.ui.components.EyeMark
import com.example.openmind.ui.components.HourlyChart
import com.example.openmind.ui.components.MiniMetric
import com.example.openmind.ui.components.Monogram
import com.example.openmind.ui.components.NavGlyph
import com.example.openmind.ui.components.OMCard
import com.example.openmind.ui.components.SectionLabel
import com.example.openmind.ui.theme.Accent
import com.example.openmind.ui.theme.AccentDim
import com.example.openmind.ui.theme.AccentSoft
import com.example.openmind.ui.theme.Hairline
import com.example.openmind.ui.theme.Ink
import com.example.openmind.ui.theme.SurfaceOne
import com.example.openmind.ui.theme.TextPrimary
import com.example.openmind.ui.theme.TextSecondary
import com.example.openmind.ui.theme.TextTertiary
import com.example.openmind.ui.todayEpochDay
import java.time.LocalDate
import java.time.LocalTime

/**
 * Today, built to be read in one second.
 *
 * The rule for this screen: a measured number, one short line under it, one action, then the list
 * of where the time actually went. Every sentence that explained the app, or the data, now lives
 * behind "today in detail" - nothing was removed, only folded away, so the first paint stays calm.
 * Clip counts are the only estimate on the page, and the footnote under the list says so.
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
    var details by rememberSaveable { mutableStateOf(false) }
    // Read in this scope, not inside the LazyColumn DSL, so toggling it recomposes the screen.
    val showDetails = details

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EyeMark(modifier = Modifier.size(20.dp), color = Accent)
                Spacer(Modifier.width(9.dp))
                Text(
                    "openeye",
                    style = MaterialTheme.typography.labelSmall,
                    color = Accent,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenSettings),
                    contentAlignment = Alignment.Center,
                ) {
                    NavGlyph(type = "settings", tint = TextSecondary, modifier = Modifier.size(22.dp))
                }
            }
        }

        item { Spacer(Modifier.height(34.dp)) }
        item {
            val rule = settings.contract.stopRuleMinutes
            Column {
                SectionLabel("feed time")
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (hasData) TimeTranslation.hoursText(summary.millis) else "0m",
                        style = MaterialTheme.typography.displayLarge,
                        color = TextPrimary,
                    )
                    if (rule > 0) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "of ${rule}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasData && summary.millis >= rule * 60_000L) AccentDim else TextTertiary,
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (hasData) TimeTranslation.oneThing(summary.millis).detail else "nothing tracked yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }

        if (!permissions.accessibility) {
            item { Spacer(Modifier.height(18.dp)) }
            item { SetupLine(onEnableAccessibility = onEnableAccessibility) }
        }

        if (cta != null) {
            item { Spacer(Modifier.height(18.dp)) }
            item {
                ActionLine(
                    text = cta,
                    onClick = when (message.target) {
                        "plan" -> onOpenPlan
                        "focus" -> onOpenFocus
                        "reentry" -> onStartReentry
                        else -> onOpenInsights
                    },
                )
            }
        }

        if (Coach.isLapse(summary.millis, settings.contract) && settings.lapseHandledDay != day) {
            item { Spacer(Modifier.height(12.dp)) }
            item {
                LapseCard(
                    onAnswer = { cause ->
                        onLogFeeling(cause)
                        onDismissLapse()
                    },
                    onSkip = onDismissLapse,
                )
            }
        }

        item { Spacer(Modifier.height(30.dp)) }
        item { SectionLabel("what you watched") }
        item { Spacer(Modifier.height(4.dp)) }
        item { WatchRows(surfaces, onOpenInsights = onOpenInsights) }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            Text(
                "time is measured. clip counts are estimates.",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
        }

        item { Spacer(Modifier.height(20.dp)) }
        item {
            Chips(
                armed = armed,
                onOpenPlan = onOpenPlan,
                onOpenFocus = onOpenFocus,
                onOpenSchedule = onOpenSchedule,
                onOpenShields = onOpenShields,
            )
        }

        item { Spacer(Modifier.height(26.dp)) }
        item { FeelingCheck(checkedInToday = checkedInToday, reveal = reveal, onPick = onLogFeeling) }

        item { Spacer(Modifier.height(20.dp)) }
        item { DetailsRow(expanded = showDetails, onClick = { details = !details }) }

        if (showDetails) {
            item { Spacer(Modifier.height(14.dp)) }
            item {
                DetailCard(
                    secondary = if (hasData) message.secondary else null,
                    rate = rate,
                    summary = summary,
                    blocksToday = blocksToday,
                )
            }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                    Column {
                        SectionLabel("when it happened")
                        Spacer(Modifier.height(14.dp))
                        HourlyChart(hourly)
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
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

        item { Spacer(Modifier.height(26.dp)) }
        item {
            Text(
                "all of this stayed on your phone.",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

/** Setup is one row and one button. The two sentences that used to explain it are gone for good. */
@Composable
private fun SetupLine(onEnableAccessibility: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AccentSoft, RoundedCornerShape(18.dp))
            .border(1.dp, AccentDim.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("one switch to start", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(3.dp))
            Text(
                "in the list: openeye → use openeye's service.",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .background(Accent, RoundedCornerShape(100.dp))
                .clickable(onClick = onEnableAccessibility)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text("open the switch", style = MaterialTheme.typography.labelMedium, color = Ink)
        }
    }
}

@Composable
private fun ActionLine(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceOne, RoundedCornerShape(18.dp))
            .border(1.dp, Hairline, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = Accent, modifier = Modifier.weight(1f))
        Text("→", style = MaterialTheme.typography.titleMedium, color = Accent)
    }
}

/** Where the time went: name, measured time, estimated clips. No caption per row. */
@Composable
private fun WatchRows(surfaces: List<SurfaceAttention>, onOpenInsights: () -> Unit) {
    if (surfaces.isEmpty()) {
        Text(
            "no feeds detected today.",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
        )
        return
    }
    val shown = if (surfaces.size > 5) surfaces.subList(0, 5) else surfaces

    OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(vertical = 2.dp)) {
        Column {
            shown.forEachIndexed { index, item ->
                val platform = PlatformCatalog.byId(item.platformId)
                val surface = PlatformCatalog.surface(item.platformId, item.surfaceId)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (platform != null) {
                        Monogram(platform, size = 30.dp)
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(
                        text = "${platform?.name ?: item.platformId} · ${surface?.label ?: item.surfaceId}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatShort(item.millis), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("${item.exposures} clips", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    }
                }
                if (index != shown.lastIndex) Divider(modifier = Modifier.padding(horizontal = 14.dp))
            }
        }
    }

    if (surfaces.size > shown.size) {
        Spacer(Modifier.height(10.dp))
        Text(
            "all numbers →",
            style = MaterialTheme.typography.labelMedium,
            color = AccentDim,
            modifier = Modifier.clickable(onClick = onOpenInsights),
        )
    }
}

@Composable
private fun Chips(
    armed: Int,
    onOpenPlan: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenShields: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Chip(if (armed == 0) "if-then plans" else "if-then plans · $armed armed", onOpenPlan)
        Chip("focus timer", onOpenFocus)
        Chip("quiet hours", onOpenSchedule)
        Chip("reminders", onOpenShields)
    }
}

@Composable
private fun Chip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(SurfaceOne, RoundedCornerShape(100.dp))
            .border(1.dp, Hairline, RoundedCornerShape(100.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}

/** The check-in stays on the main view: it is the one thing here that only the person can give. */
@Composable
private fun FeelingCheck(checkedInToday: Boolean, reveal: String?, onPick: (String) -> Unit) {
    Column {
        SectionLabel(Voice.feelPrompt)
        Spacer(Modifier.height(10.dp))
        feelRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { word ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (checkedInToday) SurfaceOne else AccentSoft, RoundedCornerShape(100.dp))
                            .border(1.dp, Hairline, RoundedCornerShape(100.dp))
                            .clickable(enabled = !checkedInToday) { onPick(word) }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            word,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (checkedInToday) TextTertiary else TextSecondary,
                            maxLines = 1,
                        )
                    }
                }
                repeat(feelRowSize - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(8.dp))
        }
        val line = reveal ?: if (checkedInToday) "logged. the pattern builds." else null
        if (line != null) {
            Text(line, style = MaterialTheme.typography.bodySmall, color = AccentDim)
        }
    }
}

@Composable
private fun DetailsRow(expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (expanded) "hide the detail" else "today in detail",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(if (expanded) "↑" else "↓", style = MaterialTheme.typography.titleMedium, color = Accent)
    }
}

/**
 * Everything that used to be four cards, in one place: the rotating coach line, the yearly rate,
 * the session shape, and how often a shield actually fired.
 */
@Composable
private fun DetailCard(secondary: String?, rate: String?, summary: AttentionSummary, blocksToday: Int) {
    OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
        Column {
            if (secondary != null) {
                Text(secondary, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.height(10.dp))
            }
            if (rate != null) {
                Text(rate, style = MaterialTheme.typography.bodySmall, color = AccentDim)
                Spacer(Modifier.height(10.dp))
            }
            if (blocksToday > 0) {
                Text(
                    "pulled you out $blocksToday ${if (blocksToday == 1) "time" else "times"} today.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(14.dp))
            }
            Row {
                MiniMetric("${summary.exposures}", "clips est.", Modifier.weight(1f))
                MiniMetric("${summary.transitions}", "swipes", Modifier.weight(1f))
                MiniMetric("${summary.sessions}", "visits", Modifier.weight(1f))
                MiniMetric(formatShort(summary.longestSessionMs), "longest", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                TimeTranslation.clipPace(summary.millis, summary.exposures)
                    ?: "auto-advance without a real swipe isn't counted, so this under-reports.",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
        }
    }
}

/**
 * The slip. Normalise, get curious, offer something small. Nothing here says "reset tomorrow",
 * because the day a plan breaks is exactly the day an app usually loses the person.
 */
@Composable
private fun LapseCard(onAnswer: (String) -> Unit, onSkip: () -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
        Column {
            Text(
                Voice.pick(Voice.lapse, LocalTime.now().hour),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.height(10.dp))
            if (open) {
                Text(Voice.lapseQuestion, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    lapseRows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { cause ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(SurfaceOne, RoundedCornerShape(100.dp))
                                        .border(1.dp, Hairline, RoundedCornerShape(100.dp))
                                        .clickable { onAnswer(cause) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(cause, style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1)
                                }
                            }
                            repeat(lapseRowSize - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "10 focused minutes takes the night back.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .background(Accent, RoundedCornerShape(100.dp))
                            .clickable { open = true }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                    ) {
                        Text("look at it", style = MaterialTheme.typography.labelMedium, color = Ink)
                    }
                    Box(
                        modifier = Modifier
                            .border(1.dp, Hairline, RoundedCornerShape(100.dp))
                            .clickable(onClick = onSkip)
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                    ) {
                        Text("drop it", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    }
                }
            }
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
    // Hoisted out of the draw lambda: the canvas repaints, the list should not be rebuilt each time.
    val shownBars = remember(bars) { bars.takeLast(24) }
    val barColor = Accent
    val idleColor = Hairline.copy(alpha = 0.6f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(AccentSoft, SurfaceOne)), RoundedCornerShape(28.dp))
            .border(1.dp, AccentDim.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
            .padding(20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(if (weekly) "this week" else "today", modifier = Modifier.weight(1f))
                SummaryToggle("day", selected = !weekly) { weekly = false }
                Spacer(Modifier.width(4.dp))
                SummaryToggle("week", selected = weekly) { weekly = true }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(formatShort(shown.millis), style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (weekly) "last 7 days" else "today",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
            }
            Spacer(Modifier.height(14.dp))
            Row {
                SummaryStat("${shown.exposures}", "clips", Modifier.weight(1f))
                SummaryStat(formatShort(shown.averageItemMs), "per clip", Modifier.weight(1f))
                SummaryStat("${shown.swipes}", "swipes", Modifier.weight(1f))
                SummaryStat("${shown.touches}", "taps", Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                val max = (shownBars.maxOrNull() ?: 0L).coerceAtLeast(1L)
                val count = shownBars.size.coerceAtLeast(1)
                val step = size.width / count
                val barWidth = (step * 0.55f).coerceAtLeast(1f)
                val corner = (barWidth / 2f).coerceAtMost(10f)
                shownBars.forEachIndexed { index, value ->
                    val fraction = value.toFloat() / max.toFloat()
                    val barHeight = (size.height * (0.12f + 0.88f * fraction)).coerceAtMost(size.height)
                    val x = index * step + (step - barWidth) / 2f
                    drawRoundRect(
                        color = if (value > 0L) barColor else idleColor,
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(corner, corner),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    barsLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "share it",
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent,
                    modifier = Modifier.clickable { onShare(shown, shownBars, barsLabel) },
                )
            }
        }
    }
}

@Composable
private fun SummaryToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) Accent else SurfaceOne, RoundedCornerShape(100.dp))
            .border(1.dp, if (selected) Accent else Hairline, RoundedCornerShape(100.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Ink else TextSecondary,
        )
    }
}

@Composable
private fun SummaryStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
    }
}

/** Chunked once, at class load: these pools are immutable, so per-recomposition chunking is waste. */
private const val feelRowSize = 4
private const val lapseRowSize = 3
private val feelRows: List<List<String>> = Voice.feelWords.chunked(feelRowSize)
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

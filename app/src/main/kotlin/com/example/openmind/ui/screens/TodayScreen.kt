package com.example.openmind.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.example.openmind.domain.TimeTranslation
import com.example.openmind.domain.Voice
import com.example.openmind.ui.PermissionState
import com.example.openmind.ui.SummaryShare
import com.example.openmind.ui.components.EyeMark
import com.example.openmind.ui.components.HourlyChart
import com.example.openmind.ui.components.MiniMetric
import com.example.openmind.ui.components.Monogram
import com.example.openmind.ui.components.NavGlyph
import com.example.openmind.ui.components.OMCard
import com.example.openmind.ui.components.PrimaryButton
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
    val today = LocalDate.now()
    val day = today.toEpochDay()
    val summary = AttentionStats.forDay(watch, day)
    val yesterday = AttentionStats.forDay(watch, day - 1)
    val surfaces = AttentionStats.bySurface(watch, day, day)
    val hourly = AttentionStats.hourly(watch, day)
    val blocksToday = events.count { it.outcome == "BLOCKED" && todayEpochDay(it.epochMs) == day }
    val cursor = settings.promptCursor
    val message = Coach.todayMessage(summary.millis, summary.exposures, settings, cursor)
    val equivalent = TimeTranslation.oneThing(summary.millis)
    val rate = TimeTranslation.atThisRate(summary.millis)
    val lapseVisible = Coach.isLapse(summary.millis, settings.contract) && settings.lapseHandledDay != day
    val reveal = Coach.feelingReveal(settings.feelings)
    val checkedInToday = settings.feelings.any { it.epochDay == day }
    val context = LocalContext.current

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
                        .background(SurfaceOne, RoundedCornerShape(100.dp))
                        .border(1.dp, Hairline, RoundedCornerShape(100.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "${summary.exposures} clips · ${formatShort(summary.millis)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentDim,
                    )
                }
                Spacer(Modifier.width(4.dp))
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
        item { Spacer(Modifier.height(22.dp)) }
        item {
            Text(openingLine(), style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
        }
        if (!permissions.accessibility) {
            item { Spacer(Modifier.height(16.dp)) }
            item {
                OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                    Column {
                        Text(
                            "one switch to start",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "it's how the app knows a feed is open. nothing leaves this phone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "in the list: openeye -> use openeye's service.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                        )
                        Spacer(Modifier.height(14.dp))
                        PrimaryButton(text = "open the switch", onClick = onEnableAccessibility)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
                Column {
                    SectionLabel("feed time today")
                    Spacer(Modifier.height(8.dp))
                    Text(message.primary, style = MaterialTheme.typography.displayLarge, color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text(message.secondary, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    Text(equivalent.detail, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    if (rate != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(rate, style = MaterialTheme.typography.bodySmall, color = AccentDim)
                    }
                    if (blocksToday > 0) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "pulled you out $blocksToday ${if (blocksToday == 1) "time" else "times"} today.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                        )
                    }
                    val cta = message.cta
                    if (cta != null) {
                        Spacer(Modifier.height(16.dp))
                        PrimaryButton(
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
            }
        }
        if (lapseVisible) {
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
        item { Spacer(Modifier.height(12.dp)) }
        item {
            FeelingCard(checkedInToday = checkedInToday, reveal = reveal, onPick = onLogFeeling)
        }
        item { Spacer(Modifier.height(14.dp)) }
        item {
            val armed = settings.plans.count { it.enabled }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenPlan)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "if-then plans",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (armed == 0) "make one" else "$armed armed",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
                Spacer(Modifier.width(8.dp))
                Text("→", style = MaterialTheme.typography.titleMedium, color = Accent)
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
        item {
            SummaryCard(
                daySummary = summary,
                dayHourly = hourly.toList(),
                weekSummary = AttentionStats.summarize(watch, day - 6, day),
                weekDaily = AttentionStats.daily(watch, ((day - 6)..day).toList()),
                onShare = { picked, bars, label -> SummaryShare.shareAsync(context, picked, bars, label) },
            )
        }
        item { Spacer(Modifier.height(26.dp)) }
        item { SectionLabel("who got the time") }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(6.dp)) {
                Column {
                    if (surfaces.isEmpty()) {
                        Text(
                            "no feeds detected today.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                            modifier = Modifier.padding(14.dp),
                        )
                    } else {
                        surfaces.take(6).forEachIndexed { index, item ->
                            val platform = PlatformCatalog.byId(item.platformId)
                            val surface = PlatformCatalog.surface(item.platformId, item.surfaceId)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (platform != null) Monogram(platform, size = 34.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        platform?.name ?: item.platformId,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                    )
                                    Text(
                                        surface?.blurb ?: item.surfaceId,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextTertiary,
                                    )
                                }
                                Text(formatShort(item.millis), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            }
                            if (index != surfaces.take(6).lastIndex) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp)
                                        .height(1.dp)
                                        .background(Hairline),
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(26.dp)) }
        item { SectionLabel("what a session looks like") }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                Column {
                    Row {
                        MiniMetric("${summary.exposures}", "clips passed", Modifier.weight(1f))
                        MiniMetric("${summary.transitions}", "swipes seen", Modifier.weight(1f))
                        MiniMetric("${summary.sessions}", "visits", Modifier.weight(1f))
                        MiniMetric(formatShort(summary.longestSessionMs), "longest", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    val pace = TimeTranslation.clipPace(summary.millis, summary.exposures)
                    Text(
                        pace ?: "auto-advance without a real swipe isn't counted, so this under-reports.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(26.dp)) }
        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                Column {
                    SectionLabel("when it happened")
                    Spacer(Modifier.height(14.dp))
                    HourlyChart(hourly)
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "all of this stayed on your phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "quiet hours",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentDim,
                    modifier = Modifier.clickable(onClick = onOpenSchedule),
                )
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

private fun openingLine(): String = when (LocalTime.now().hour) {
    in 5..11 -> "morning."
    in 12..16 -> "afternoon."
    in 17..21 -> "evening. the dangerous hours."
    else -> "it's late. you're here though."
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
            Spacer(Modifier.height(8.dp))
            Text(
                if (open) Voice.lapseQuestion else "no streak to lose.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(14.dp))
            if (open) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Voice.lapseLadder.chunked(3).forEach { row ->
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
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
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
private fun FeelingCard(checkedInToday: Boolean, reveal: String?, onPick: (String) -> Unit) {
    OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
        Column {
            SectionLabel(Voice.feelPrompt)
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Voice.feelWords.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { word ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (checkedInToday) SurfaceOne else AccentSoft, RoundedCornerShape(100.dp))
                                    .border(1.dp, Hairline, RoundedCornerShape(100.dp))
                                    .clickable(enabled = !checkedInToday) { onPick(word) }
                                    .padding(vertical = 10.dp),
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
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                reveal ?: if (checkedInToday) {
                    "logged. the pattern builds."
                } else {
                    "one word a day. the pattern gets interesting."
                },
                style = MaterialTheme.typography.bodySmall,
                color = AccentDim,
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
            Text(formatShort(shown.millis), style = MaterialTheme.typography.displayLarge, color = TextPrimary)
            Text(
                if (weekly) "reels + shorts, last 7 days" else "reels + shorts, today",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(16.dp))
            Row {
                SummaryStat("${shown.exposures}", "clips", Modifier.weight(1f))
                SummaryStat(formatShort(shown.averageItemMs), "per clip", Modifier.weight(1f))
                SummaryStat("${shown.swipes}", "swipes", Modifier.weight(1f))
                SummaryStat("${shown.touches}", "taps", Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                val shownBars = bars.takeLast(24)
                val max = (shownBars.maxOrNull() ?: 0L).coerceAtLeast(1L)
                val count = shownBars.size.coerceAtLeast(1)
                val step = size.width / count
                val barWidth = (step * 0.55f).coerceAtLeast(1f)
                shownBars.forEachIndexed { index, value ->
                    val fraction = value.toFloat() / max.toFloat()
                    val barHeight = (size.height * (0.12f + 0.88f * fraction)).coerceAtMost(size.height)
                    val x = index * step + (step - barWidth) / 2f
                    drawRoundRect(
                        color = if (value > 0L) barColor else idleColor,
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius((barWidth / 2f).coerceAtMost(10f), (barWidth / 2f).coerceAtMost(10f)),
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
                    modifier = Modifier.clickable { onShare(shown, bars, barsLabel) },
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

private fun formatShort(millis: Long): String {
    val minutes = millis / 60_000L
    return when {
        millis <= 0L -> "0m"
        minutes < 1 -> "<1m"
        minutes < 60 -> "${minutes}m"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }
}

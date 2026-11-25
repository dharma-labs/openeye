package com.example.openmind.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.openmind.data.BlockEvent
import com.example.openmind.data.PlatformCatalog
import com.example.openmind.data.Settings
import com.example.openmind.data.UsageSnapshot
import com.example.openmind.data.WatchRecord
import com.example.openmind.domain.AttentionStats
import com.example.openmind.domain.Coach
import com.example.openmind.domain.TimeTranslation
import com.example.openmind.domain.Voice
import com.example.openmind.ui.components.BarChart
import com.example.openmind.ui.components.Divider
import com.example.openmind.ui.components.GhostButton
import com.example.openmind.ui.components.HourlyChart
import com.example.openmind.ui.components.MiniMetric
import com.example.openmind.ui.components.Monogram
import com.example.openmind.ui.components.OMCard
import com.example.openmind.ui.components.PrimaryButton
import com.example.openmind.ui.components.ScreenHeader
import com.example.openmind.ui.components.SectionLabel
import com.example.openmind.ui.theme.Accent
import com.example.openmind.ui.theme.AccentDim
import com.example.openmind.ui.theme.Hairline
import com.example.openmind.ui.theme.Ink
import com.example.openmind.ui.theme.SurfaceOne
import com.example.openmind.ui.theme.TextPrimary
import com.example.openmind.ui.theme.TextSecondary
import com.example.openmind.ui.theme.TextTertiary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * The numbers page.
 *
 * The default view stays short: a hero total, a compact per-app breakdown and one trend. Everything
 * that reads as commentary - the translations, the observations, the plan prompt and whole-app screen
 * time - lives behind a single "advanced" row so it never drops a capability, only its prominence.
 * Every figure is the person's own tracked history; clips and swipes remain labelled as estimates.
 */
@Composable
fun InsightsScreen(
    settings: Settings,
    events: List<BlockEvent>,
    watch: List<WatchRecord>,
    usage: UsageSnapshot,
    onGrantUsage: () -> Unit,
    onRefresh: () -> Unit,
    onOpenPlan: () -> Unit,
) {
    var range by rememberSaveable { mutableIntStateOf(0) }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    val today = LocalDate.now()
    val todayDay = today.toEpochDay()
    val days = when (range) {
        0 -> 1
        1 -> 7
        else -> 30
    }
    val startDay = todayDay - (days - 1)
    val summary = AttentionStats.summarize(watch, startDay, todayDay)
    val previous = AttentionStats.summarize(watch, startDay - days, startDay - 1)
    val surfaces = AttentionStats.bySurface(watch, startDay, todayDay)
    val dayList = (startDay..todayDay).toList()
    val dailyMillis = AttentionStats.daily(watch, dayList)
    val hourly = AttentionStats.hourly(watch, todayDay)
    val peakHour = hourly.indices.maxByOrNull { hourly[it] }?.takeIf { hourly[it] > 0L }
    val deltaMinutes = ((summary.millis - previous.millis) / 60_000L).toInt()
    val trackedDays = AttentionStats.daily(watch, (todayDay - 29..todayDay).toList()).filter { it > 0L }
    val ownAverage = if (trackedDays.isEmpty()) 0L else trackedDays.sum() / trackedDays.size
    val reveal = Coach.feelingReveal(settings.feelings)
    val blockedTotal = events.count { it.outcome == "BLOCKED" }
    val experiment = experimentLine(surfaces, peakHour, summary.exposures)

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
    ) {
        item { ScreenHeader(title = "the numbers", subtitle = "no grades.") }
        item { Spacer(Modifier.height(16.dp)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("today", "7d", "30d").forEachIndexed { index, label ->
                    val selected = range == index
                    Box(
                        modifier = Modifier
                            .background(if (selected) Accent else SurfaceOne, RoundedCornerShape(100.dp))
                            .border(1.dp, if (selected) Accent else Hairline, RoundedCornerShape(100.dp))
                            .clickable { range = index }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Ink else TextSecondary,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
                Column {
                    SectionLabel(if (days == 1) "feed time" else "feed time, ${days}d")
                    Spacer(Modifier.height(8.dp))
                    Text(formatLong(summary.millis), style = MaterialTheme.typography.displayMedium, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        comparison(summary.millis, previous.millis, deltaMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (summary.millis <= previous.millis) AccentDim else TextSecondary,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniMetric("${summary.sessions}", "visits", Modifier.weight(1f))
                MiniMetric(formatShort(summary.longestSessionMs), "longest", Modifier.weight(1f))
                MiniMetric("${summary.transitions}", "swipes", Modifier.weight(1f))
                MiniMetric("${summary.exposures}", "clips", Modifier.weight(1f))
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Text(
                "time measured. clips and swipes estimated.",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
        item { SectionLabel("where it went") }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
                Column {
                    if (surfaces.isEmpty()) {
                        Text("nothing in this range.", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    } else {
                        val shown = surfaces.take(6)
                        val max = shown.maxOf { it.millis }.coerceAtLeast(1L)
                        shown.forEachIndexed { index, item ->
                            val platform = PlatformCatalog.byId(item.platformId)
                            val surface = PlatformCatalog.surface(item.platformId, item.surfaceId)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (platform != null) Monogram(platform, size = 32.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            platform?.name ?: item.platformId,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(surface?.label ?: item.surfaceId, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                        Spacer(Modifier.width(10.dp))
                                        Text(formatShort(item.millis), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                    Spacer(Modifier.height(7.dp))
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .background(SurfaceOne, RoundedCornerShape(100.dp)),
                                    ) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth((item.millis.toFloat() / max).coerceIn(0f, 1f))
                                                .height(5.dp)
                                                .background(
                                                    if (index == 0) Accent else AccentDim.copy(alpha = 0.55f),
                                                    RoundedCornerShape(100.dp),
                                                ),
                                        )
                                    }
                                }
                            }
                            if (index != shown.lastIndex) Spacer(Modifier.height(14.dp))
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                Column {
                    SectionLabel(if (days == 1) "when" else "by day")
                    Spacer(Modifier.height(14.dp))
                    if (days == 1) {
                        HourlyChart(hourly)
                    } else {
                        val minutes = dailyMillis.map { (it / 60_000L).toInt() }
                        val labels = dayList.mapIndexed { index, day ->
                            when {
                                days == 7 -> LocalDate.ofEpochDay(day)
                                    .format(DateTimeFormatter.ofPattern("EE", Locale.US)).take(2)
                                index % 7 == 0 || index == dayList.lastIndex -> "d${index + 1}"
                                else -> ""
                            }
                        }
                        BarChart(values = minutes, labels = labels)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showAdvanced = !showAdvanced }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel("advanced", modifier = Modifier.weight(1f))
                Text(if (showAdvanced) "⌃" else "⌄", color = TextTertiary, style = MaterialTheme.typography.titleMedium)
            }
        }

        if (showAdvanced) {
            if (ownAverage > 0L) {
                item { Spacer(Modifier.height(12.dp)) }
                item {
                    OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                        Column {
                            SectionLabel("compared with you")
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "an average tracked day is ${formatShort(ownAverage)}.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(Voice.noPeerStats, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                    Column {
                        Text(TimeTranslation.oneThing(summary.millis).detail, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        val rate = if (days == 1) TimeTranslation.atThisRate(summary.millis) else null
                        if (rate != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(rate, style = MaterialTheme.typography.bodySmall, color = AccentDim)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                    Column {
                        SectionLabel("what we noticed")
                        Spacer(Modifier.height(10.dp))
                        val lines = buildList {
                            surfaces.firstOrNull()?.let {
                                val platform = PlatformCatalog.byId(it.platformId)?.name ?: it.platformId
                                add("$platform took the biggest share, ${formatShort(it.millis)}.")
                            }
                            if (peakHour != null && days == 1) {
                                add("most of it around ${hourLabel(peakHour)}.")
                            }
                            if (previous.millis > 0L && deltaMinutes != 0) {
                                add(
                                    if (deltaMinutes < 0) "${abs(deltaMinutes)} min less than before."
                                    else "$deltaMinutes min more than before.",
                                )
                            }
                            if (blockedTotal > 0) add("blocked $blockedTotal opens.")
                        }
                        if (lines.isEmpty()) {
                            Text("nothing yet. it fills in as the days do.", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                        } else {
                            lines.forEachIndexed { index, line ->
                                Row {
                                    Text("·", color = Accent, modifier = Modifier.width(14.dp))
                                    Text(line, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                                if (index != lines.lastIndex) Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
            if (reveal != null) {
                item { Spacer(Modifier.height(12.dp)) }
                item {
                    OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                        Column {
                            SectionLabel("how it actually felt")
                            Spacer(Modifier.height(10.dp))
                            Text(reveal, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                    }
                }
            }
            if (experiment != null) {
                item { Spacer(Modifier.height(12.dp)) }
                item {
                    OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                        Column {
                            SectionLabel("one thing to test")
                            Spacer(Modifier.height(10.dp))
                            Text(experiment, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Spacer(Modifier.height(14.dp))
                            PrimaryButton(text = "write it as a plan", onClick = onOpenPlan)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("whole app, today", modifier = Modifier.weight(1f))
                    Text(formatMinutes(usage.totalMinutesToday), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
            }
            item { Spacer(Modifier.height(10.dp)) }
            if (!usage.granted) {
                item {
                    OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                        Column {
                            Text("screen time is off.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Spacer(Modifier.height(12.dp))
                            GhostButton(text = "let it read screen time", onClick = onGrantUsage)
                        }
                    }
                }
            } else {
                item {
                    OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                        Column {
                            usage.apps.forEachIndexed { index, app ->
                                val platform = PlatformCatalog.byId(app.platformId)
                                if (platform != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Monogram(platform, size = 30.dp)
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            platform.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            if (app.minutesToday == 0) "0m" else formatMinutes(app.minutesToday),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                        )
                                    }
                                    if (index != usage.apps.lastIndex) {
                                        Spacer(Modifier.height(12.dp))
                                        Divider()
                                        Spacer(Modifier.height(12.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "read on open. never stored, never sent.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary,
                                modifier = Modifier.clickable(onClick = onRefresh),
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

private fun experimentLine(surfaces: List<com.example.openmind.domain.SurfaceAttention>, peakHour: Int?, exposures: Int): String? {
    val top = surfaces.firstOrNull() ?: return null
    val platform = PlatformCatalog.byId(top.platformId)?.name ?: top.platformId
    val whenPart = if (peakHour != null) "when it's ${hourLabel(peakHour)} and i reach for $platform" else "when i open $platform"
    val thenPart = if (exposures > 40) "phone goes face down for 10 minutes first" else "i say out loud what i actually went looking for"
    return "$whenPart -> $thenPart. one plan, tested for a week."
}

private fun comparison(total: Long, previous: Long, deltaMinutes: Int): String = when {
    total == 0L && previous == 0L -> "nothing yet."
    previous == 0L -> "first tracked stretch."
    deltaMinutes == 0 -> "level with before."
    deltaMinutes < 0 -> "${abs(deltaMinutes)}m less than before."
    else -> "${deltaMinutes}m more than before."
}

private fun formatMinutes(minutes: Int): String =
    if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m"

private fun hourLabel(hour: Int): String {
    val h = if (hour % 12 == 0) 12 else hour % 12
    return "$h${if (hour < 12) "am" else "pm"}"
}

private fun formatLong(millis: Long): String {
    val minutes = millis / 60_000L
    return if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m"
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

package io.github.dharma_labs.openeye.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import io.github.dharma_labs.openeye.data.BlockEvent
import io.github.dharma_labs.openeye.data.PlatformCatalog
import io.github.dharma_labs.openeye.data.Settings
import io.github.dharma_labs.openeye.data.UsageSnapshot
import io.github.dharma_labs.openeye.data.WatchRecord
import io.github.dharma_labs.openeye.domain.AttentionStats
import io.github.dharma_labs.openeye.domain.Coach
import io.github.dharma_labs.openeye.domain.SurfaceAttention
import io.github.dharma_labs.openeye.domain.TimeTranslation
import io.github.dharma_labs.openeye.domain.Voice
import io.github.dharma_labs.openeye.ui.components.BarChart
import io.github.dharma_labs.openeye.ui.components.Divider
import io.github.dharma_labs.openeye.ui.components.DisclosureRow
import io.github.dharma_labs.openeye.ui.components.GhostButton
import io.github.dharma_labs.openeye.ui.components.HourlyChart
import io.github.dharma_labs.openeye.ui.components.IconTile
import io.github.dharma_labs.openeye.ui.components.MiniMetric
import io.github.dharma_labs.openeye.ui.components.Monogram
import io.github.dharma_labs.openeye.ui.components.OMCard
import io.github.dharma_labs.openeye.ui.components.PrimaryButton
import io.github.dharma_labs.openeye.ui.components.Revealed
import io.github.dharma_labs.openeye.ui.components.ScreenHeader
import io.github.dharma_labs.openeye.ui.components.SegmentedControl
import io.github.dharma_labs.openeye.ui.components.SectionLabel
import io.github.dharma_labs.openeye.ui.components.eyePress
import io.github.dharma_labs.openeye.ui.theme.Accent
import io.github.dharma_labs.openeye.ui.theme.AccentDim
import io.github.dharma_labs.openeye.ui.theme.EyeShape
import io.github.dharma_labs.openeye.ui.theme.EyeSpace
import io.github.dharma_labs.openeye.ui.theme.MinTouch
import io.github.dharma_labs.openeye.ui.theme.SurfaceOne
import io.github.dharma_labs.openeye.ui.theme.SurfaceThree
import io.github.dharma_labs.openeye.ui.theme.TextPrimary
import io.github.dharma_labs.openeye.ui.theme.TextSecondary
import io.github.dharma_labs.openeye.ui.theme.TextTertiary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * The numbers page.
 *
 * A person arrives here with a feeling, not a question: "am I doing badly?" So the page leads with one
 * number, never a grade, and puts every judgement behind a disclosure they open on purpose. The
 * breakdown repeats the shape of the today list on purpose - same rows, same bars - because
 * recognition is cheaper than decoding, and a page that looks like the page you already learned is a
 * page you can read at a glance.
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
    onOpenSettings: () -> Unit,
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
        contentPadding = PaddingValues(
            start = EyeSpace.screen,
            top = EyeSpace.lg,
            end = EyeSpace.screen,
            bottom = EyeSpace.xxxl,
        ),
        verticalArrangement = Arrangement.spacedBy(EyeSpace.between),
    ) {
        item {
            ScreenHeader(
                title = "the numbers",
                subtitle = "no grades.",
                trailing = {
                    IconTile(
                        type = "settings",
                        tint = TextSecondary,
                        size = 40.dp,
                        modifier = Modifier.eyePress(onClick = onOpenSettings),
                    )
                },
            )
        }

        item {
            SegmentedControl(
                options = listOf("today", "7 days", "30 days"),
                selectedIndex = range,
                onSelect = { range = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            OMCard(
                modifier = Modifier.fillMaxWidth(),
                padding = PaddingValues(EyeSpace.xl),
                glow = if (summary.millis > 0L) Accent else null,
            ) {
                Column {
                    SectionLabel(if (days == 1) "feed time" else "feed time, ${days} days")
                    Spacer(Modifier.height(EyeSpace.md))
                    Text(
                        formatLong(summary.millis),
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(EyeSpace.xs))
                    Text(
                        comparison(summary.millis, previous.millis, deltaMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (summary.millis <= previous.millis) AccentDim else TextSecondary,
                    )
                }
            }
        }

        item {
            OMCard(
                modifier = Modifier.fillMaxWidth(),
                padding = PaddingValues(horizontal = EyeSpace.lg, vertical = EyeSpace.md),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(EyeSpace.md)) {
                    MiniMetric("${summary.sessions}", "visits", Modifier.weight(1f))
                    MiniMetric(formatShort(summary.longestSessionMs), "longest", Modifier.weight(1f))
                    MiniMetric("${summary.transitions}", "swipes", Modifier.weight(1f))
                    MiniMetric("${summary.exposures}", "clips", Modifier.weight(1f))
                }
            }
        }

        item {
            Column {
                Text(
                    "time measured. clips and swipes estimated.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
                Spacer(Modifier.height(EyeSpace.xl))
                SectionLabel("where it went")
            }
        }
        item { WhereItWent(surfaces) }

        item {
            OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(EyeSpace.lg)) {
                Column {
                    SectionLabel(if (days == 1) "when" else "by day")
                    Spacer(Modifier.height(EyeSpace.md))
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

        item {
            DisclosureRow(
                label = "everything else",
                expanded = showAdvanced,
                onClick = { showAdvanced = !showAdvanced },
                hint = if (showAdvanced) null else "translations, screen time, plans",
            )
        }

        item {
            Revealed(expanded = showAdvanced) {
                Column(verticalArrangement = Arrangement.spacedBy(EyeSpace.between)) {
                    if (ownAverage > 0L) {
                        NoteCard("compared with you", "an average tracked day is ${formatShort(ownAverage)}.", Voice.noPeerStats)
                    }
                    val rate = if (days == 1) TimeTranslation.atThisRate(summary.millis) else null
                    NoteCard(
                        title = "in real time",
                        body = TimeTranslation.oneThing(summary.millis).detail,
                        note = rate,
                    )
                    NoticedCard(
                        surfaces = surfaces,
                        peakHour = if (days == 1) peakHour else null,
                        previous = previous.millis,
                        deltaMinutes = deltaMinutes,
                        blockedTotal = blockedTotal,
                    )
                    if (reveal != null) NoteCard("how it actually felt", reveal)
                    if (experiment != null) {
                        OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(EyeSpace.lg)) {
                            Column {
                                SectionLabel("one thing to test")
                                Spacer(Modifier.height(EyeSpace.md))
                                Text(experiment, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Spacer(Modifier.height(EyeSpace.md))
                                PrimaryButton(text = "write it as a plan", onClick = onOpenPlan)
                            }
                        }
                    }

                    OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(EyeSpace.lg)) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SectionLabel("whole app, today", modifier = Modifier.weight(1f))
                                Text(
                                    formatMinutes(usage.totalMinutesToday),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                )
                            }
                            Spacer(Modifier.height(EyeSpace.md))
                            if (!usage.granted) {
                                Text(
                                    "screen time is off.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                )
                                Spacer(Modifier.height(EyeSpace.md))
                                GhostButton(text = "let it read screen time", onClick = onGrantUsage)
                            } else {
                                usage.apps.forEachIndexed { index, app ->
                                    val platform = PlatformCatalog.byId(app.platformId)
                                    if (platform != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Monogram(platform, size = 30.dp)
                                            Spacer(Modifier.width(EyeSpace.md))
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
                                            Spacer(Modifier.height(EyeSpace.md))
                                            Divider()
                                            Spacer(Modifier.height(EyeSpace.md))
                                        }
                                    }
                                }
                                Spacer(Modifier.height(EyeSpace.md))
                                Text(
                                    "read on open. never stored, never sent.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentDim,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = MinTouch)
                                        .eyePress(onClick = onRefresh),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Same row shape as the today list: mark, name, number, then a bar that does the comparing. */
@Composable
private fun WhereItWent(surfaces: List<SurfaceAttention>) {
    OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(EyeSpace.lg)) {
        Column {
            if (surfaces.isEmpty()) {
                Text(
                    "nothing in this range.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
            } else {
                val shown = surfaces.take(6)
                val max = shown.maxOf { it.millis }.coerceAtLeast(1L)
                shown.forEachIndexed { index, item ->
                    val platform = PlatformCatalog.byId(item.platformId)
                    val surface = PlatformCatalog.surface(item.platformId, item.surfaceId)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (platform != null) Monogram(platform, size = 30.dp)
                        Spacer(Modifier.width(EyeSpace.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    platform?.name ?: item.platformId,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                )
                                Text(
                                    surface?.label ?: item.surfaceId,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary,
                                )
                                Spacer(Modifier.width(EyeSpace.md))
                                Text(
                                    formatShort(item.millis),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                )
                            }
                            Spacer(Modifier.height(EyeSpace.sm))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .background(SurfaceThree, RoundedCornerShape(EyeShape.chip)),
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth((item.millis.toFloat() / max).coerceIn(0f, 1f))
                                        .height(5.dp)
                                        .background(
                                            if (index == 0) Accent else AccentDim.copy(alpha = 0.6f),
                                            RoundedCornerShape(EyeShape.chip),
                                        ),
                                )
                            }
                        }
                    }
                    if (index != shown.lastIndex) Spacer(Modifier.height(EyeSpace.lg))
                }
            }
        }
    }
}

@Composable
private fun NoteCard(title: String, body: String, note: String? = null) {
    OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(EyeSpace.lg)) {
        Column {
            SectionLabel(title)
            Spacer(Modifier.height(EyeSpace.md))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            if (note != null) {
                Spacer(Modifier.height(EyeSpace.sm))
                Text(note, style = MaterialTheme.typography.bodySmall, color = AccentDim)
            }
        }
    }
}

@Composable
private fun NoticedCard(
    surfaces: List<SurfaceAttention>,
    peakHour: Int?,
    previous: Long,
    deltaMinutes: Int,
    blockedTotal: Int,
) {
    val lines = buildList {
        surfaces.firstOrNull()?.let {
            val platform = PlatformCatalog.byId(it.platformId)?.name ?: it.platformId
            add("$platform took the biggest share, ${formatShort(it.millis)}.")
        }
        if (peakHour != null) add("most of it around ${hourLabel(peakHour)}.")
        if (previous > 0L && deltaMinutes != 0) {
            add(
                if (deltaMinutes < 0) "${abs(deltaMinutes)} min less than before."
                else "$deltaMinutes min more than before.",
            )
        }
        if (blockedTotal > 0) add("blocked $blockedTotal opens.")
    }
    OMCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(EyeSpace.lg)) {
        Column {
            SectionLabel("what we noticed")
            Spacer(Modifier.height(EyeSpace.md))
            if (lines.isEmpty()) {
                Text(
                    "nothing yet. it fills in as the days do.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
            } else {
                lines.forEachIndexed { index, line ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(Accent, RoundedCornerShape(100.dp)),
                        )
                        Spacer(Modifier.width(EyeSpace.md))
                        Text(line, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    if (index != lines.lastIndex) Spacer(Modifier.height(EyeSpace.sm))
                }
            }
        }
    }
}

private fun experimentLine(surfaces: List<SurfaceAttention>, peakHour: Int?, exposures: Int): String? {
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

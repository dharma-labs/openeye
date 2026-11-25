package io.github.dharma_labs.openeye.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppUsage(
    val platformId: String,
    val name: String,
    val minutesToday: Int,
    val week: List<Int>,
)

data class UsageSnapshot(
    val granted: Boolean,
    val apps: List<AppUsage> = emptyList(),
    val days: List<LocalDate> = emptyList(),
    val totalMinutesToday: Int = 0,
)

class UsageStatsRepository(private val context: Context) {

    private val zone: ZoneId = ZoneId.systemDefault()

    suspend fun load(): UsageSnapshot = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext UsageSnapshot(false)
        val manager = context.getSystemService(UsageStatsManager::class.java)
            ?: return@withContext UsageSnapshot(false)
        val today = LocalDate.now(zone)
        val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
        val packages = PlatformCatalog.all.map { it.packageName }.toSet()
        val perDay = mutableMapOf<String, MutableList<Int>>()
        PlatformCatalog.all.forEach { platform -> perDay[platform.id] = MutableList(days.size) { 0 } }
        var screenOnToday = 0L

        days.forEachIndexed { index, day ->
            val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                .coerceAtMost(System.currentTimeMillis())
            if (end <= start) return@forEachIndexed
            val timeline = readTimeline(manager, start, end, packages)
            PlatformCatalog.all.forEach { platform ->
                val minutes = ((timeline.perPackage[platform.packageName] ?: 0L) / 60_000L).toInt()
                perDay[platform.id]?.set(index, minutes)
            }
            if (day == today) screenOnToday = timeline.screenOnMillis
        }
        // Hand the figure to the meter the corner timer and the widget read, so the numbers screen
        // and the overlay cannot disagree about the same phone on the same day.
        ScreenTimeMeter.seed(screenOnToday)

        val apps = PlatformCatalog.all.map { platform ->
            val week = perDay[platform.id].orEmpty()
            AppUsage(
                platformId = platform.id,
                name = platform.name,
                minutesToday = week.lastOrNull() ?: 0,
                week = week,
            )
        }
        UsageSnapshot(true, apps, days, (screenOnToday / 60_000L).toInt())
    }

    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        @Suppress("DEPRECATION")
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    companion object {
        /** Whole-device screen time today, in milliseconds. */
        fun screenOnMillisToday(context: Context): Long {
            val manager = context.getSystemService(UsageStatsManager::class.java)
                ?: return 0L
            val start = todayStartMillis()
            return readTimeline(manager, start, System.currentTimeMillis(), emptySet()).screenOnMillis
        }

        /**
         * App-level time for the daily budgets. Event-based like every other figure in the app:
         * the usage buckets a device hands back are not guaranteed to start at local midnight, so
         * reading them can pull part of yesterday into today and fire a limit early.
         */
        fun minutesToday(context: Context, packageName: String): Int {
            val manager = context.getSystemService(UsageStatsManager::class.java) ?: return 0
            val start = todayStartMillis()
            val now = System.currentTimeMillis()
            val millis = readTimeline(manager, start, now, setOf(packageName)).perPackage[packageName] ?: 0L
            return (millis / 60_000L).toInt()
        }

        fun todayStartMillis(): Long =
            LocalDate.now(ZoneId.systemDefault())
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        fun formatMinutes(minutes: Int): String {
            if (minutes < 60) return "${minutes}m"
            return "${minutes / 60}h ${minutes % 60}m"
        }

        fun startOfDay(epochMillis: Long): Long =
            Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
                .toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}

/**
 * One pass over the day's usage events gives both numbers the app shows, which matters: if the
 * per-app rows and the headline came from different queries they could disagree on screen and the
 * app would look like it was inventing one of them.
 */
internal data class UsageTimeline(
    val perPackage: Map<String, Long>,
    val screenOnMillis: Long,
)

internal fun readTimeline(
    manager: UsageStatsManager,
    start: Long,
    end: Long,
    wanted: Set<String>,
): UsageTimeline {
    val events = runCatching { manager.queryEvents(start, end) }.getOrNull()
        ?: return UsageTimeline(emptyMap(), 0L)
    val resumedAt = mutableMapOf<String, Long>()
    val spansByPackage = mutableMapOf<String, MutableList<Pair<Long, Long>>>()
    val foreground = mutableListOf<Pair<Long, Long>>()
    val lit = mutableListOf<Pair<Long, Long>>()
    var litSince = -1L
    var sawScreenEvents = false
    val event = UsageEvents.Event()
    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        val at = event.timeStamp
        when (event.eventType) {
            UsageEvents.Event.SCREEN_INTERACTIVE -> {
                sawScreenEvents = true
                if (litSince < 0) litSince = at
            }
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                sawScreenEvents = true
                if (litSince >= 0) {
                    lit += litSince to at
                    litSince = -1L
                }
            }
            UsageEvents.Event.ACTIVITY_RESUMED -> {
                val pkg = event.packageName ?: continue
                if (resumedAt[pkg] == null) resumedAt[pkg] = at
            }
            UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                val pkg = event.packageName ?: continue
                val from = resumedAt.remove(pkg) ?: continue
                foreground += from to at
                if (pkg in wanted) spansByPackage.getOrPut(pkg) { mutableListOf() } += from to at
            }
        }
    }
    if (litSince >= 0) lit += litSince to end
    resumedAt.forEach { (pkg, from) ->
        foreground += from to end
        if (pkg in wanted) spansByPackage.getOrPut(pkg) { mutableListOf() } += from to end
    }

    val mergedForeground = mergeSpans(foreground)
    // An app in split screen has two overlapping windows of its own, so per-app time is merged the
    // same way or one minute of split viewing counts twice.
    val perPackage = spansByPackage.mapValues { (_, spans) -> mergeSpans(spans).sumOf { it.second - it.first } }
    val screenOn = if (sawScreenEvents) {
        overlapMillis(mergedForeground, mergeSpans(lit))
    } else {
        // Some devices never emit screen events into the usage stream. The merged foreground
        // timeline is the same thing minus the lock screen, which is the honest fallback.
        mergedForeground.sumOf { it.second - it.first }
    }
    return UsageTimeline(perPackage, screenOn.coerceIn(0L, end - start))
}

/** Sorts and joins touching or overlapping spans. Endpoints are exclusive. */
internal fun mergeSpans(spans: List<Pair<Long, Long>>): List<Pair<Long, Long>> {
    if (spans.isEmpty()) return emptyList()
    val sorted = spans.filter { it.second > it.first }.sortedBy { it.first }
    val merged = ArrayList<Pair<Long, Long>>(sorted.size)
    var (from, to) = sorted.firstOrNull() ?: return emptyList()
    for (index in 1 until sorted.size) {
        val next = sorted[index]
        if (next.first <= to) {
            if (next.second > to) to = next.second
        } else {
            merged += from to to
            from = next.first
            to = next.second
        }
    }
    merged += from to to
    return merged
}

/** Milliseconds covered by both timelines, each of which must already be merged. */
internal fun overlapMillis(a: List<Pair<Long, Long>>, b: List<Pair<Long, Long>>): Long {
    var i = 0
    var j = 0
    var total = 0L
    while (i < a.size && j < b.size) {
        val from = maxOf(a[i].first, b[j].first)
        val to = minOf(a[i].second, b[j].second)
        if (to > from) total += to - from
        if (a[i].second <= b[j].second) i++ else j++
    }
    return total
}

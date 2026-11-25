package com.example.openmind.data

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
        val perDay = mutableMapOf<String, MutableList<Int>>()
        PlatformCatalog.all.forEach { platform ->
            perDay[platform.id] = MutableList(days.size) { 0 }
        }

        days.forEachIndexed { index, day ->
            val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                .coerceAtMost(System.currentTimeMillis())
            if (end <= start) return@forEachIndexed
            val totals = foregroundMillis(manager, start, end)
            PlatformCatalog.all.forEach { platform ->
                val minutes = ((totals[platform.packageName] ?: 0L) / 60_000L).toInt()
                perDay[platform.id]?.set(index, minutes)
            }
        }

        val apps = PlatformCatalog.all.map { platform ->
            val week = perDay[platform.id].orEmpty()
            AppUsage(
                platformId = platform.id,
                name = platform.name,
                minutesToday = week.lastOrNull() ?: 0,
                week = week,
            )
        }
        UsageSnapshot(true, apps, days, totalMinutesToday(context))
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

    private fun foregroundMillis(manager: UsageStatsManager, start: Long, end: Long): Map<String, Long> {
        val wanted = PlatformCatalog.all.map { it.packageName }.toSet()
        val totals = mutableMapOf<String, Long>()
        val resumedAt = mutableMapOf<String, Long>()
        val events = runCatching { manager.queryEvents(start, end) }.getOrNull() ?: return totals
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            if (pkg !in wanted) continue
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED, UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (resumedAt[pkg] == null) resumedAt[pkg] = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val from = resumedAt.remove(pkg) ?: continue
                    totals[pkg] = (totals[pkg] ?: 0L) + (event.timeStamp - from).coerceAtLeast(0L)
                }
            }
        }
        resumedAt.forEach { (pkg, from) ->
            totals[pkg] = (totals[pkg] ?: 0L) + (end - from).coerceAtLeast(0L)
        }
        return totals
    }

    companion object {
        /** Whole-device screen time today, for the home-screen widget's headline. */
        fun totalMinutesToday(context: Context): Int {
            val manager = context.getSystemService(UsageStatsManager::class.java) ?: return 0
            val start = todayStartMillis()
            val now = System.currentTimeMillis()
            val stats = runCatching {
                manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, now)
            }.getOrNull() ?: return 0
            val millis = stats.groupBy { it.packageName }.values.sumOf { buckets ->
                buckets.maxOfOrNull { it.totalTimeInForeground } ?: 0L
            }
            return (millis / 60_000L).toInt()
        }

        /** Fast check for app-level budgets, called at most once a minute from the service. */
        fun minutesToday(context: Context, packageName: String): Int {
            val manager = context.getSystemService(UsageStatsManager::class.java) ?: return 0
            val start = todayStartMillis()
            val now = System.currentTimeMillis()
            val stats = runCatching {
                manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, now)
            }.getOrNull() ?: return 0
            val millis = stats.filter { it.packageName == packageName }
                .maxOfOrNull { it.totalTimeInForeground } ?: 0L
            return (millis / 60_000L).toInt()
        }

        fun todayStartMillis(): Long =
            LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        fun formatMinutes(minutes: Int): String {
            if (minutes < 60) return "${minutes}m"
            return "${minutes / 60}h ${minutes % 60}m"
        }

        fun startOfDay(epochMillis: Long): Long =
            Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
                .toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
package io.github.dharma_labs.openeye.domain

import io.github.dharma_labs.openeye.data.WatchRecord

data class AttentionSummary(
    val millis: Long = 0L,
    val exposures: Int = 0,
    val sessions: Int = 0,
    val transitions: Int = 0,
    val longestSessionMs: Long = 0L,
    val touches: Int = 0,
) {
    val averageItemMs: Long
        get() = if (exposures > 0) millis / exposures else 0L

    val swipes: Int get() = transitions
}

data class SurfaceAttention(
    val platformId: String,
    val surfaceId: String,
    val millis: Long,
    val exposures: Int,
    val sessions: Int,
)

/**
 * One definition of "feed time", "estimated exposures" and "transitions" shared by Today and
 * Insights so the two screens can never disagree.
 *
 * - millis: real time in a detected feed surface.
 * - exposures: estimated items encountered (first item + detected paging).
 * - transitions: paging events, i.e. exposures minus sessions. Never labelled unique videos.
 */
object AttentionStats {

    fun summarize(records: List<WatchRecord>, startDay: Long, endDay: Long): AttentionSummary {
        val filtered = records.filter { it.epochDay in startDay..endDay }
        val exposures = filtered.sumOf { it.count }
        val recordedSessions = filtered.sumOf { it.sessions }
        // Records written before v2.0.0 have no session count; approximate one per record with views.
        val sessions = if (recordedSessions == 0) filtered.count { it.count > 0 } else recordedSessions
        return AttentionSummary(
            millis = filtered.sumOf { it.millis },
            exposures = exposures,
            sessions = sessions,
            transitions = (exposures - sessions).coerceAtLeast(0),
            longestSessionMs = filtered.maxOfOrNull { it.longestSessionMs } ?: 0L,
            touches = filtered.sumOf { it.touches },
        )
    }

    fun forDay(records: List<WatchRecord>, day: Long): AttentionSummary =
        summarize(records, day, day)

    /** Milliseconds per local hour for one day. Unknown-hour legacy records are omitted. */
    fun hourly(records: List<WatchRecord>, day: Long): LongArray {
        val buckets = LongArray(24)
        records.forEach { record ->
            if (record.epochDay == day && record.hour in 0..23) {
                buckets[record.hour] += record.millis
            }
        }
        return buckets
    }

    fun bySurface(records: List<WatchRecord>, startDay: Long, endDay: Long): List<SurfaceAttention> =
        records
            .filter { it.epochDay in startDay..endDay }
            .groupBy { it.platformId to it.surfaceId }
            .map { (key, group) ->
                SurfaceAttention(
                    platformId = key.first,
                    surfaceId = key.second,
                    millis = group.sumOf { it.millis },
                    exposures = group.sumOf { it.count },
                    sessions = group.sumOf { it.sessions },
                )
            }
            .sortedByDescending { it.millis }

    fun daily(records: List<WatchRecord>, days: List<Long>): List<Long> =
        days.map { day -> records.filter { it.epochDay == day }.sumOf { it.millis } }
}
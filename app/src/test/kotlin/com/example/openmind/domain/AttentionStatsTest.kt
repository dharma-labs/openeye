package com.example.openmind.domain

import com.example.openmind.data.WatchRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class AttentionStatsTest {

    private val day = 20_000L

    private fun record(
        platform: String = "instagram",
        surface: String = "reels",
        count: Int = 2,
        millis: Long = 60_000L,
        sessions: Int = 1,
        longest: Long = 45_000L,
        hour: Int = 9,
        epochDay: Long = day,
    ) = WatchRecord(
        epochDay = epochDay,
        platformId = platform,
        surfaceId = surface,
        count = count,
        millis = millis,
        sessions = sessions,
        longestSessionMs = longest,
        hour = hour,
    )

    @Test
    fun `summary sums metrics and derives transitions`() {
        val summary = AttentionStats.forDay(
            listOf(record(count = 4, sessions = 2), record(surface = "explore", count = 2, sessions = 1)),
            day,
        )
        assertEquals(120_000L, summary.millis)
        assertEquals(6, summary.exposures)
        assertEquals(3, summary.sessions)
        assertEquals(3, summary.transitions)
    }

    @Test
    fun `longest session is a maximum not a sum`() {
        val summary = AttentionStats.forDay(
            listOf(record(longest = 70_000L), record(surface = "explore", longest = 20_000L)),
            day,
        )
        assertEquals(70_000L, summary.longestSessionMs)
    }

    @Test
    fun `range excludes other days`() {
        val summary = AttentionStats.summarize(
            listOf(record(), record(epochDay = day - 1), record(epochDay = day - 5)),
            day - 1,
            day,
        )
        assertEquals(120_000L, summary.millis)
    }

    @Test
    fun `hourly buckets ignore unknown hours`() {
        val hourly = AttentionStats.hourly(
            listOf(record(hour = 9), record(hour = 9, millis = 30_000L), record(hour = -1)),
            day,
        )
        assertEquals(90_000L, hourly[9])
        assertEquals(0L, hourly.sum() - hourly[9])
    }

    @Test
    fun `surface breakdown sorts by time`() {
        val surfaces = AttentionStats.bySurface(
            listOf(record(millis = 10_000L), record(surface = "explore", millis = 50_000L)),
            day,
            day,
        )
        assertEquals("explore", surfaces.first().surfaceId)
        assertEquals(50_000L, surfaces.first().millis)
    }

    @Test
    fun `daily series preserves requested order`() {
        val values = AttentionStats.daily(
            listOf(record(epochDay = day), record(epochDay = day - 2, millis = 5_000L)),
            listOf(day - 2, day - 1, day),
        )
        assertEquals(listOf(5_000L, 0L, 60_000L), values)
    }
}
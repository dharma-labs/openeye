package com.example.openmind.domain

import com.example.openmind.data.BlockMode
import com.example.openmind.data.BlockReason
import com.example.openmind.data.FocusConfig
import com.example.openmind.data.PlatformConfig
import com.example.openmind.data.ScheduleConfig
import com.example.openmind.data.Settings
import com.example.openmind.data.SurfaceConfig
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    private fun settings(
        platformEnabled: Boolean = true,
        surfaceMode: BlockMode = BlockMode.INTENTIONAL,
        strictMode: Boolean = false,
        master: Boolean = true,
        schedule: ScheduleConfig = ScheduleConfig(),
        focus: FocusConfig = FocusConfig(),
    ) = Settings(
        masterEnabled = master,
        strictMode = strictMode,
        onboardingComplete = true,
        platforms = mapOf(
            "instagram" to PlatformConfig(
                enabled = platformEnabled,
                surfaces = mapOf(
                    "reels" to SurfaceConfig(surfaceMode),
                    "explore" to SurfaceConfig(BlockMode.OFF),
                ),
            ),
        ),
        schedule = schedule,
        focus = focus,
    )

    @Test
    fun `master switch wins over everything`() {
        val decision = RuleEngine.decide(
            "instagram", "reels",
            settings(master = false, surfaceMode = BlockMode.STRICT),
            at(2026, 11, 23, 10, 0), zone,
        )
        assertFalse(decision.block)
    }

    @Test
    fun `disabled platform is allowed`() {
        val decision = RuleEngine.decide(
            "instagram", "reels", settings(platformEnabled = false), at(2026, 11, 23, 10, 0), zone,
        )
        assertFalse(decision.block)
    }

    @Test
    fun `off surface is allowed`() {
        val decision = RuleEngine.decide(
            "instagram", "explore", settings(), at(2026, 11, 23, 10, 0), zone,
        )
        assertFalse(decision.block)
    }

    @Test
    fun `strict surface blocks without bypass`() {
        val decision = RuleEngine.decide(
            "instagram", "reels", settings(surfaceMode = BlockMode.STRICT), at(2026, 11, 23, 10, 0), zone,
        )
        assertTrue(decision.block)
        assertEquals(BlockMode.STRICT, decision.mode)
        assertEquals(BlockReason.SHIELD, decision.reason)
    }

    @Test
    fun `gentle surface blocks with intentional mode`() {
        val decision = RuleEngine.decide(
            "instagram", "reels", settings(), at(2026, 11, 23, 10, 0), zone,
        )
        assertTrue(decision.block)
        assertEquals(BlockMode.INTENTIONAL, decision.mode)
    }

    @Test
    fun `global strict mode upgrades gentle surfaces`() {
        val decision = RuleEngine.decide(
            "instagram", "reels", settings(strictMode = true), at(2026, 11, 23, 10, 0), zone,
        )
        assertEquals(BlockMode.STRICT, decision.mode)
    }

    @Test
    fun `active focus blocks strictly`() {
        val now = at(2026, 11, 23, 10, 0)
        val focus = FocusConfig(active = true, startEpochMs = now - 60_000, endEpochMs = now + 600_000, durationMinutes = 10)
        val decision = RuleEngine.decide("instagram", "reels", settings(focus = focus), now, zone)
        assertTrue(decision.block)
        assertEquals(BlockReason.FOCUS, decision.reason)
        assertEquals(BlockMode.STRICT, decision.mode)
    }

    @Test
    fun `expired focus falls back to surface rule`() {
        val now = at(2026, 11, 23, 10, 0)
        val focus = FocusConfig(active = true, startEpochMs = now - 900_000, endEpochMs = now - 300_000, durationMinutes = 10)
        val decision = RuleEngine.decide("instagram", "reels", settings(focus = focus), now, zone)
        assertEquals(BlockReason.SHIELD, decision.reason)
        assertEquals(BlockMode.INTENTIONAL, decision.mode)
    }

    @Test
    fun `schedule blocks inside its window on an enabled day`() {
        val schedule = ScheduleConfig(enabled = true, days = setOf(1), startMinute = 9 * 60, endMinute = 17 * 60)
        val monday = at(2026, 11, 23, 10, 30)
        val decision = RuleEngine.decide("instagram", "reels", settings(schedule = schedule), monday, zone)
        assertTrue(decision.block)
        assertEquals(BlockReason.SCHEDULE, decision.reason)
        assertEquals(BlockMode.STRICT, decision.mode)
    }

    @Test
    fun `schedule ignores disabled days and outside hours`() {
        val schedule = ScheduleConfig(enabled = true, days = setOf(1), startMinute = 9 * 60, endMinute = 17 * 60)
        val sunday = at(2026, 11, 22, 10, 30)
        assertFalse(RuleEngine.isScheduleActive(settings(schedule = schedule), sunday, zone))
        val mondayEvening = at(2026, 11, 23, 19, 0)
        assertFalse(RuleEngine.isScheduleActive(settings(schedule = schedule), mondayEvening, zone))
    }

    @Test
    fun `overnight schedule wraps to the previous day`() {
        val schedule = ScheduleConfig(enabled = true, days = setOf(1), startMinute = 22 * 60, endMinute = 7 * 60)
        val mondayLate = at(2026, 11, 23, 23, 30)
        val tuesdayEarly = at(2026, 11, 24, 6, 0)
        val mondayEarly = at(2026, 11, 23, 6, 0)
        assertTrue(RuleEngine.isScheduleActive(settings(schedule = schedule), mondayLate, zone))
        assertTrue(RuleEngine.isScheduleActive(settings(schedule = schedule), tuesdayEarly, zone))
        assertFalse(RuleEngine.isScheduleActive(settings(schedule = schedule), mondayEarly, zone))
    }

    @Test
    fun `bypass allows a gentle surface until it expires`() {
        val now = at(2026, 11, 23, 10, 0)
        val allowed = RuleEngine.decide(
            "instagram", "reels", settings(), now, zone, bypassUntilMillis = now + 120_000,
        )
        assertFalse(allowed.block)
        val expired = RuleEngine.decide(
            "instagram", "reels", settings(), now, zone, bypassUntilMillis = now - 1,
        )
        assertTrue(expired.block)
    }

    @Test
    fun `strict mode ignores an existing bypass`() {
        val now = at(2026, 11, 23, 10, 0)
        val decision = RuleEngine.decide(
            "instagram", "reels", settings(strictMode = true), now, zone, bypassUntilMillis = now + 300_000,
        )
        assertTrue(decision.block)
        assertEquals(BlockMode.STRICT, decision.mode)
    }

    @Test
    fun `schedule is not bypassable`() {
        val schedule = ScheduleConfig(enabled = true, days = setOf(1), startMinute = 9 * 60, endMinute = 17 * 60)
        val now = at(2026, 11, 23, 10, 0)
        val decision = RuleEngine.decide(
            "instagram", "reels", settings(schedule = schedule), now, zone, bypassUntilMillis = now + 300_000,
        )
        assertTrue(decision.block)
        assertEquals(BlockReason.SCHEDULE, decision.reason)
    }
}
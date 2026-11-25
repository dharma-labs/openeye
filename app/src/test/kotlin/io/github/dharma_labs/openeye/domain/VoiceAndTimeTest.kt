package io.github.dharma_labs.openeye.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeTranslationTest {

    @Test
    fun hoursTextCoversSecondsMinutesAndHours() {
        assertEquals("0s", TimeTranslation.hoursText(0L))
        assertEquals("45s", TimeTranslation.hoursText(45_000L))
        assertEquals("1m", TimeTranslation.hoursText(90_000L))
        assertEquals("1h 30m", TimeTranslation.hoursText(90L * 60_000L))
    }

    @Test
    fun tinyDaysDoNotSoundDramatic() {
        assertTrue(TimeTranslation.oneThing(20_000L).detail.contains("you looked"))
        assertTrue(TimeTranslation.oneThing(120_000L).detail.contains("kettle"))
        assertEquals(0L, TimeTranslation.minutesOf(45_000L))
        assertEquals(1L, TimeTranslation.minutesOf(60_000L))
    }

    @Test
    fun bigDaysGetTranslatedIntoAFilmOrAWorkday() {
        assertTrue(TimeTranslation.oneThing(150L * 60_000L).detail.contains("film"))
        assertTrue(TimeTranslation.oneThing(500L * 60_000L).detail.contains("workday"))
    }

    @Test
    fun rateRefusesToInflateASmallDay() {
        assertNull(TimeTranslation.atThisRate(60_000L))
        val twoHoursEveryDay = 7_200_000L
        assertTrue(requireNotNull(TimeTranslation.atThisRate(twoHoursEveryDay)).contains("days a year"))
    }

    @Test
    fun clipPaceDescribesPaceNotJudgement() {
        assertEquals("60 clips, about 10s each. you kept going.", TimeTranslation.clipPace(600_000L, 60))
        assertNull(TimeTranslation.clipPace(600_000L, 0))
    }
}

class VoiceTest {

    private val pools = listOf(
        Voice.blockGentle,
        Voice.blockStrict,
        Voice.blockFocus,
        Voice.blockSchedule,
        Voice.blockLimit,
        Voice.reminders,
        Voice.lapse,
        Voice.nudges,
        Voice.nudgesOver,
    )

    @Test
    fun rotatingPoolsNeverRepeatConsecutively() {
        pools.forEach { pool ->
            for (cursor in 0 until pool.size * 2) {
                assertTrue(
                    "pool repeated at cursor $cursor",
                    Voice.pick(pool, cursor) != Voice.pick(pool, cursor + 1),
                )
            }
        }
    }

    @Test
    fun negativeAndOverflowCursorsStayInBounds() {
        val pool = Voice.blockGentle
        listOf(Int.MIN_VALUE, -1, 0, Int.MAX_VALUE).forEach { cursor ->
            assertTrue(pool.contains(Voice.pick(pool, cursor)))
        }
        assertEquals("", Voice.pick(emptyList(), 3))
    }

    @Test
    fun reminderTemplatesCarryExactlyOneSlot() {
        Voice.reminders.forEach { line -> assertTrue(line, line.count { c -> c == '%' } == 1) }
        // underRule lines take (time, limit); any of them may use neither, one or both.
        Voice.underRule.forEach { line ->
            val slots = line.count { c -> c == '%' }
            assertTrue(line, slots <= 2)
        }
    }

    /**
     * The in-feed popup fills (time, clips) or (time, their number). A template with the wrong slot
     * count silently prints a raw "%s" over someone's video, which is the one bug this screen
     * cannot afford.
     */
    @Test
    fun nudgeTemplatesCarryExactlyTwoSlots() {
        (Voice.nudges + Voice.nudgesOver).forEach { line ->
            assertTrue(line, line.count { c -> c == '%' } == 2)
        }
        assertTrue(Voice.nudgeScreenTime.count { c -> c == '%' } == 1)
        val filled = Voice.fill(Voice.pick(Voice.nudges, 0), "24m", "61")
        assertTrue(filled, !filled.contains("%s"))
    }

    @Test
    fun fillSubstitutesInOrderAndToleratesFewerSlots() {
        assertEquals("a 1 b 2", Voice.fill("a %s b %s", "1", "2"))
        assertEquals("only 1", Voice.fill("only %s", "1", "ignored"))
        assertEquals("no slots", Voice.fill("no slots", "1"))
    }

    /** The house voice is a hard constraint, not a preference. This keeps it that way. */
    @Test
    fun poolsAvoidShameClinicalAndHypeLanguage() {
        val banned = listOf(
            "addict", "detox", "junk", "discipline", "failure", "no self-control",
            "you'll thank yourself", "put the phone down", "unlock", "level up",
            "achievement", "!!!", "warning",
        )
        pools.forEach { pool ->
            pool.forEach { line ->
                banned.forEach { word ->
                    assertTrue("$line contains $word", !line.lowercase().contains(word))
                }
                assertTrue("$line is too long", line.split(" ").size <= 22)
                assertTrue("$line shouts", line == line.lowercase())
            }
        }
    }
}

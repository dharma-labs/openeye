package io.github.dharma_labs.openeye.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The arithmetic behind the whole-phone number. It exists because summing per-app foreground time
 * reported 21h on a phone that had been awake for six, and the fix is entirely in here.
 */
class ScreenTimeMathTest {

    private val minute = 60_000L

    private fun at(minutes: Int) = minutes * minute

    @Test
    fun `two apps foreground at once are counted once`() {
        // A handover: the old app has not paused when the new one resumes.
        val spans = listOf(at(0) to at(10), at(8) to at(20))
        assertEquals(at(20), mergeSpans(spans).sumOf { it.second - it.first })
    }

    @Test
    fun `merged total cannot exceed the time available`() {
        val spans = (0 until 100).map { at(0) to at(60) }
        val total = mergeSpans(spans).sumOf { it.second - it.first }
        assertEquals(at(60), total)
        assertEquals(1, mergeSpans(spans).size)
    }

    @Test
    fun `touching spans join into one`() {
        assertEquals(listOf(at(0) to at(5)), mergeSpans(listOf(at(0) to at(3), at(3) to at(5))))
    }

    @Test
    fun `out of order input still merges`() {
        val merged = mergeSpans(listOf(at(10) to at(15), at(0) to at(5), at(3) to at(6)))
        assertEquals(listOf(at(0) to at(6), at(10) to at(15)), merged)
    }

    @Test
    fun `time with the screen dark is not screen time`() {
        val foreground = mergeSpans(listOf(at(0) to at(30), at(40) to at(60)))
        val lit = mergeSpans(listOf(at(5) to at(20), at(45) to at(60)))
        // 15 min inside the first lit window, 15 inside the second, the 10 dark minutes excluded.
        assertEquals(at(30), overlapMillis(foreground, lit))
    }

    @Test
    fun `a span that only touches the edge of a lit window adds nothing`() {
        val foreground = listOf(at(10) to at(20))
        val lit = listOf(at(20) to at(30))
        assertEquals(0L, overlapMillis(foreground, lit))
    }

    @Test
    fun `empty input is zero and not a crash`() {
        assertEquals(emptyList<Pair<Long, Long>>(), mergeSpans(emptyList()))
        assertEquals(0L, overlapMillis(emptyList(), listOf(at(0) to at(5))))
        assertEquals(0L, mergeSpans(listOf(at(5) to at(5))).sumOf { it.second - it.first })
    }
}

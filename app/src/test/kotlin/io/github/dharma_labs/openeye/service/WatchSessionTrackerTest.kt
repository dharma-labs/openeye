package io.github.dharma_labs.openeye.service

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The arithmetic behind the in-feed counter. Continuous watching is the product promise, so these
 * guard the three ways it used to break: a transient interruption restarting the readout from zero,
 * the interrupted seconds being counted anyway, and one scroll session logging itself as several.
 */
class WatchSessionTrackerTest {

    private class Recorder {
        val slices = mutableListOf<WatchSlice>()
        fun write(slice: WatchSlice) {
            slices += slice
        }

        val sessions: Int get() = slices.sumOf { it.sessions }
        val millis: Long get() = slices.sumOf { it.millis }
        val count: Int get() = slices.sumOf { it.count }
    }

    private fun trackerWith(recorder: Recorder) = WatchSessionTracker(recorder::write)

    @Test
    fun `a brief interruption continues the session instead of restarting it`() {
        val recorder = Recorder()
        val tracker = trackerWith(recorder)
        tracker.enter("youtube", "shorts", now = 0L)
        repeat(10) { tracker.tick(now = (it + 1) * 1_000L) }
        assertEquals(10_000L, tracker.snapshot().millis)

        tracker.leave(now = 10_000L)
        // A volume panel comes up for two seconds, then the feed is in front again.
        tracker.enter("youtube", "shorts", now = 12_000L)
        assertEquals(10_000L, tracker.snapshot().millis)

        tracker.tick(now = 13_000L)
        assertEquals(11_000L, tracker.snapshot().millis)
        tracker.leave(now = 16_000L)

        // Two seconds of interruption are still missing from the total: only watched time counts.
        assertEquals(14_000L, recorder.millis)
        assertEquals(1, recorder.sessions)
    }

    @Test
    fun `swipes keep accumulating across an interruption`() {
        val recorder = Recorder()
        val tracker = trackerWith(recorder)
        tracker.enter("instagram", "reels", now = 0L)
        repeat(5) { tracker.tick(now = (it + 1) * 1_000L) }
        tracker.noteScroll(now = 5_000L)
        val before = tracker.snapshot().count
        tracker.leave(now = 6_000L)
        tracker.enter("instagram", "reels", now = 7_000L)
        assertEquals(before, tracker.snapshot().count)
        tracker.noteScroll(now = 9_000L)
        assertEquals(before + 1, tracker.snapshot().count)
    }

    @Test
    fun `a session ended for good starts a new one`() {
        val recorder = Recorder()
        val tracker = trackerWith(recorder)
        tracker.enter("youtube", "shorts", now = 0L)
        repeat(5) { tracker.tick(now = (it + 1) * 1_000L) }
        tracker.leave(now = 5_000L)

        val later = 5_000L + WatchSessionTracker.RESUME_WINDOW_MS + 1_000L
        tracker.enter("youtube", "shorts", now = later)
        assertEquals(0L, tracker.snapshot().millis)
        tracker.leave(now = later + 1_000L)
        assertEquals(2, recorder.sessions)
    }

    @Test
    fun `a different feed never resumes onto the previous one`() {
        val recorder = Recorder()
        val tracker = trackerWith(recorder)
        tracker.enter("instagram", "reels", now = 0L)
        repeat(5) { tracker.tick(now = (it + 1) * 1_000L) }
        tracker.leave(now = 5_000L)
        tracker.enter("youtube", "shorts", now = 5_500L)
        assertEquals(0L, tracker.snapshot().millis)
        assertEquals(1, recorder.sessions)
    }

    @Test
    fun `a feed opened and closed instantly invents no clips`() {
        val recorder = Recorder()
        val tracker = trackerWith(recorder)
        tracker.enter("instagram", "reels", now = 0L)
        tracker.tick(now = 1_000L)
        assertEquals(0, tracker.snapshot().count)
        tracker.leave(now = 1_500L)
        assertEquals(0, recorder.count)
        // The glance was real time in the feed, so returning inside the window continues it.
        tracker.enter("instagram", "reels", now = 2_000L)
        assertEquals(1_500L, tracker.snapshot().millis)
        assertEquals(0, tracker.snapshot().count)
    }

    @Test
    fun `switching the shield off forgets the held session`() {
        val recorder = Recorder()
        val tracker = trackerWith(recorder)
        tracker.enter("youtube", "shorts", now = 0L)
        repeat(5) { tracker.tick(now = (it + 1) * 1_000L) }
        tracker.leave(now = 5_000L)
        tracker.clearHold()
        tracker.enter("youtube", "shorts", now = 6_000L)
        assertEquals(0L, tracker.snapshot().millis)
        tracker.leave(now = 7_000L)
        assertEquals(2, recorder.sessions)
    }
}

package io.github.dharma_labs.openeye.service

import android.os.SystemClock
import android.util.Log
import io.github.dharma_labs.openeye.BuildConfig
import java.time.LocalTime
import kotlin.math.abs

data class WatchSnapshot(
    val surfaceId: String?,
    val count: Int,
    val millis: Long,
    val touches: Int,
    val swipes: Int,
    val currentItemMillis: Long,
)

/** One slice of measured feed time, handed straight to storage. */
data class WatchSlice(
    val platformId: String,
    val surfaceId: String,
    val count: Int,
    val millis: Long,
    val sessions: Int,
    val longestSessionMs: Long,
    val touches: Int,
    val hour: Int,
)

/**
 * Counts time inside Reels/Shorts and estimates how many items were watched.
 *
 * The estimate is deliberately conservative: entering a surface counts the first item, and each
 * paging swipe afterwards counts one more. Auto-advancing players that emit no accessibility
 * evidence are not counted, so the counter under-reports rather than inventing views.
 *
 * Time is measured, never derived from a clip count. The tracker writes through a lambda instead of
 * holding a repository so the session arithmetic can be tested off-device.
 */
class WatchSessionTracker(private val write: (WatchSlice) -> Unit) {

    private var platformId: String? = null
    private var surfaceId: String? = null

    private var sessionCount = 0
    private var sessionMillis = 0L

    private var unflushedCount = 0
    private var unflushedMillis = 0L
    private var unflushedSessions = 0
    private var unflushedTouches = 0
    private var sessionTouches = 0
    private var lastItemAt = 0L
    private var lastTouchAt = 0L
    private var firstViewCounted = false

    private var lastTickAt = 0L
    private var lastFlushAt = 0L
    private var lastCountAt = 0L

    /**
     * The session that just closed. A volume key, a notification glance or an app update that
     * renamed a class used to end the session outright, so the readout restarted from zero in the
     * middle of continuous watching. Held open for a moment lets it continue instead.
     */
    private var holdPlatform: String? = null
    private var holdSurface: String? = null
    private var holdCount = 0
    private var holdMillis = 0L
    private var holdTouches = 0
    private var holdFirstView = false
    private var holdLastCountAt = 0L
    private var holdLastItemAt = 0L
    private var holdUntil = 0L

    val active: Boolean get() = platformId != null

    fun enter(platform: String, surface: String, now: Long = SystemClock.elapsedRealtime()) {
        if (platformId == platform && surfaceId == surface) {
            tick(now)
            return
        }
        flush(now)
        val resumed = holdPlatform == platform && holdSurface == surface && now <= holdUntil
        reset()
        platformId = platform
        surfaceId = surface
        if (resumed) {
            // The gap is excluded because lastTickAt starts from now: only the seconds actually
            // spent on the feed are added, while the numbers continue from where they stopped.
            val resumedCount = holdCount
            val resumedMillis = holdMillis
            val resumedTouches = holdTouches
            val resumedFirstView = holdFirstView
            val resumedLastCountAt = holdLastCountAt
            val resumedLastItemAt = holdLastItemAt
            clearHold()
            sessionCount = resumedCount
            sessionMillis = resumedMillis
            sessionTouches = resumedTouches
            firstViewCounted = resumedFirstView
            lastCountAt = resumedLastCountAt
            lastItemAt = resumedLastItemAt
        } else {
            // Do not count a view until the feed has actually been on screen for a moment.
            sessionCount = 0
            unflushedCount = 0
            firstViewCounted = false
            unflushedSessions = 1
            // The first swipe can arrive immediately; do not gate it behind the entry count.
            lastCountAt = now - SCROLL_COOLDOWN_MS
            lastItemAt = now
        }
        lastTickAt = now
        lastFlushAt = now
    }

    fun tick(now: Long = SystemClock.elapsedRealtime()) {
        if (platformId == null) return
        val delta = (now - lastTickAt).coerceIn(0L, MAX_TICK_GAP_MS)
        sessionMillis += delta
        unflushedMillis += delta
        lastTickAt = now
        if (!firstViewCounted && sessionMillis >= FIRST_VIEW_DELAY_MS) {
            firstViewCounted = true
            sessionCount = 1
            unflushedCount += 1
            lastItemAt = now
        }
        if (now - lastFlushAt >= FLUSH_INTERVAL_MS) flush(now)
    }

    fun noteView(deltaY: Int, fromIndex: Int, toIndex: Int, now: Long = SystemClock.elapsedRealtime()) {
        if (platformId == null) return
        val paged = fromIndex >= 0 && toIndex >= 0 && fromIndex != toIndex
        val swiped = abs(deltaY) >= SWIPE_THRESHOLD
        if (!paged && !swiped) return
        recordTransition(now, COUNT_COOLDOWN_MS, "index")
    }

    fun noteTouch(now: Long = SystemClock.elapsedRealtime()) {
        if (platformId == null) return
        if (now - lastTouchAt < TOUCH_COOLDOWN_MS) return
        lastTouchAt = now
        sessionTouches++
        unflushedTouches++
        tick(now)
    }

    /** Any scroll event in a feed is treated as a paging attempt.
     *  Many apps emit zero deltas and -1 indices, so the old evidence check missed real swipes.
     */
    fun noteScroll(now: Long = SystemClock.elapsedRealtime()) {
        if (platformId == null) return
        recordTransition(now, SCROLL_COOLDOWN_MS, "scroll")
    }

    /** Content changed enough to look like a new item; slower gate to avoid double counting. */
    fun noteSignature(now: Long = SystemClock.elapsedRealtime()) {
        if (platformId == null) return
        recordTransition(now, SIGNATURE_COOLDOWN_MS, "content")
    }

    private fun recordTransition(now: Long, minGapMs: Long, source: String) {
        if (now - lastCountAt < minGapMs) return
        lastCountAt = now
        lastItemAt = now
        if (!firstViewCounted) {
            firstViewCounted = true
            // First item plus the item just reached.
            sessionCount += 2
            unflushedCount += 2
        } else {
            sessionCount++
            unflushedCount++
        }
        if (BuildConfig.DEBUG) Log.d("OpeneyeCount", "source=$source surface=$surfaceId count=$sessionCount")
        tick(now)
    }

    fun snapshot(): WatchSnapshot = WatchSnapshot(
        surfaceId = surfaceId,
        count = sessionCount,
        millis = sessionMillis,
        touches = sessionTouches,
        swipes = (sessionCount - 1).coerceAtLeast(0),
        currentItemMillis = if (platformId == null) 0L else (SystemClock.elapsedRealtime() - lastItemAt).coerceAtLeast(0L),
    )

    /** Minutes the current session has been on screen, or the held one if nothing is active. */
    fun sessionMillis(): Long = if (platformId != null) sessionMillis else holdMillis

    /** True while a closed session can still be continued. */
    fun holdOpen(now: Long = SystemClock.elapsedRealtime()): Boolean =
        platformId == null && holdUntil != 0L && now <= holdUntil

    fun pendingCount(): Int = unflushedCount

    fun pendingMillis(): Long = unflushedMillis

    fun flush(now: Long = SystemClock.elapsedRealtime()) {
        val platform = platformId ?: return
        val surface = surfaceId ?: return
        if (unflushedCount > 0 || unflushedMillis > 0 || unflushedSessions > 0 || unflushedTouches > 0) {
            write(
                WatchSlice(
                    platformId = platform,
                    surfaceId = surface,
                    count = unflushedCount,
                    millis = unflushedMillis,
                    sessions = unflushedSessions,
                    longestSessionMs = sessionMillis,
                    touches = unflushedTouches,
                    hour = LocalTime.now().hour,
                ),
            )
            unflushedCount = 0
            unflushedMillis = 0
            unflushedSessions = 0
            unflushedTouches = 0
        }
        lastFlushAt = now
    }

    fun leave(now: Long = SystemClock.elapsedRealtime()) {
        if (platformId == null) return
        tick(now)
        flush(now)
        holdSession(now)
        reset()
    }

    private fun holdSession(now: Long) {
        // Only a session with measured time can continue, so a tap that opens nothing cannot
        // chain onto the next one.
        if (sessionMillis <= 0L) {
            clearHold()
            return
        }
        holdPlatform = platformId
        holdSurface = surfaceId
        holdCount = sessionCount
        holdMillis = sessionMillis
        holdTouches = sessionTouches
        holdFirstView = firstViewCounted
        holdLastCountAt = lastCountAt
        holdLastItemAt = lastItemAt
        holdUntil = now + RESUME_WINDOW_MS
    }

    /** Forget the held session: used when the shield is switched off or the day rolls over. */
    fun clearHold() {
        holdPlatform = null
        holdSurface = null
        holdCount = 0
        holdMillis = 0L
        holdTouches = 0
        holdFirstView = false
        holdLastCountAt = 0L
        holdLastItemAt = 0L
        holdUntil = 0L
    }

    private fun reset() {
        platformId = null
        surfaceId = null
        sessionCount = 0
        sessionMillis = 0L
        unflushedCount = 0
        unflushedMillis = 0L
        unflushedSessions = 0
        unflushedTouches = 0
        sessionTouches = 0
        lastTouchAt = 0L
        lastTickAt = 0L
        lastFlushAt = 0L
    }

    companion object {
        private const val SWIPE_THRESHOLD = 140
        private const val COUNT_COOLDOWN_MS = 1_100L
        private const val FLUSH_INTERVAL_MS = 60_000L
        private const val MAX_TICK_GAP_MS = 5_000L
        private const val FIRST_VIEW_DELAY_MS = 2_000L
        private const val TOUCH_COOLDOWN_MS = 250L
        private const val SCROLL_COOLDOWN_MS = 1_500L
        private const val SIGNATURE_COOLDOWN_MS = 3_000L
        const val RESUME_WINDOW_MS = 8_000L
    }
}

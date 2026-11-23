package com.example.openmind.service

import android.os.SystemClock
import android.util.Log
import com.example.openmind.BuildConfig
import com.example.openmind.data.AppRepository
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

/**
 * Counts time inside Reels/Shorts and estimates how many items were watched.
 *
 * The estimate is deliberately conservative: entering a surface counts the first item, and each
 * paging swipe afterwards counts one more. Auto-advancing players that emit no accessibility
 * scroll are not counted, so the counter under-reports rather than inventing views.
 */
class WatchSessionTracker(private val repository: AppRepository) {

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

    val active: Boolean get() = platformId != null

    fun enter(platform: String, surface: String, now: Long = SystemClock.elapsedRealtime()) {
        if (platformId == platform && surfaceId == surface) {
            tick(now)
            return
        }
        flush(now)
        reset()
        platformId = platform
        surfaceId = surface
        // Do not count a view until the feed has actually been on screen for a moment.
        sessionCount = 0
        unflushedCount = 0
        firstViewCounted = false
        unflushedSessions = 1
        lastTickAt = now
        lastFlushAt = now
        // The first swipe can arrive immediately; do not gate it behind the entry count.
        lastCountAt = now - SCROLL_COOLDOWN_MS
        lastItemAt = now
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

    fun pendingCount(): Int = unflushedCount

    fun pendingMillis(): Long = unflushedMillis

    fun flush(now: Long = SystemClock.elapsedRealtime()) {
        val platform = platformId ?: return
        val surface = surfaceId ?: return
        if (unflushedCount > 0 || unflushedMillis > 0 || unflushedSessions > 0 || unflushedTouches > 0) {
            repository.addWatch(
                platformId = platform,
                surfaceId = surface,
                count = unflushedCount,
                millis = unflushedMillis,
                sessions = unflushedSessions,
                longestSessionMs = sessionMillis,
                touches = unflushedTouches,
                hour = LocalTime.now().hour,
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
        reset()
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
        lastItemAt = 0L
        lastTouchAt = 0L
        lastTickAt = 0L
        lastFlushAt = 0L
        lastCountAt = 0L
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
    }
}
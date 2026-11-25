package com.example.openmind.data

import android.content.Context
import android.os.SystemClock
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The one number for "how long this phone has been on today", shared by the corner timer, the
 * widget and the numbers screen so the three cannot disagree with each other.
 *
 * The past comes from the usage events, which are authoritative but cost a query. The present comes
 * from the wall clock between the screen going on and off, which costs nothing at all. Every few
 * minutes the two are compared and the event figure wins when it is ahead, which is what makes a
 * service restart, a killed process or a midnight rollover self-correcting instead of a hole in the
 * number.
 *
 * Nothing here blocks. The query runs on an IO worker and the caller gets whatever is already
 * counted, or -1 until the first one lands.
 */
object ScreenTimeMeter {

    private const val RESEED_MS = 5 * 60_000L

    /** Returned when this process has not measured today yet. Not the same as zero minutes. */
    const val UNKNOWN = -1L

    private val lock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var day = -1L

    /** Screen-on milliseconds counted up to [baseAt]. */
    private var baseMillis = 0L
    private var baseAt = 0L

    /** elapsedRealtime of the session the screen is in right now, or 0 when it is dark. */
    private var onSinceElapsed = 0L
    private var seededAt = 0L
    private var seeding = false

    fun millis(context: Context): Long = synchronized(lock) {
        val today = LocalDate.now().toEpochDay()
        val now = System.currentTimeMillis()
        if (today != day) {
            day = today
            baseMillis = 0L
            baseAt = UsageStatsRepository.todayStartMillis()
            seededAt = 0L
        }
        if (seededAt == 0L || now - seededAt >= RESEED_MS) startReseed(context, now)
        if (seededAt == 0L) return UNKNOWN
        (baseMillis + liveMillis(now)).coerceAtMost(now - UsageStatsRepository.todayStartMillis())
    }

    fun minutes(context: Context): Int {
        val millis = millis(context)
        return if (millis < 0) -1 else (millis / 60_000L).toInt()
    }

    /** Fold the session that just ended into the counted total. Called from the screen-off event. */
    fun onScreenOff() = synchronized(lock) {
        val now = System.currentTimeMillis()
        baseMillis += liveMillis(now)
        baseAt = now
        onSinceElapsed = 0L
    }

    fun onScreenOn() = synchronized(lock) {
        onSinceElapsed = SystemClock.elapsedRealtime()
    }

    /**
     * Adopt a figure someone else read out of the events. Behind is left alone: the counted total
     * only ever moves forward within a day, so a number on screen cannot shrink while you watch it.
     */
    fun seed(millis: Long) = synchronized(lock) {
        if (millis < 0L) return
        val now = System.currentTimeMillis()
        if (LocalDate.now().toEpochDay() != day) {
            day = LocalDate.now().toEpochDay()
            baseMillis = 0L
            seededAt = 0L
        }
        val live = liveMillis(now)
        if (millis > baseMillis + live) {
            baseMillis = (millis - live).coerceAtLeast(0L)
            baseAt = now
        }
        seededAt = now
    }

    private fun startReseed(context: Context, now: Long) {
        if (seeding) return
        seeding = true
        scope.launch {
            val read = UsageStatsRepository.screenOnMillisToday(context)
            seed(read)
            synchronized(lock) { seeding = false }
        }
    }

    private fun liveMillis(now: Long): Long {
        if (onSinceElapsed <= 0L) return 0L
        val sessionStartWall = now - (SystemClock.elapsedRealtime() - onSinceElapsed)
        return (now - maxOf(baseAt, sessionStartWall)).coerceAtLeast(0L)
    }
}

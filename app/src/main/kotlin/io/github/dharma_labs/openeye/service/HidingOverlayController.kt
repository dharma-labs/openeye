package io.github.dharma_labs.openeye.service

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.util.Log
import android.view.WindowManager

/** One opaque, touch-blocking rectangle in screen coordinates. */
data class HideTarget(
    val key: String,
    val bounds: Rect,
    val color: Int,
)

/**
 * Draws and maintains TYPE_ACCESSIBILITY_OVERLAY windows above Instagram/YouTube.
 *
 * Only the accessibility service may create this window type, which keeps the app free of
 * the broad "draw over other apps" permission. The controller diffs targets on every pass so
 * scrolling moves the cover instead of recreating it.
 *
 * Everything here is written to fail safe: a cover that is wrong must never be able to take a
 * screen away from its owner. Each window is capped in area and count, and a WindowManager that
 * refuses our windows is left alone for a while instead of being asked again on every event.
 */
class HidingOverlayController(
    private val service: AccessibilityService,
    private val windowManager: WindowManager,
) {

    private val views = mutableMapOf<String, View>()
    private var lastTargets: List<HideTarget> = emptyList()
    private var failures = 0
    private var retryAfter = 0L

    fun sync(targets: List<HideTarget>) {
        val now = SystemClock.elapsedRealtime()
        if (retryAfter != 0L && now < retryAfter) return
        if (targets == lastTargets) return
        val wanted = targets.map { it.key }.toSet()

        views.keys.toList().filter { it !in wanted }.forEach(::remove)

        var failed = false
        targets.forEach { target ->
            val existing = views[target.key]
            if (existing == null) {
                if (!add(target)) failed = true
            } else if (!update(target, existing)) {
                failed = true
            }
        }
        if (failed) {
            failures++
            if (failures >= CONSECUTIVE_FAILURES_BEFORE_BACKOFF) {
                retryAfter = now + BACKOFF_MS
                failures = 0
                clear()
                Log.w("OpeneyeOverlay", "window manager refused $CONSECUTIVE_FAILURES_BEFORE_BACKOFF covers in a row; resting ${BACKOFF_MS / 1000}s")
            }
        } else {
            failures = 0
        }
        // If a window could not be added or moved, retry on the next event instead of caching it.
        lastTargets = if (failed) emptyList() else targets
    }

    fun clear() {
        views.keys.toList().forEach(::remove)
        lastTargets = emptyList()
    }

    private fun add(target: HideTarget): Boolean {
        if (views.size >= MAX_WINDOWS) return false
        val safe = bounded(target.bounds) ?: return false
        val view = View(service).apply {
            setBackgroundColor(target.color)
            isClickable = true
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setOnTouchListener { _, _ -> true } // swallow every touch on the hidden element
        }
        return runCatching { windowManager.addView(view, paramsFor(target, safe)) }
            .onSuccess { views[target.key] = view }
            .onFailure { Log.w("OpeneyeOverlay", "Unable to add overlay for ${target.key}", it) }
            .isSuccess
    }

    /**
     * The largest cover we are willing to paint, whatever the node tree asked for.
     *
     * A mis-detected node can hand back a container that is really the whole page. A cover that
     * big makes the app look frozen, and on a phone that is indistinguishable from a broken
     * handset - so it is refused outright rather than drawn smaller and guessed at.
     */
    private fun bounded(bounds: Rect): Rect? {
        val metrics = service.resources.displayMetrics
        val screen = metrics.widthPixels.toLong() * metrics.heightPixels
        val area = bounds.width().toLong() * bounds.height().toLong()
        if (area <= 0 || area > screen * MAX_SCREEN_FRACTION) return null
        if (bounds.height() > metrics.heightPixels * MAX_HEIGHT_FRACTION) return null
        if (bounds.width() > metrics.widthPixels * MAX_WIDTH_FRACTION) return null
        return Rect(bounds).apply {
            left = bounds.left.coerceIn(0, metrics.widthPixels)
            top = bounds.top.coerceIn(0, metrics.heightPixels)
            right = bounds.right.coerceIn(0, metrics.widthPixels)
            bottom = bounds.bottom.coerceIn(0, metrics.heightPixels)
        }.takeIf { it.width() > 0 && it.height() > 0 }
    }

    private fun paramsFor(target: HideTarget, bounds: Rect): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            bounds.width().coerceAtLeast(1),
            bounds.height().coerceAtLeast(1),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bounds.left
            y = bounds.top
            title = "Openeye hidden element"
        }

    private fun update(target: HideTarget, view: View): Boolean {
        val safe = bounded(target.bounds) ?: return false
        return runCatching { windowManager.updateViewLayout(view, paramsFor(target, safe)) }
            .onFailure { Log.w("OpeneyeOverlay", "Unable to move overlay for ${target.key}", it) }
            .isSuccess
    }

    private fun remove(key: String) {
        val view = views.remove(key) ?: return
        runCatching { windowManager.removeView(view) }
    }

    private companion object {
        const val MAX_WINDOWS = 4
        const val MAX_SCREEN_FRACTION = 0.18f
        const val MAX_HEIGHT_FRACTION = 0.30f
        const val MAX_WIDTH_FRACTION = 0.95f
        const val CONSECUTIVE_FAILURES_BEFORE_BACKOFF = 3
        const val BACKOFF_MS = 20_000L
    }
}
package com.example.openmind.service

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.graphics.Rect
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
 */
class HidingOverlayController(
    private val service: AccessibilityService,
    private val windowManager: WindowManager,
) {

    private val views = mutableMapOf<String, View>()
    private var lastTargets: List<HideTarget> = emptyList()

    fun sync(targets: List<HideTarget>) {
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
        // If a window could not be added or moved, retry on the next event instead of caching it.
        lastTargets = if (failed) emptyList() else targets
    }

    fun clear() {
        views.keys.toList().forEach(::remove)
        lastTargets = emptyList()
    }

    private fun add(target: HideTarget): Boolean {
        val view = View(service).apply {
            setBackgroundColor(target.color)
            isClickable = true
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setOnTouchListener { _, _ -> true } // swallow every touch on the hidden element
        }
        return runCatching { windowManager.addView(view, paramsFor(target)) }
            .onSuccess { views[target.key] = view }
            .onFailure { Log.w("OpeneyeOverlay", "Unable to add overlay for ${target.key}", it) }
            .isSuccess
    }

    private fun paramsFor(target: HideTarget): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            target.bounds.width().coerceAtLeast(1),
            target.bounds.height().coerceAtLeast(1),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = target.bounds.left
            y = target.bounds.top
            title = "Openeye hidden element"
        }

    private fun update(target: HideTarget, view: View): Boolean =
        runCatching { windowManager.updateViewLayout(view, paramsFor(target)) }
            .onFailure { Log.w("OpeneyeOverlay", "Unable to move overlay for ${target.key}", it) }
            .isSuccess

    private fun remove(key: String) {
        val view = views.remove(key) ?: return
        runCatching { windowManager.removeView(view) }
    }
}
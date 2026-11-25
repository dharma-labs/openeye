package io.github.dharma_labs.openeye.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

/**
 * A small, self-dismissing reminder that floats over an open Reels or Shorts feed.
 *
 * Same window type as the counter and for the same reason: `TYPE_ACCESSIBILITY_OVERLAY` plus
 * `FLAG_NOT_TOUCHABLE` means it can appear inside another app without stealing a single tap, and
 * without the SYSTEM_ALERT_WINDOW permission that a Play-declared wellbeing app should not need.
 * It fades out on its own, is never modal, and disappears with the app it was drawn over.
 */
class WatchNudgeController(
    private val service: AccessibilityService,
    private val windowManager: WindowManager,
) {

    private val handler = Handler(Looper.getMainLooper())
    private var view: TextView? = null
    private var dismissal: Runnable? = null

    val isShowing: Boolean get() = view != null

    fun show(text: String, visibleMillis: Long = SHOW_MS) {
        removeNow()
        val label = TextView(service).apply {
            this.text = text
            setTextColor(Color.parseColor("#F2F5EF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            letterSpacing = 0.02f
            setLineSpacing(0f, 1.25f)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            includeFontPadding = false
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(Color.parseColor("#F20A0A0B"))
                setStroke(dp(1), Color.parseColor("#66C9F24D"))
            }
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
            isClickable = false
            isFocusable = false
            alpha = 0f
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(140)
            title = "Openeye reminder"
        }
        runCatching { windowManager.addView(label, params) }
            .onSuccess {
                view = label
                label.animate().alpha(1f).setDuration(180L).start()
                val finish = Runnable { hide() }
                dismissal = finish
                handler.postDelayed(finish, visibleMillis)
            }
    }

    /** Fades out, then detaches. Calling [show] again replaces whatever is on screen. */
    fun hide() {
        val current = view ?: return
        dismissal?.let { handler.removeCallbacks(it) }
        dismissal = null
        view = null
        current.animate().alpha(0f).setDuration(260L).withEndAction { detach(current) }.start()
    }

    fun removeNow() {
        val current = view ?: return
        dismissal?.let { handler.removeCallbacks(it) }
        dismissal = null
        view = null
        current.animate().cancel()
        detach(current)
    }

    private fun detach(label: TextView) {
        runCatching { windowManager.removeView(label) }
    }

    private fun dp(value: Int): Int = (value * service.resources.displayMetrics.density).toInt()

    private companion object {
        const val SHOW_MS = 6_000L
    }
}

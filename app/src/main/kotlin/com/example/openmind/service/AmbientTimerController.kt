package com.example.openmind.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

/**
 * The whole-phone screen-time figure, kept in the top-left corner while the phone is awake.
 *
 * It is deliberately the opposite of the feed counter in every way that matters: one line, no
 * dismiss, no tap target, and it is taken off the moment a feed or one of our own screens is in
 * front, because two counters on one screen is noise. The window is FLAG_NOT_TOUCHABLE, so it can
 * never swallow a tap, and it is sized by its own text, so it cannot cover anything.
 */
class AmbientTimerController(
    private val service: AccessibilityService,
    private val windowManager: WindowManager,
) {

    private var view: TextView? = null
    private var lastText: String? = null

    val isShowing: Boolean get() = view != null

    fun show(text: String) {
        val existing = view
        if (existing != null) {
            if (text == lastText) return
            existing.text = text
            lastText = text
            return
        }
        val textView = TextView(service).apply {
            setTextColor(Color.parseColor("#8FBFA2"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            letterSpacing = 0.06f
            maxLines = 1
            setPadding(dp(7), dp(4), dp(7), dp(4))
            includeFontPadding = false
            background = GradientDrawable().apply {
                cornerRadius = dp(9).toFloat()
                setColor(Color.parseColor("#990A0A0B"))
            }
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
            isClickable = false
            isFocusable = false
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(10)
            y = statusBarHeight() + dp(4)
            title = "Openeye screen timer"
        }
        runCatching { windowManager.addView(textView, params) }.onSuccess {
            view = textView
            textView.text = text
            lastText = text
        }
    }

    fun hide() {
        val current = view ?: return
        view = null
        lastText = null
        runCatching { windowManager.removeView(current) }
    }

    private fun statusBarHeight(): Int {
        val id = service.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) service.resources.getDimensionPixelSize(id) else dp(24)
    }

    private fun dp(value: Int): Int = (value * service.resources.displayMetrics.density).toInt()
}

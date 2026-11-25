package io.github.dharma_labs.openeye.service

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
 * Small, non-dismissible counter in the top corner of Instagram/YouTube.
 *
 * The window is FLAG_NOT_TOUCHABLE: it never blocks a tap, and it has no close affordance.
 * It disappears the moment another app takes the foreground or the service stops.
 */
class WatchHudController(
    private val service: AccessibilityService,
    private val windowManager: WindowManager,
) {

    private var view: TextView? = null
    private var lastText: String? = null

    fun show(text: String) {
        val existing = view
        if (existing != null) {
            // Ticked once a second, but most seconds the readout has not changed. Setting text
            // anyway costs a measure and layout pass on a window drawn over another app.
            if (text == lastText) return
            existing.text = text
            lastText = text
            return
        }
        val textView = TextView(service).apply {
            setTextColor(Color.parseColor("#C9F24D"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            letterSpacing = 0.06f
            setLineSpacing(0f, 1.2f)
            setPadding(dp(9), dp(6), dp(9), dp(6))
            includeFontPadding = false
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor("#E60A0A0B"))
                setStroke(dp(1), Color.parseColor("#33FFFFFF"))
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
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(12)
            y = statusBarHeight() + dp(6)
            title = "Openeye watch counter"
        }
        runCatching { windowManager.addView(textView, params) }
            .onSuccess {
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

    private fun dp(value: Int): Int =
        (value * service.resources.displayMetrics.density).toInt()
}
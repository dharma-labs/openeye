package com.example.openmind.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.openmind.domain.AttentionSummary
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Renders the weekly summary as a branded PNG and shares it through the system sheet. */
object SummaryShare {

    private const val WIDTH = 1080
    private const val HEIGHT = 1350
    private const val BG = "#08090B"
    private const val CARD = "#14171B"
    private const val ACCENT = "#C9F24D"
    private const val TEXT = "#F4F6F8"
    private const val SECONDARY = "#9299A2"
    private const val MUTED = "#646B74"

    fun shareAsync(context: Context, summary: AttentionSummary, bars: List<Long>, barsLabel: String) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val file = runCatching { render(appContext, summary, bars, barsLabel) }.getOrNull()
            if (file == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "couldn't make that image", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            withContext(Dispatchers.Main) {
                runCatching {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file,
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TEXT, if (barsLabel.contains("hour", ignoreCase = true)) "today, paid to the feed" else "this week, paid to the feed")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        clipData = ClipData.newRawUri("Openeye summary", uri)
                    }
                    context.startActivity(Intent.createChooser(intent, "share the card"))
                }
            }
        }
    }

    private fun render(context: Context, summary: AttentionSummary, bars: List<Long>, barsLabel: String): File {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.shader = LinearGradient(
            0f, 0f, 0f, HEIGHT.toFloat(),
            Color.parseColor(BG),
            Color.parseColor("#11160A"),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), paint)
        paint.shader = null

        paint.shader = RadialGradient(
            WIDTH * 0.82f, 140f, 620f,
            intArrayOf(Color.parseColor("#33C9F24D"), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(WIDTH * 0.82f, 140f, 620f, paint)
        paint.shader = null

        val card = RectF(56f, 56f, WIDTH - 56f, HEIGHT - 56f)
        paint.color = Color.parseColor(CARD)
        canvas.drawRoundRect(card, 56f, 56f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.parseColor("#2B3320")
        canvas.drawRoundRect(card, 56f, 56f, paint)
        paint.style = Paint.Style.FILL

        drawEye(canvas, 120f, 150f, 58f)

        canvas.drawText("OPENEYE", 190f, 168f, textPaint(30f, ACCENT, medium = true, tracking = 0.12f))
        val cardTitle = if (barsLabel.contains("hour", ignoreCase = true)) "one day" else "one week"
        canvas.drawText(cardTitle, 120f, 300f, textPaint(30f, MUTED, medium = true, tracking = 0.16f))
        canvas.drawText(short(summary.millis), 116f, 490f, textPaint(146f, TEXT, medium = true))
        canvas.drawText("reels + shorts", 120f, 548f, textPaint(34f, SECONDARY))

        paint.color = Color.parseColor("#2B3320")
        canvas.drawRect(120f, 600f, WIDTH - 120f, 602f, paint)

        val columns = listOf(
            summary.exposures.toString() to "clips",
            short(summary.averageItemMs) to "avg clip",
            summary.swipes.toString() to "swipes",
            summary.touches.toString() to "taps",
        )
        columns.forEachIndexed { index, (value, label) ->
            val x = 120f + index * 225f
            canvas.drawText(value, x, 690f, textPaint(58f, TEXT, medium = true))
            canvas.drawText(label, x, 732f, textPaint(24f, MUTED, tracking = 0.1f))
        }

        val shownBars = bars.takeLast(24)
        val max = (shownBars.maxOrNull() ?: 0L).coerceAtLeast(1L)
        val left = 120f
        val right = WIDTH - 120f
        val count = shownBars.size.coerceAtLeast(1)
        val step = (right - left) / count
        val barWidth = step * 0.55f
        val baseline = 1040f
        val chartTop = 800f
        shownBars.forEachIndexed { index, value ->
            val fraction = value.toFloat() / max.toFloat()
            val height = (baseline - chartTop) * (0.12f + 0.88f * fraction)
            val x = left + index * step + (step - barWidth) / 2f
            paint.color = Color.parseColor(if (value > 0L) ACCENT else "#2B3320")
            canvas.drawRoundRect(
                RectF(x, baseline - height, x + barWidth, baseline),
                (barWidth / 2f).coerceAtMost(10f),
                (barWidth / 2f).coerceAtMost(10f),
                paint,
            )
        }
        canvas.drawText(barsLabel, 120f, 1110f, textPaint(24f, MUTED, tracking = 0.12f))
        canvas.drawText(
            "stays on this phone. no account, no cloud.",
            120f,
            1238f,
            textPaint(26f, SECONDARY),
        )
        canvas.drawText("this is where the time went.", 120f, 1180f, textPaint(34f, ACCENT, medium = true))

        val dir = File(context.cacheDir, "summary").apply { mkdirs() }
        val file = File(dir, "openeye-weekly.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        return file
    }

    private fun drawEye(canvas: Canvas, x: Float, y: Float, size: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(ACCENT)
            style = Paint.Style.FILL
        }
        // Same negative-space mark as the launcher icon: one lens, the exit cut out of it.
        val s = size / 0.88f
        val mark = Path().apply {
            moveTo(x - 0.44f * s, y)
            cubicTo(x - 0.25f * s, y - 0.31f * s, x + 0.25f * s, y - 0.31f * s, x + 0.44f * s, y)
            cubicTo(x + 0.25f * s, y + 0.31f * s, x - 0.25f * s, y + 0.31f * s, x - 0.44f * s, y)
            close()
            moveTo(x, y - 0.185f * s)
            lineTo(x + 0.145f * s, y)
            lineTo(x + 0.075f * s, y)
            lineTo(x + 0.075f * s, y + 0.165f * s)
            lineTo(x - 0.075f * s, y + 0.165f * s)
            lineTo(x - 0.075f * s, y)
            lineTo(x - 0.145f * s, y)
            close()
            fillType = Path.FillType.EVEN_ODD
        }
        canvas.drawPath(mark, paint)
    }

    private fun textPaint(
        size: Float,
        color: String,
        medium: Boolean = false,
        tracking: Float = 0f,
    ): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = Color.parseColor(color)
        textSize = size
        typeface = Typeface.create(
            if (medium) "sans-serif-medium" else "sans-serif",
            Typeface.NORMAL,
        )
        if (tracking > 0f) {
            letterSpacing = tracking
        }
    }

    private fun short(millis: Long): String {
        val minutes = millis / 60_000L
        return when {
            millis <= 0L -> "0m"
            minutes < 1 -> "<1m"
            minutes < 60 -> "${minutes}m"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
    }
}
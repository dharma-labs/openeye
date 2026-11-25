package com.example.openmind.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.example.openmind.MainActivity
import com.example.openmind.R
import com.example.openmind.data.AppRepository
import com.example.openmind.data.ScreenTimeMeter
import com.example.openmind.data.UsageStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The day's feed time on the home screen, so the number is seen without opening the app.
 *
 * Everything drawn here is already measured and stored locally: the same records the Today screen
 * reads. Nothing is invented - the clip count keeps its tilde because it is an estimate. The widget
 * is the only place Openeye reaches outside its own window, and it costs one short read per refresh.
 */
class OpeneyeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        OpeneyeWidget.draw(context, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        options: Bundle,
    ) {
        OpeneyeWidget.updateAll(context, force = true)
    }

    override fun onEnabled(context: Context) {
        OpeneyeWidget.updateAll(context, force = true)
    }
}

object OpeneyeWidget {

    private const val REQUEST_OPEN = 41
    private const val UPDATE_INTERVAL_MS = 60_000L

    @Volatile
    private var lastUpdateAt = 0L

    fun component(context: Context) = ComponentName(context, OpeneyeWidgetProvider::class.java)

    /**
     * Called from the app and from the counting service. Throttled so a busy feed cannot make the
     * launcher redraw every second: the widget shows minutes, and a minute is all it can show.
     */
    fun updateAll(context: Context, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastUpdateAt < UPDATE_INTERVAL_MS) return
        lastUpdateAt = now
        val app = context.applicationContext
        val ids = runCatching {
            AppWidgetManager.getInstance(app).getAppWidgetIds(component(app))
        }.getOrNull()
        if (ids == null || ids.isEmpty()) return
        draw(app, ids)
    }

    fun draw(context: Context, ids: IntArray) {
        val app = context.applicationContext
        // DataStore is async and the receiver must not block the main thread to read it.
        CoroutineScope(Dispatchers.IO).launch {
            val views = runCatching { buildViews(app) }.getOrNull() ?: return@launch
            runCatching { AppWidgetManager.getInstance(app).updateAppWidget(ids, views) }
        }
    }

    private suspend fun buildViews(context: Context): RemoteViews? {
        val repo = AppRepository.get(context)
        if (!awaitReady(repo)) return null
        val records = repo.todayWatch()
        val settings = repo.settings.value
        val millis = records.sumOf { it.millis }
        val clips = records.sumOf { it.count }
        val visits = records.sumOf { it.sessions }
        val minutes = (millis / 60_000L).toInt()

        val screenMinutes = if (UsageStatsRepository(context).hasPermission()) {
            // The same meter the corner timer reads, so the widget and the timer cannot show two
            // different numbers for the same phone on the same day. A widget added straight after a
            // boot finds the meter cold, so it reads the events itself rather than paint a blank.
            val metered = ScreenTimeMeter.minutes(context)
            if (metered >= 0) metered
            else (UsageStatsRepository.screenOnMillisToday(context) / 60_000L).toInt()
        } else {
            0
        }

        val views = RemoteViews(context.packageName, R.layout.openeye_widget)
        views.setTextViewText(
            R.id.widget_label,
            if (screenMinutes > 0) "openeye · ${UsageStatsRepository.formatMinutes(screenMinutes)} on screen"
            else "openeye",
        )
        views.setTextViewText(R.id.widget_big, UsageStatsRepository.formatMinutes(minutes))
        views.setTextViewText(
            R.id.widget_sub,
            when {
                clips == 0 && visits == 0 -> "no feeds today"
                // A first flush can land under a minute. "0m" next to a clip count would read as a
                // mistake, so the line says what it actually is.
                minutes == 0 -> "under a minute · ~$clips clips"
                clips > 0 -> "~$clips clips · $visits visits"
                else -> "$visits visits"
            },
        )

        val rule = settings.contract.stopRuleMinutes
        if (rule > 0) {
            views.setTextViewText(
                R.id.widget_goal,
                when {
                    minutes < rule -> "${rule - minutes}m left of your ${rule}m"
                    minutes == rule -> "right on your ${rule}m"
                    else -> "past your ${rule}m by ${minutes - rule}m"
                },
            )
            views.setViewVisibility(R.id.widget_goal, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_goal, View.GONE)
        }

        val open = PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        views.setOnClickPendingIntent(R.id.widget_root, open)
        return views
    }

    private suspend fun awaitReady(repo: AppRepository): Boolean {
        if (repo.ready.value) return true
        return withTimeoutOrNull(READY_TIMEOUT_MS) { repo.ready.first { it } } == true
    }

    private const val READY_TIMEOUT_MS = 2_000L
}

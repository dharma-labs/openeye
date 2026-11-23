package com.example.openmind.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import com.example.openmind.MainActivity
import com.example.openmind.R
import com.example.openmind.data.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusTimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ticker: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            AppRepository.get(this).stopFocus()
            stopSelf()
            return START_NOT_STICKY
        }
        if (!AppRepository.get(this).settings.value.focus.active) {
            stopSelf()
            return START_NOT_STICKY
        }
        startInForeground(notification(remainingMinutes(), 0))
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                val focus = AppRepository.get(this@FocusTimerService).settings.value.focus
                val remaining = focus.endEpochMs - System.currentTimeMillis()
                if (!focus.active || remaining <= 0L) {
                    AppRepository.get(this@FocusTimerService).stopFocus()
                    break
                }
                val total = (focus.endEpochMs - focus.startEpochMs).coerceAtLeast(1L)
                val progress = (100 - (remaining * 100 / total)).toInt().coerceIn(0, 100)
                val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        this@FocusTimerService,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                if (canNotify) {
                    runCatching {
                        NotificationManagerCompat.from(this@FocusTimerService)
                            .notify(NOTIFICATION_ID, notification((remaining / 60_000L).toInt() + 1, progress))
                    }
                }
                delay(1_000L)
            }
            stopSelf()
        }
        return START_STICKY
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private fun startInForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun remainingMinutes(): Int {
        val focus = AppRepository.get(this).settings.value.focus
        val remaining = focus.endEpochMs - System.currentTimeMillis()
        return (remaining / 60_000L).toInt().coerceAtLeast(0)
    }

    private fun notification(minutesLeft: Int, progress: Int): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, FocusTimerService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle("Focus session")
            .setContentText("$minutesLeft min left · shields are strict")
            .setContentIntent(open)
            .addAction(0, "End session", stop)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setProgress(100, progress, false)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.focus_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.focus_channel_description)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        ticker?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "focus_sessions"
        private const val NOTIFICATION_ID = 42
        const val ACTION_STOP = "com.example.openmind.action.STOP_FOCUS"

        fun start(context: Context) {
            val intent = Intent(context, FocusTimerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FocusTimerService::class.java))
        }
    }
}
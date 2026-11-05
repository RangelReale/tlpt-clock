package com.rangelreale.tlptclock.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.rangelreale.tlptclock.MainActivity
import com.rangelreale.tlptclock.R
import com.rangelreale.tlptclock.clock.ClockController
import com.rangelreale.tlptclock.clock.LevelEndAlarm
import com.rangelreale.tlptclock.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Rings the level-end alarm in a short foreground service, so that it can ring for longer than a
 * broadcast receiver may run. Its notification has a Stop button. When the rings end or the user
 * stops them, the service tells [ClockController], which starts a level that waits for the alarm.
 */
class AlarmService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ringing: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            finish()
        } else {
            ring(intent?.getIntExtra(EXTRA_LEVEL, 0) ?: 0)
        }
        return START_NOT_STICKY
    }

    private fun ring(level: Int) {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification(level),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
            } else {
                0
            },
        )
        // Keeps the processor awake between rings while the screen is off.
        if (wakeLock == null) {
            wakeLock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "tlptclock:alarm")
                .apply { acquire(WAKE_LOCK_MS) }
        }
        ringing?.cancel()
        ringing = scope.launch {
            AlarmPlayer.play(this@AlarmService, SettingsStore.get(this@AlarmService).settings.value)
            finish()
        }
    }

    private fun finish() {
        ringing?.cancel()
        ringing = null
        ClockController.get(this).alarmStopped()
        releaseWakeLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** A short service must stop when the system says its time is up. */
    override fun onTimeout(startId: Int) = finish()

    override fun onDestroy() {
        scope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun notification(level: Int): android.app.Notification {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.alarm_channel),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { setSound(null, null) }
        manager.createNotificationChannel(channel)

        val stop = PendingIntent.getService(
            this,
            0,
            stopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val waits = SettingsStore.get(this).settings.value.waitForAlarm
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.blinds_up, level))
            .setContentText(
                getString(if (waits) R.string.alarm_text_waits else R.string.alarm_text_runs),
            )
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(open)
            .addAction(0, getString(R.string.stop_alarm), stop)
            .build()
    }

    companion object {
        private const val ACTION_STOP = "com.rangelreale.tlptclock.STOP_ALARM"
        private const val EXTRA_LEVEL = "level"
        private const val CHANNEL_ID = "alarm"
        private const val NOTIFICATION_ID = 2
        private const val WAKE_LOCK_MS = 3 * 60_000L

        fun ringIntent(context: Context, level: Int): Intent =
            Intent(context, AlarmService::class.java).putExtra(EXTRA_LEVEL, level)

        fun stopIntent(context: Context): Intent =
            Intent(context, AlarmService::class.java).setAction(ACTION_STOP)
    }
}

/** Rings the alarm through [AlarmService]. */
class ServiceAlarm(context: Context) : LevelEndAlarm {
    private val context = context.applicationContext

    override fun ring(level: Int): Boolean = try {
        ContextCompat.startForegroundService(context, AlarmService.ringIntent(context, level))
        true
    } catch (e: RuntimeException) {
        // Android can refuse to start a foreground service from the background.
        Log.w("ServiceAlarm", "Cannot start the alarm", e)
        false
    }

    override fun stop() {
        try {
            context.startService(AlarmService.stopIntent(context))
        } catch (e: RuntimeException) {
            Log.w("ServiceAlarm", "Cannot stop the alarm", e)
        }
    }
}

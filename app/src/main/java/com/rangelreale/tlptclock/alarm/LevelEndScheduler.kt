package com.rangelreale.tlptclock.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.rangelreale.tlptclock.MainActivity
import com.rangelreale.tlptclock.R
import com.rangelreale.tlptclock.clock.ClockScheduler
import com.rangelreale.tlptclock.clock.ClockState

/**
 * Makes the clock work while the app is in the background.
 *
 * While the clock runs, a system alarm wakes [LevelEndReceiver] when the level ends, and a
 * notification counts down to that moment. The app process does not need to stay alive between
 * level ends. While the clock is paused, there is no alarm and no notification.
 */
class LevelEndScheduler(context: Context) : ClockScheduler {
    private val context = context.applicationContext
    private val alarms = this.context.getSystemService(AlarmManager::class.java)
    private val notifications = this.context.getSystemService(NotificationManager::class.java)

    override fun update(state: ClockState) {
        val endsAt = state.endsAtMs
        if (endsAt == null) {
            alarms.cancel(receiverIntent())
            notifications.cancel(NOTIFICATION_ID)
            return
        }
        // The clock uses the time since boot. The alarm clock API needs wall-clock time.
        val wallEndsAt = System.currentTimeMillis() + (endsAt - SystemClock.elapsedRealtime())
        scheduleAlarm(wallEndsAt)
        showNotification(state.level, endsAt)
    }

    private fun scheduleAlarm(wallEndsAt: Long) {
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        if (canExact) {
            // An alarm clock alarm is exact and is not delayed by battery saving.
            alarms.setAlarmClock(
                AlarmManager.AlarmClockInfo(wallEndsAt, activityIntent()),
                receiverIntent(),
            )
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, wallEndsAt, receiverIntent())
        }
    }

    /** @param endsAt The level end in time since boot, which the countdown view uses. */
    private fun showNotification(level: Int, endsAt: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel()
        val levelText = context.getString(R.string.level, level)
        // A custom view, because the standard countdown in the header is small and easy to miss.
        val view = RemoteViews(context.packageName, R.layout.notification_clock).apply {
            setTextViewText(R.id.level, levelText)
            setChronometer(R.id.time_left, endsAt, null, true)
            setChronometerCountDown(R.id.time_left, true)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(levelText)
            .setContentText(context.getString(R.string.notification_text))
            .setCustomContentView(view)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setShowWhen(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(activityIntent())
            .build()
        notifications.notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        notifications.createNotificationChannel(channel)
    }

    private fun receiverIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, LevelEndReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun activityIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val CHANNEL_ID = "clock"
        const val NOTIFICATION_ID = 1
    }
}

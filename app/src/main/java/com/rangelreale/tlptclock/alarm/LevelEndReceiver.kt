package com.rangelreale.tlptclock.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.rangelreale.tlptclock.clock.ClockState
import com.rangelreale.tlptclock.clock.PreferencesClockStore
import com.rangelreale.tlptclock.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Runs when a level ends, whether the app is on screen, in the background or not running.
 *
 * It moves the stored clock to the next level, plays the alarm and schedules the next level end.
 * The screen sees the new level on its next tick, because the clock state only depends on time.
 */
class LevelEndReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                handle(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handle(context: Context) {
        val store = PreferencesClockStore(context)
        val saved = store.load() ?: return
        val now = SystemClock.elapsedRealtime()
        val advanced = saved.advanceForAlarm(now)
        if (advanced.level != saved.level) store.save(advanced, now)
        LevelEndScheduler(context).update(advanced)
        if (advanced.level == saved.level) return
        AlarmPlayer.play(context, SettingsStore.get(context).settings.value)
    }
}

/**
 * Moves to the next level when the alarm fires up to [ALARM_EARLY_MS] before the level end. The
 * alarm uses wall-clock time and the clock uses time since boot, so the two can differ slightly.
 */
internal fun ClockState.advanceForAlarm(nowMs: Long): ClockState {
    val ends = endsAtMs ?: return this
    return advance(if (ends - nowMs in 1..ALARM_EARLY_MS) ends else nowMs)
}

internal const val ALARM_EARLY_MS = 2_000L

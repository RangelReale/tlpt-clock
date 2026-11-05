package com.rangelreale.tlptclock.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.rangelreale.tlptclock.clock.ClockController
import com.rangelreale.tlptclock.clock.ClockState

/**
 * Runs when a level ends, whether the app is on screen, in the background or not running.
 *
 * It moves the clock to the next level, which starts the alarm, and schedules the next level end.
 * If the screen already noticed the level end, nothing more happens.
 */
class LevelEndReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val controller = ClockController.get(context)
        val now = SystemClock.elapsedRealtime()
        if (!controller.tick(controller.state.value.alarmTickTime(now))) controller.refresh()
    }
}

/**
 * The time at which to check the clock when the alarm fires. An alarm up to [ALARM_EARLY_MS] before
 * the level end counts as the level end, because the alarm uses wall-clock time and the clock uses
 * time since boot, and the two can differ slightly.
 */
internal fun ClockState.alarmTickTime(nowMs: Long): Long {
    val ends = endsAtMs ?: return nowMs
    return if (ends - nowMs in 1..ALARM_EARLY_MS) ends else nowMs
}

internal const val ALARM_EARLY_MS = 2_000L

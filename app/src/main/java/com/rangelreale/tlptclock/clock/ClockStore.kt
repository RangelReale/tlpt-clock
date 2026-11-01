package com.rangelreale.tlptclock.clock

import android.content.Context
import android.provider.Settings
import androidx.core.content.edit

/** Keeps the clock across app restarts. */
interface ClockStore {
    fun load(): ClockState?
    fun save(state: ClockState, nowMs: Long)
}

/**
 * Stores the clock in shared preferences.
 *
 * A running clock's end time uses the time since boot, which restarts at zero when the device
 * restarts. After a restart the clock therefore loads as paused, with the time it had when saved.
 */
class PreferencesClockStore(context: Context) : ClockStore {
    private val resolver = context.applicationContext.contentResolver
    private val prefs = context.applicationContext
        .getSharedPreferences("clock", Context.MODE_PRIVATE)

    override fun load(): ClockState? {
        if (!prefs.contains(KEY_LENGTH)) return null
        val saved = ClockState(
            levelLengthMs = prefs.getLong(KEY_LENGTH, 0),
            level = prefs.getInt(KEY_LEVEL, 1),
            remainingMs = prefs.getLong(KEY_REMAINING, 0),
            endsAtMs = if (prefs.contains(KEY_ENDS_AT)) prefs.getLong(KEY_ENDS_AT, 0) else null,
        )
        return if (prefs.getInt(KEY_BOOT, -1) == bootCount()) saved else saved.copy(endsAtMs = null)
    }

    override fun save(state: ClockState, nowMs: Long) {
        prefs.edit {
            putLong(KEY_LENGTH, state.levelLengthMs)
            putInt(KEY_LEVEL, state.level)
            putLong(KEY_REMAINING, state.remainingAt(nowMs))
            putInt(KEY_BOOT, bootCount())
            if (state.endsAtMs != null) putLong(KEY_ENDS_AT, state.endsAtMs) else remove(KEY_ENDS_AT)
        }
    }

    private fun bootCount(): Int =
        Settings.Global.getInt(resolver, Settings.Global.BOOT_COUNT, 0)

    private companion object {
        const val KEY_LENGTH = "level_length_ms"
        const val KEY_LEVEL = "level"
        const val KEY_REMAINING = "remaining_ms"
        const val KEY_ENDS_AT = "ends_at_ms"
        const val KEY_BOOT = "boot_count"
    }
}

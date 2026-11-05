package com.rangelreale.tlptclock.clock

import android.content.Context
import android.os.SystemClock
import com.rangelreale.tlptclock.alarm.LevelEndScheduler
import com.rangelreale.tlptclock.alarm.ServiceAlarm
import com.rangelreale.tlptclock.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Keeps the system alarm and notification in step with the clock while the app is in the background. */
fun interface ClockScheduler {
    fun update(state: ClockState)
}

/** Rings and silences the level-end alarm. */
interface LevelEndAlarm {
    /** Starts ringing. Returns false if the alarm could not start. */
    fun ring(level: Int): Boolean

    fun stop()
}

/**
 * The one clock of the app process.
 *
 * The screen, the alarm receiver and the alarm service all change the clock through this class, so
 * that a level end is noticed once and rings the alarm once, whoever notices it first.
 */
class ClockController(
    private val store: ClockStore,
    private val now: () -> Long,
    private val scheduler: ClockScheduler = ClockScheduler {},
    private val alarm: LevelEndAlarm? = null,
    private val waitForAlarm: () -> Boolean = { false },
) {
    private val _state = MutableStateFlow(store.load() ?: ClockState())
    val state: StateFlow<ClockState> = _state.asStateFlow()

    private val _ringing = MutableStateFlow(false)

    /** True from a level end until the alarm stops. */
    val ringing: StateFlow<Boolean> = _ringing.asStateFlow()

    /** When the last level ended by reaching zero, or null after any command. */
    @Volatile
    var levelEndedAtMs: Long? = null
        private set

    init {
        // The system drops alarms when the app is updated or force-stopped.
        scheduler.update(_state.value)
    }

    /**
     * Moves to the next level if the current one has ended at [atMs], and rings the alarm.
     * Returns true if a level ended.
     */
    @Synchronized
    fun tick(atMs: Long = now()): Boolean {
        val old = _state.value
        val new = old.advance(atMs, waitForAlarm())
        if (new == old) return false
        levelEndedAtMs = now()
        commit(new, atMs)
        _ringing.value = true
        if (alarm?.ring(new.level) != true) alarmStopped()
        return true
    }

    /** Applies a command from the user. */
    @Synchronized
    fun update(change: (ClockState, Long) -> ClockState) {
        tick()
        val t = now()
        levelEndedAtMs = null
        commit(change(_state.value, t), t)
    }

    /** Silences the alarm. If the next level waits for the alarm, it starts now. */
    fun stopAlarm() {
        if (_ringing.value) alarm?.stop()
        alarmStopped()
    }

    /** Called when the alarm has stopped, by itself or by the user. */
    @Synchronized
    fun alarmStopped() {
        _ringing.value = false
        val current = _state.value
        if (current.waitingForAlarm) {
            val t = now()
            commit(current.start(t), t)
        }
    }

    /** Updates the system alarm and notification, for example after notifications are allowed. */
    fun refresh() = scheduler.update(_state.value)

    private fun commit(new: ClockState, t: Long) {
        _state.value = new
        store.save(new, t)
        scheduler.update(new)
    }

    companion object {
        @Volatile
        private var instance: ClockController? = null

        fun get(context: Context): ClockController = instance ?: synchronized(this) {
            instance ?: run {
                val app = context.applicationContext
                val settings = SettingsStore.get(app)
                ClockController(
                    store = PreferencesClockStore(app),
                    now = SystemClock::elapsedRealtime,
                    scheduler = LevelEndScheduler(app),
                    alarm = ServiceAlarm(app),
                    waitForAlarm = { settings.settings.value.waitForAlarm },
                )
            }.also { instance = it }
        }
    }
}

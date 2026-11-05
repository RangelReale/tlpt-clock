package com.rangelreale.tlptclock.clock

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

/** What the screen shows. It changes at most once per second while the clock runs. */
data class ClockUiState(
    val level: Int,
    val lengthMinutes: Int,
    val secondsLeft: Long,
    val isRunning: Boolean,
    val isAtLevelStart: Boolean,
    val blindsUp: Boolean,
    val isAlarmRinging: Boolean = false,
)

/** The commands the screen can give. */
interface ClockActions {
    fun startOrPause()
    fun stopAlarm()
    fun resetLevel()
    fun nextLevel()
    fun previousLevel()
    fun newGame()
    fun setLengthMinutes(minutes: Int)
}

class ClockViewModel(
    private val controller: ClockController,
    private val now: () -> Long,
) : ViewModel(), ClockActions {

    private val _ui = MutableStateFlow(currentUi())
    val ui: StateFlow<ClockUiState> = _ui.asStateFlow()

    init {
        // The alarm service and receiver can change the clock while the screen shows it.
        viewModelScope.launch {
            combine(controller.state, controller.ringing) { _, _ -> }.collect { publish() }
        }
    }

    /**
     * Keeps the screen current while the caller stays active. It only loops while the clock runs or
     * shows "Blinds up".
     */
    suspend fun runTicker() {
        tick()
        ui.distinctUntilChangedBy { it.isRunning || it.blindsUp }.collectLatest { state ->
            while (state.isRunning || state.blindsUp) {
                tick()
                delay(TICK_MS)
            }
        }
    }

    fun tick() {
        controller.tick()
        publish()
    }

    /** Updates the alarm and notification, for example after the user allows notifications. */
    fun refreshBackground() = controller.refresh()

    override fun startOrPause() = command { state, t ->
        if (state.isRunning) state.pause(t) else state.start(t)
    }

    override fun stopAlarm() {
        controller.stopAlarm()
        publish()
    }

    override fun resetLevel() = command { state, t -> state.resetLevel(t) }
    override fun nextLevel() = command { state, t -> state.nextLevel(t) }
    override fun previousLevel() = command { state, t -> state.previousLevel(t) }
    override fun newGame() = command { state, _ -> state.newGame() }

    override fun setLengthMinutes(minutes: Int) = command { state, t ->
        val allowed = minutes.coerceIn(MIN_LENGTH_MINUTES, MAX_LENGTH_MINUTES)
        state.withLevelLength(allowed * MS_PER_MINUTE, t)
    }

    private fun command(change: (ClockState, Long) -> ClockState) {
        controller.update(change)
        publish()
    }

    private fun publish() {
        _ui.value = currentUi()
    }

    private fun currentUi(): ClockUiState {
        val t = now()
        val clock = controller.state.value
        val ringing = controller.ringing.value
        val left = clock.remainingAt(t)
        val endedAt = controller.levelEndedAtMs
        return ClockUiState(
            level = clock.level,
            lengthMinutes = (clock.levelLengthMs / MS_PER_MINUTE).toInt(),
            secondsLeft = wholeSecondsLeft(left),
            isRunning = clock.isRunning,
            isAtLevelStart = !clock.isRunning && left == clock.levelLengthMs,
            blindsUp = ringing || clock.waitingForAlarm ||
                (endedAt != null && t - endedAt < BLINDS_UP_MS),
            isAlarmRinging = ringing,
        )
    }

    companion object {
        private const val TICK_MS = 100L
        private const val BLINDS_UP_MS = 10_000L

        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ClockViewModel(ClockController.get(context), SystemClock::elapsedRealtime)
            }
        }
    }
}

package com.rangelreale.tlptclock.clock

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** What the screen shows. It changes at most once per second while the clock runs. */
data class ClockUiState(
    val level: Int,
    val lengthMinutes: Int,
    val secondsLeft: Long,
    val isRunning: Boolean,
    val isAtLevelStart: Boolean,
    val blindsUp: Boolean,
)

/** The commands the screen can give. */
interface ClockActions {
    fun startOrPause()
    fun resetLevel()
    fun nextLevel()
    fun previousLevel()
    fun newGame()
    fun setLengthMinutes(minutes: Int)
}

class ClockViewModel(
    private val store: ClockStore,
    private val now: () -> Long,
) : ViewModel(), ClockActions {

    private var clock = store.load() ?: ClockState()
    private var blindsUp = false
    private var blindsUpJob: Job? = null

    private val _ui = MutableStateFlow(clock.toUi(now()))
    val ui: StateFlow<ClockUiState> = _ui.asStateFlow()

    private val levelEnded = Channel<Unit>(Channel.CONFLATED)

    /** Emits when a level ends by reaching zero, but not when the user changes the level. */
    val levelEnds: Flow<Unit> = levelEnded.receiveAsFlow()

    /** Keeps the clock current while the caller stays active. It only loops while the clock runs. */
    suspend fun runTicker() {
        tick()
        ui.distinctUntilChangedBy { it.isRunning }.collectLatest { state ->
            while (state.isRunning) {
                tick()
                delay(TICK_MS)
            }
        }
    }

    fun tick() {
        val t = now()
        val advanced = clock.advance(t)
        if (advanced.level != clock.level) {
            clock = advanced
            store.save(clock, t)
            showBlindsUp()
            levelEnded.trySend(Unit)
        }
        publish(t)
    }

    override fun startOrPause() = command { state, t ->
        if (state.isRunning) state.pause(t) else state.start(t)
    }

    override fun resetLevel() = command { state, t -> state.resetLevel(t) }
    override fun nextLevel() = command { state, t -> state.nextLevel(t) }
    override fun previousLevel() = command { state, t -> state.previousLevel(t) }
    override fun newGame() = command { state, _ -> state.newGame() }

    override fun setLengthMinutes(minutes: Int) = command { state, t ->
        val allowed = minutes.coerceIn(MIN_LENGTH_MINUTES, MAX_LENGTH_MINUTES)
        state.withLevelLength(allowed * MS_PER_MINUTE, t)
    }

    private inline fun command(change: (ClockState, Long) -> ClockState) {
        tick()
        val t = now()
        blindsUpJob?.cancel()
        blindsUp = false
        clock = change(clock, t)
        store.save(clock, t)
        publish(t)
    }

    private fun showBlindsUp() {
        blindsUp = true
        blindsUpJob?.cancel()
        blindsUpJob = viewModelScope.launch {
            delay(BLINDS_UP_MS)
            blindsUp = false
            publish(now())
        }
    }

    private fun publish(t: Long) {
        _ui.value = clock.toUi(t)
    }

    private fun ClockState.toUi(t: Long): ClockUiState {
        val left = remainingAt(t)
        return ClockUiState(
            level = level,
            lengthMinutes = (levelLengthMs / MS_PER_MINUTE).toInt(),
            secondsLeft = wholeSecondsLeft(left),
            isRunning = isRunning,
            isAtLevelStart = !isRunning && left == levelLengthMs,
            blindsUp = blindsUp,
        )
    }

    companion object {
        private const val TICK_MS = 100L
        private const val BLINDS_UP_MS = 10_000L

        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ClockViewModel(PreferencesClockStore(context), SystemClock::elapsedRealtime)
            }
        }
    }
}

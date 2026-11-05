package com.rangelreale.tlptclock.clock

const val MS_PER_MINUTE = 60_000L
const val MIN_LENGTH_MINUTES = 1
const val MAX_LENGTH_MINUTES = 180
const val DEFAULT_LENGTH_MINUTES = 20

/**
 * The poker clock as an immutable value.
 *
 * The clock divides play into levels. Each level is one blind period, and all levels have the same
 * length. Every operation takes the current time, in milliseconds from any steady clock, so that
 * this class needs no Android code and tests can control time.
 *
 * @property remainingMs The time left in the level while the clock is paused. It has no meaning
 *   while the clock is running.
 * @property endsAtMs The time at which the level ends, or null while the clock is paused.
 * @property waitingForAlarm True after a level ended while the next level waits for the alarm to
 *   stop. The clock does not count down while it waits. [start] ends the wait.
 */
data class ClockState(
    val levelLengthMs: Long = DEFAULT_LENGTH_MINUTES * MS_PER_MINUTE,
    val level: Int = 1,
    val remainingMs: Long = levelLengthMs,
    val endsAtMs: Long? = null,
    val waitingForAlarm: Boolean = false,
) {
    val isRunning: Boolean get() = endsAtMs != null

    fun remainingAt(nowMs: Long): Long =
        if (endsAtMs != null) (endsAtMs - nowMs).coerceAtLeast(0) else remainingMs

    /**
     * Moves to the level that is current at [nowMs]. Each new level starts with its full length.
     * The next level starts exactly when the last one ended, so no time is lost if a tick is late
     * or the app was in the background.
     *
     * With [waitForAlarm], the clock instead stops at the start of the next level and waits until
     * [start] is called, normally when the alarm stops.
     */
    fun advance(nowMs: Long, waitForAlarm: Boolean = false): ClockState {
        val ends = endsAtMs ?: return this
        if (nowMs < ends) return this
        if (waitForAlarm) {
            return copy(
                level = level + 1,
                remainingMs = levelLengthMs,
                endsAtMs = null,
                waitingForAlarm = true,
            )
        }
        val finished = 1 + (nowMs - ends) / levelLengthMs
        return copy(level = level + finished.toInt(), endsAtMs = ends + finished * levelLengthMs)
    }

    fun start(nowMs: Long): ClockState =
        if (isRunning) this else copy(endsAtMs = nowMs + remainingMs, waitingForAlarm = false)

    fun pause(nowMs: Long): ClockState {
        val current = advance(nowMs)
        if (!current.isRunning) return current.copy(waitingForAlarm = false)
        return current.copy(remainingMs = current.remainingAt(nowMs), endsAtMs = null)
    }

    fun resetLevel(nowMs: Long): ClockState = advance(nowMs).restarted(nowMs)

    fun nextLevel(nowMs: Long): ClockState {
        val current = advance(nowMs)
        return current.copy(level = current.level + 1).restarted(nowMs)
    }

    fun previousLevel(nowMs: Long): ClockState {
        val current = advance(nowMs)
        return current.copy(level = (current.level - 1).coerceAtLeast(1)).restarted(nowMs)
    }

    /** Returns to a paused level 1. The level length does not change. */
    fun newGame(): ClockState = ClockState(levelLengthMs = levelLengthMs)

    /**
     * Sets the length of the current level and of all later levels.
     *
     * The time already played in this level stays the same, so the time left changes by the same
     * amount as the length. If that would leave no time, the current time left stays, limited to
     * the new length. A length change therefore never ends a level by itself.
     */
    fun withLevelLength(lengthMs: Long, nowMs: Long): ClockState {
        val current = advance(nowMs)
        val left = current.remainingAt(nowMs)
        val shifted = left + lengthMs - current.levelLengthMs
        val newLeft = if (shifted > 0) shifted else minOf(left, lengthMs)
        return current.copy(
            levelLengthMs = lengthMs,
            remainingMs = newLeft,
            endsAtMs = if (current.isRunning) nowMs + newLeft else null,
        )
    }

    private fun restarted(nowMs: Long): ClockState = copy(
        remainingMs = levelLengthMs,
        endsAtMs = if (isRunning) nowMs + levelLengthMs else null,
    )
}

/** Rounds up, so the display shows the full length at the start and 00:00 only at the very end. */
fun wholeSecondsLeft(remainingMs: Long): Long = (remainingMs + 999) / 1000

fun formatSeconds(seconds: Long): String = "%02d:%02d".format(seconds / 60, seconds % 60)

package com.rangelreale.tlptclock.alarm

import com.rangelreale.tlptclock.clock.ClockState
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmTickTimeTest {
    private val running = ClockState(levelLengthMs = 60_000, endsAtMs = 60_000)

    @Test
    fun `an alarm slightly early counts as the level end`() {
        assertEquals(60_000L, running.alarmTickTime(59_000))
    }

    @Test
    fun `an alarm much too early keeps the current time`() {
        assertEquals(50_000L, running.alarmTickTime(50_000))
    }

    @Test
    fun `a late alarm keeps the current time`() {
        assertEquals(200_000L, running.alarmTickTime(200_000))
    }

    @Test
    fun `a paused clock keeps the current time`() {
        assertEquals(1_000L, ClockState().alarmTickTime(1_000))
    }
}

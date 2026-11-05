package com.rangelreale.tlptclock.alarm

import com.rangelreale.tlptclock.clock.ClockState
import org.junit.Assert.assertEquals
import org.junit.Test

class AdvanceForAlarmTest {
    private val running = ClockState(levelLengthMs = 60_000, endsAtMs = 60_000)

    @Test
    fun `an alarm slightly early still ends the level`() {
        val next = running.advanceForAlarm(59_000)
        assertEquals(2, next.level)
        assertEquals(120_000L, next.endsAtMs)
    }

    @Test
    fun `an alarm much too early keeps the level`() {
        assertEquals(running, running.advanceForAlarm(50_000))
    }

    @Test
    fun `a late alarm skips every level that has ended`() {
        assertEquals(4, running.advanceForAlarm(200_000).level)
    }

    @Test
    fun `a paused clock does not change`() {
        val paused = ClockState()
        assertEquals(paused, paused.advanceForAlarm(1_000_000))
    }
}

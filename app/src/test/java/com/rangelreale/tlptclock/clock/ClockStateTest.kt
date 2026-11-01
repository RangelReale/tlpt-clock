package com.rangelreale.tlptclock.clock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockStateTest {
    private val minute = MS_PER_MINUTE
    private val tenMinutes = ClockState(levelLengthMs = 10 * minute)

    @Test
    fun `new clock is paused at level 1 with the full length`() {
        assertFalse(tenMinutes.isRunning)
        assertEquals(1, tenMinutes.level)
        assertEquals(10 * minute, tenMinutes.remainingAt(12345))
    }

    @Test
    fun `running clock counts down and pause keeps the time left`() {
        val running = tenMinutes.start(1000)
        assertEquals(10 * minute - 500, running.remainingAt(1500))

        val paused = running.pause(1500)
        assertFalse(paused.isRunning)
        assertEquals(10 * minute - 500, paused.remainingAt(999_999))

        val resumed = paused.start(2000)
        assertEquals(10 * minute - 600, resumed.remainingAt(2100))
    }

    @Test
    fun `countdown resets and keeps running when the level ends`() {
        val running = tenMinutes.start(0)
        assertEquals(1, running.advance(10 * minute - 1).level)

        val next = running.advance(10 * minute)
        assertEquals(2, next.level)
        assertTrue(next.isRunning)
        assertEquals(10 * minute, next.remainingAt(10 * minute))
    }

    @Test
    fun `a late update loses no time`() {
        val next = tenMinutes.start(0).advance(10 * minute + 1500)
        assertEquals(2, next.level)
        assertEquals(10 * minute - 1500, next.remainingAt(10 * minute + 1500))
    }

    @Test
    fun `time in the background can pass several levels`() {
        val later = tenMinutes.start(0).advance(35 * minute)
        assertEquals(4, later.level)
        assertEquals(5 * minute, later.remainingAt(35 * minute))
    }

    @Test
    fun `paused clock does not advance`() {
        assertEquals(tenMinutes, tenMinutes.advance(100 * minute))
    }

    @Test
    fun `reset restores the full length and keeps the running state`() {
        val running = tenMinutes.start(0).resetLevel(3 * minute)
        assertTrue(running.isRunning)
        assertEquals(1, running.level)
        assertEquals(10 * minute, running.remainingAt(3 * minute))

        val paused = tenMinutes.start(0).pause(3 * minute).resetLevel(3 * minute)
        assertFalse(paused.isRunning)
        assertEquals(10 * minute, paused.remainingAt(3 * minute))
    }

    @Test
    fun `next and previous level restart the countdown`() {
        val next = tenMinutes.start(0).nextLevel(3 * minute)
        assertEquals(2, next.level)
        assertEquals(10 * minute, next.remainingAt(3 * minute))

        val previous = next.previousLevel(4 * minute)
        assertEquals(1, previous.level)
        assertEquals(10 * minute, previous.remainingAt(4 * minute))
    }

    @Test
    fun `previous level stops at level 1`() {
        assertEquals(1, tenMinutes.previousLevel(0).level)
    }

    @Test
    fun `new game returns to a paused level 1 and keeps the length`() {
        val game = tenMinutes.start(0).advance(25 * minute).newGame()
        assertEquals(ClockState(levelLengthMs = 10 * minute), game)
        assertNull(game.endsAtMs)
    }

    @Test
    fun `changing the length of an unplayed level sets the countdown to it`() {
        val changed = tenMinutes.withLevelLength(20 * minute, 0)
        assertEquals(20 * minute, changed.levelLengthMs)
        assertEquals(20 * minute, changed.remainingAt(0))
    }

    @Test
    fun `changing the length mid level keeps the time already played`() {
        val longer = tenMinutes.start(0).withLevelLength(15 * minute, 4 * minute)
        assertTrue(longer.isRunning)
        assertEquals(11 * minute, longer.remainingAt(4 * minute))

        val shorter = tenMinutes.start(0).withLevelLength(6 * minute, 4 * minute)
        assertEquals(2 * minute, shorter.remainingAt(4 * minute))
    }

    @Test
    fun `the new length applies to the next level`() {
        val changed = tenMinutes.start(0).withLevelLength(5 * minute, 0)
        val next = changed.advance(5 * minute)
        assertEquals(2, next.level)
        assertEquals(5 * minute, next.remainingAt(5 * minute))
    }

    @Test
    fun `shortening past the time played does not end the level`() {
        val changed = tenMinutes.start(0).withLevelLength(minute, 9 * minute + 30_000)
        assertEquals(1, changed.level)
        assertEquals(30_000, changed.remainingAt(9 * minute + 30_000))

        val capped = tenMinutes.start(0).withLevelLength(minute, 9 * minute)
        assertEquals(minute, capped.remainingAt(9 * minute))
    }

    @Test
    fun `display rounds up to whole seconds`() {
        assertEquals("20:00", formatSeconds(wholeSecondsLeft(20 * minute)))
        assertEquals("19:59", formatSeconds(wholeSecondsLeft(20 * minute - 1000)))
        assertEquals("00:01", formatSeconds(wholeSecondsLeft(1)))
        assertEquals("00:00", formatSeconds(wholeSecondsLeft(0)))
        assertEquals("120:00", formatSeconds(wholeSecondsLeft(120 * minute)))
    }
}

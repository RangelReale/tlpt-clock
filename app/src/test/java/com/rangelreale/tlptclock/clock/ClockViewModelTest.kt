package com.rangelreale.tlptclock.clock

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ClockViewModelTest {
    private var time = 0L
    private val store = PreferencesClockStore(ApplicationProvider.getApplicationContext<Application>())
    private val scheduled = mutableListOf<ClockState>()
    private val rings = mutableListOf<Int>()
    private var stops = 0
    private var wait = false

    private val alarm = object : LevelEndAlarm {
        override fun ring(level: Int): Boolean {
            rings += level
            return true
        }

        override fun stop() {
            stops++
        }
    }

    private fun controller() = ClockController(store, { time }, { scheduled += it }, alarm, { wait })

    private fun viewModel(controller: ClockController = controller()) =
        ClockViewModel(controller) { time }

    @Test
    fun `reaching zero starts the next level and rings once`() {
        val vm = viewModel()
        vm.setLengthMinutes(5)
        vm.startOrPause()
        assertEquals(5 * MS_PER_MINUTE, scheduled.last().endsAtMs)

        time = 5 * MS_PER_MINUTE - 1
        vm.tick()
        assertEquals(1, vm.ui.value.level)
        assertTrue(rings.isEmpty())

        time = 5 * MS_PER_MINUTE
        vm.tick()
        vm.tick()
        assertEquals(2, vm.ui.value.level)
        assertEquals(300, vm.ui.value.secondsLeft)
        assertTrue(vm.ui.value.isRunning)
        assertTrue(vm.ui.value.blindsUp)
        assertTrue(vm.ui.value.isAlarmRinging)
        assertEquals(listOf(2), rings)
        assertEquals(10 * MS_PER_MINUTE, scheduled.last().endsAtMs)
    }

    @Test
    fun `a level end noticed by the receiver is not rung again by the screen`() {
        val controller = controller()
        val vm = viewModel(controller)
        vm.setLengthMinutes(5)
        vm.startOrPause()

        time = 5 * MS_PER_MINUTE
        assertTrue(controller.tick())
        vm.tick()
        assertEquals(listOf(2), rings)
        assertEquals(2, vm.ui.value.level)
    }

    @Test
    fun `waiting for the alarm starts the next level when the alarm stops by itself`() {
        wait = true
        val controller = controller()
        val vm = viewModel(controller)
        vm.setLengthMinutes(5)
        vm.startOrPause()

        time = 5 * MS_PER_MINUTE + 200
        vm.tick()
        assertEquals(2, vm.ui.value.level)
        assertFalse(vm.ui.value.isRunning)
        assertTrue(vm.ui.value.blindsUp)
        assertNull(scheduled.last().endsAtMs)

        time += 20_000
        vm.tick()
        assertEquals(300, vm.ui.value.secondsLeft)

        controller.alarmStopped()
        vm.tick()
        assertTrue(vm.ui.value.isRunning)
        assertFalse(vm.ui.value.isAlarmRinging)
        assertEquals(time + 5 * MS_PER_MINUTE, scheduled.last().endsAtMs)
    }

    @Test
    fun `the stop button silences the alarm and starts the waiting level`() {
        wait = true
        val vm = viewModel()
        vm.startOrPause()
        time = 20 * MS_PER_MINUTE
        vm.tick()

        time += 2_000
        vm.stopAlarm()
        assertEquals(1, stops)
        assertTrue(vm.ui.value.isRunning)
        assertFalse(vm.ui.value.isAlarmRinging)
        assertEquals(time + 20 * MS_PER_MINUTE, scheduled.last().endsAtMs)
    }

    @Test
    fun `a waiting level that the alarm cannot ring for starts at once`() {
        wait = true
        val failing = object : LevelEndAlarm {
            override fun ring(level: Int) = false
            override fun stop() {}
        }
        val vm = viewModel(ClockController(store, { time }, alarm = failing, waitForAlarm = { wait }))
        vm.startOrPause()
        time = 20 * MS_PER_MINUTE
        vm.tick()
        assertTrue(vm.ui.value.isRunning)
        assertEquals(2, vm.ui.value.level)
    }

    @Test
    fun `pausing cancels the system alarm`() {
        val vm = viewModel()
        vm.startOrPause()
        vm.startOrPause()
        assertNull(scheduled.last().endsAtMs)
    }

    @Test
    fun `changing the level by hand does not ring`() {
        val vm = viewModel()
        vm.nextLevel()
        assertEquals(2, vm.ui.value.level)
        assertFalse(vm.ui.value.blindsUp)
        assertTrue(rings.isEmpty())
    }

    @Test
    fun `length and running clock survive an app restart`() {
        val first = viewModel()
        first.setLengthMinutes(7)
        first.startOrPause()

        time = MS_PER_MINUTE
        val second = viewModel()
        assertEquals(7, second.ui.value.lengthMinutes)
        assertTrue(second.ui.value.isRunning)
        assertEquals(6 * 60L, second.ui.value.secondsLeft)
    }

    @Test
    fun `a device restart loads the clock as paused with its saved time`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val first = viewModel()
        first.setLengthMinutes(7)
        first.startOrPause()

        android.provider.Settings.Global.putInt(
            context.contentResolver,
            android.provider.Settings.Global.BOOT_COUNT,
            99,
        )
        val loaded = store.load()
        assertNotNull(loaded)
        assertNull(loaded!!.endsAtMs)
        assertEquals(7 * MS_PER_MINUTE, loaded.remainingMs)
    }

    @Test
    fun `length reset and new game are ignored while running`() {
        val vm = viewModel()
        vm.nextLevel()
        vm.startOrPause()
        time = MS_PER_MINUTE
        vm.setLengthMinutes(5)
        vm.resetLevel()
        vm.newGame()
        vm.tick()
        assertEquals(20, vm.ui.value.lengthMinutes)
        assertEquals(2, vm.ui.value.level)
        assertEquals(19 * 60L, vm.ui.value.secondsLeft)
    }

    @Test
    fun `length stays within its limits`() {
        val vm = viewModel()
        vm.setLengthMinutes(0)
        assertEquals(MIN_LENGTH_MINUTES, vm.ui.value.lengthMinutes)
        vm.setLengthMinutes(1000)
        assertEquals(MAX_LENGTH_MINUTES, vm.ui.value.lengthMinutes)
    }
}

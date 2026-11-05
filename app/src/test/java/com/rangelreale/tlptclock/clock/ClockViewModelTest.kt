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

    private fun viewModel() = ClockViewModel(store, { time }, { scheduled += it })

    @Test
    fun `reaching zero starts the next level and schedules the next alarm`() {
        val vm = viewModel()
        vm.setLengthMinutes(5)
        vm.startOrPause()
        assertEquals(5 * MS_PER_MINUTE, scheduled.last().endsAtMs)

        time = 5 * MS_PER_MINUTE - 1
        vm.tick()
        assertEquals(1, vm.ui.value.level)

        time = 5 * MS_PER_MINUTE
        vm.tick()
        assertEquals(2, vm.ui.value.level)
        assertEquals(300, vm.ui.value.secondsLeft)
        assertTrue(vm.ui.value.isRunning)
        assertTrue(vm.ui.value.blindsUp)
        assertEquals(10 * MS_PER_MINUTE, scheduled.last().endsAtMs)
    }

    @Test
    fun `pausing cancels the alarm`() {
        val vm = viewModel()
        vm.startOrPause()
        vm.startOrPause()
        assertNull(scheduled.last().endsAtMs)
    }

    @Test
    fun `changing the level by hand does not show blinds up`() {
        val vm = viewModel()
        vm.nextLevel()
        assertEquals(2, vm.ui.value.level)
        assertFalse(vm.ui.value.blindsUp)
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
    fun `length stays within its limits`() {
        val vm = viewModel()
        vm.setLengthMinutes(0)
        assertEquals(MIN_LENGTH_MINUTES, vm.ui.value.lengthMinutes)
        vm.setLengthMinutes(1000)
        assertEquals(MAX_LENGTH_MINUTES, vm.ui.value.lengthMinutes)
    }
}

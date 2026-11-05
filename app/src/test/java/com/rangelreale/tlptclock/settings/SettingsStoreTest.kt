package com.rangelreale.tlptclock.settings

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun reset() = SettingsStore.resetForTest()

    @Test
    fun `defaults are three beeps and the device theme`() {
        assertEquals(AppSettings(), SettingsStore.get(context).settings.value)
    }

    @Test
    fun `settings survive an app restart`() {
        val uri = Uri.parse("content://media/internal/audio/media/7")
        SettingsStore.get(context).update {
            it.copy(
                theme = ThemeMode.LIGHT,
                sound = AlarmSound.DEVICE_SOUND,
                soundUri = uri,
                vibrate = true,
                beepCount = 5,
            )
        }
        SettingsStore.resetForTest()
        assertEquals(
            AppSettings(ThemeMode.LIGHT, AlarmSound.DEVICE_SOUND, 5, uri, true),
            SettingsStore.get(context).settings.value,
        )
    }

    @Test
    fun `beep count stays within its limits`() {
        val store = SettingsStore.get(context)
        store.update { it.copy(beepCount = 0) }
        assertEquals(MIN_BEEPS, store.settings.value.beepCount)
        store.update { it.copy(beepCount = 99) }
        assertEquals(MAX_BEEPS, store.settings.value.beepCount)
    }
}

package com.rangelreale.tlptclock.settings

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AlarmSound {
    /** Short beeps made by the tone generator. */
    BEEPS,

    /** A sound the user picked from the sounds on the device. */
    DEVICE_SOUND,

    /** No sound. Vibration can still be on. */
    NONE,
}

const val MIN_BEEPS = 1
const val MAX_BEEPS = 10

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val sound: AlarmSound = AlarmSound.BEEPS,
    val beepCount: Int = 3,
    /** The sound used when [sound] is [AlarmSound.DEVICE_SOUND]. */
    val soundUri: Uri? = null,
    val vibrate: Boolean = false,
)

/**
 * Stores the settings in shared preferences.
 *
 * Use [get] so that the screen and the alarm receiver share one instance and see the same values.
 */
class SettingsStore private constructor(private val prefs: SharedPreferences) {
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun update(change: (AppSettings) -> AppSettings) {
        val new = change(_settings.value).let {
            it.copy(beepCount = it.beepCount.coerceIn(MIN_BEEPS, MAX_BEEPS))
        }
        prefs.edit {
            putString(KEY_THEME, new.theme.name)
            putString(KEY_SOUND, new.sound.name)
            putInt(KEY_BEEPS, new.beepCount)
            if (new.soundUri != null) putString(KEY_URI, new.soundUri.toString()) else remove(KEY_URI)
            putBoolean(KEY_VIBRATE, new.vibrate)
        }
        _settings.value = new
    }

    private fun load(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            theme = enumOrDefault(prefs.getString(KEY_THEME, null), defaults.theme),
            sound = enumOrDefault(prefs.getString(KEY_SOUND, null), defaults.sound),
            beepCount = prefs.getInt(KEY_BEEPS, defaults.beepCount).coerceIn(MIN_BEEPS, MAX_BEEPS),
            soundUri = prefs.getString(KEY_URI, null)?.let(Uri::parse),
            vibrate = prefs.getBoolean(KEY_VIBRATE, defaults.vibrate),
        )
    }

    companion object {
        private const val KEY_THEME = "theme"
        private const val KEY_SOUND = "alarm_sound"
        private const val KEY_BEEPS = "beep_count"
        private const val KEY_URI = "sound_uri"
        private const val KEY_VIBRATE = "vibrate"

        @Volatile
        private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore = instance ?: synchronized(this) {
            instance ?: SettingsStore(
                context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE),
            ).also { instance = it }
        }

        /** Drops the shared instance. Tests use this because each test has a new application. */
        internal fun resetForTest() {
            instance = null
        }

        private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
            enumValues<T>().firstOrNull { it.name == name } ?: default
    }
}

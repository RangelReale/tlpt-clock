package com.rangelreale.tlptclock

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rangelreale.tlptclock.alarm.AlarmPlayer
import com.rangelreale.tlptclock.clock.ClockViewModel
import com.rangelreale.tlptclock.settings.SettingsStore
import com.rangelreale.tlptclock.ui.ClockScreen
import com.rangelreale.tlptclock.ui.ClockTheme
import com.rangelreale.tlptclock.ui.SettingsScreen
import com.rangelreale.tlptclock.ui.isDark
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: ClockViewModel by viewModels { ClockViewModel.factory(this) }
    private val settingsStore by lazy { SettingsStore.get(this) }
    private var testAlarm: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The volume keys should control the alarm while the clock is on screen.
        volumeControlStream = AudioManager.STREAM_ALARM

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.runTicker() }
        }

        setContent {
            val ui by viewModel.ui.collectAsStateWithLifecycle()
            val settings by settingsStore.settings.collectAsStateWithLifecycle()
            var showingSettings by rememberSaveable { mutableStateOf(false) }

            val view = LocalView.current
            DisposableEffect(view, ui.isRunning) {
                view.keepScreenOn = ui.isRunning
                onDispose { view.keepScreenOn = false }
            }

            val notificationPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted -> if (granted) viewModel.refreshBackground() }
            LaunchedEffect(ui.isRunning) {
                if (ui.isRunning && needsNotificationPermission()) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            val pickSound = rememberLauncherForActivityResult(PickAlarmSound()) { uri ->
                if (uri != null) settingsStore.update { it.copy(soundUri = uri) }
            }

            ClockTheme(settings.theme) {
                val dark = settings.theme.isDark()
                DisposableEffect(dark) {
                    val style = if (dark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                    onDispose {}
                }

                if (showingSettings) {
                    BackHandler { showingSettings = false }
                    val soundName = remember(settings.soundUri) { soundTitle(settings.soundUri) }
                    SettingsScreen(
                        settings = settings,
                        soundName = soundName,
                        onChange = settingsStore::update,
                        onPickSound = { pickSound.launch(settings.soundUri) },
                        onTestAlarm = ::playTestAlarm,
                        onBack = { showingSettings = false },
                    )
                } else {
                    ClockScreen(ui, viewModel, onOpenSettings = { showingSettings = true })
                }
            }
        }
    }

    private fun playTestAlarm() {
        testAlarm?.cancel()
        testAlarm = lifecycleScope.launch {
            AlarmPlayer.play(this@MainActivity, settingsStore.settings.value)
        }
    }

    private fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

    private fun soundTitle(uri: Uri?): String? =
        uri?.let { runCatching { RingtoneManager.getRingtone(this, it)?.getTitle(this) }.getOrNull() }
}

/** Opens the system sound picker. The input is the current sound, so the picker can mark it. */
private class PickAlarmSound : ActivityResultContract<Uri?, Uri?>() {
    override fun createIntent(context: Context, input: Uri?): Intent =
        Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            .putExtra(
                RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            )
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, input)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK && intent != null) {
            IntentCompat.getParcelableExtra(
                intent,
                RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                Uri::class.java,
            )
        } else {
            null
        }
}

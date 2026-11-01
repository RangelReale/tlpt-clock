package com.rangelreale.tlptclock

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rangelreale.tlptclock.clock.ClockViewModel
import com.rangelreale.tlptclock.clock.LevelEndAlarm
import com.rangelreale.tlptclock.ui.ClockScreen
import com.rangelreale.tlptclock.ui.ClockTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: ClockViewModel by viewModels { ClockViewModel.factory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The volume keys should control the alarm while the clock is on screen.
        volumeControlStream = AudioManager.STREAM_ALARM

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.runTicker() }
                viewModel.levelEnds.collect { LevelEndAlarm.play() }
            }
        }

        setContent {
            val ui by viewModel.ui.collectAsStateWithLifecycle()
            val view = LocalView.current
            DisposableEffect(view, ui.isRunning) {
                view.keepScreenOn = ui.isRunning
                onDispose { view.keepScreenOn = false }
            }
            ClockTheme {
                ClockScreen(ui, viewModel)
            }
        }
    }
}

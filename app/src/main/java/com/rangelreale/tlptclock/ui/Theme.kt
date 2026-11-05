package com.rangelreale.tlptclock.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.rangelreale.tlptclock.settings.ThemeMode

/** The colours the clock uses beyond the Material scheme. */
data class ClockColors(
    val time: Color,
    /** The time in the last minute, the "Blinds up" message and the Pause button. */
    val warning: Color,
    /** Text on a [warning] background. */
    val onWarning: Color,
    /** The time in the last ten seconds. */
    val urgent: Color,
)

private val DarkClockColors = ClockColors(
    time = Color.White,
    warning = Color(0xFFFFC107),
    onWarning = Color.Black,
    urgent = Color(0xFFFF5252),
)

// Darker shades keep the contrast high on a white background.
private val LightClockColors = ClockColors(
    time = Color.Black,
    warning = Color(0xFFB26A00),
    onWarning = Color.White,
    urgent = Color(0xFFC62828),
)

val LocalClockColors = staticCompositionLocalOf { DarkClockColors }

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun ClockTheme(mode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val dark = mode.isDark()
    val scheme = if (dark) {
        darkColorScheme(
            primary = Color(0xFF2FBF71),
            onPrimary = Color.Black,
            background = Color.Black,
            surface = Color.Black,
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF1B7F4A),
            onPrimary = Color.White,
            background = Color.White,
            surface = Color.White,
        )
    }
    CompositionLocalProvider(LocalClockColors provides if (dark) DarkClockColors else LightClockColors) {
        MaterialTheme(colorScheme = scheme) {
            Surface(modifier = Modifier.fillMaxSize(), content = content)
        }
    }
}

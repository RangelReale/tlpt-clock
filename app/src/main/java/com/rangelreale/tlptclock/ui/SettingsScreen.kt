package com.rangelreale.tlptclock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rangelreale.tlptclock.R
import com.rangelreale.tlptclock.settings.AlarmSound
import com.rangelreale.tlptclock.settings.AppSettings
import com.rangelreale.tlptclock.settings.MAX_RINGS
import com.rangelreale.tlptclock.settings.MIN_RINGS
import com.rangelreale.tlptclock.settings.ThemeMode

/**
 * The settings screen.
 *
 * @param soundName The name of the picked device sound, or null if there is none.
 * @param onPickSound Opens the system sound picker.
 * @param onTestAlarm Plays the alarm with the current settings.
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    soundName: String?,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onPickSound: () -> Unit,
    onTestAlarm: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        SectionTitle(stringResource(R.string.theme))
        Column(Modifier.selectableGroup()) {
            ThemeMode.entries.forEach { mode ->
                RadioRow(
                    label = stringResource(
                        when (mode) {
                            ThemeMode.SYSTEM -> R.string.theme_system
                            ThemeMode.LIGHT -> R.string.theme_light
                            ThemeMode.DARK -> R.string.theme_dark
                        },
                    ),
                    selected = settings.theme == mode,
                ) { onChange { it.copy(theme = mode) } }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SectionTitle(stringResource(R.string.level_end_alarm))
        Column(Modifier.selectableGroup()) {
            RadioRow(stringResource(R.string.sound_beeps), settings.sound == AlarmSound.BEEPS) {
                onChange { it.copy(sound = AlarmSound.BEEPS) }
            }

            RadioRow(
                label = stringResource(R.string.sound_device),
                detail = soundName ?: stringResource(R.string.sound_device_default),
                selected = settings.sound == AlarmSound.DEVICE_SOUND,
            ) { onChange { it.copy(sound = AlarmSound.DEVICE_SOUND) } }
            if (settings.sound == AlarmSound.DEVICE_SOUND) {
                OutlinedButton(
                    onClick = onPickSound,
                    modifier = Modifier.padding(start = 48.dp, bottom = 8.dp),
                ) { Text(stringResource(R.string.choose_sound)) }
            }

            RadioRow(stringResource(R.string.sound_none), settings.sound == AlarmSound.NONE) {
                onChange { it.copy(sound = AlarmSound.NONE) }
            }
        }

        RingCount(settings, onChange)

        SwitchRow(stringResource(R.string.vibrate), null, settings.vibrate) { on ->
            onChange { it.copy(vibrate = on) }
        }
        SwitchRow(
            stringResource(R.string.wait_for_alarm),
            stringResource(R.string.wait_for_alarm_detail),
            settings.waitForAlarm,
        ) { on -> onChange { it.copy(waitForAlarm = on) } }

        Button(
            onClick = onTestAlarm,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) { Text(stringResource(R.string.test_alarm)) }

        Text(
            stringResource(R.string.alarm_note),
            modifier = Modifier.padding(vertical = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    detail: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label)
            if (detail != null) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, detail: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(label)
            if (detail != null) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun RingCount(settings: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.ring_count), Modifier.weight(1f))
        val fewer = stringResource(R.string.fewer_rings)
        val more = stringResource(R.string.more_rings)
        Button(
            onClick = { onChange { it.copy(ringCount = it.ringCount - 1) } },
            enabled = settings.ringCount > MIN_RINGS,
            colors = ButtonDefaults.filledTonalButtonColors(),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = fewer },
        ) { Text("−", fontSize = 24.sp) }
        Text(
            settings.ringCount.toString(),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = { onChange { it.copy(ringCount = it.ringCount + 1) } },
            enabled = settings.ringCount < MAX_RINGS,
            colors = ButtonDefaults.filledTonalButtonColors(),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = more },
        ) { Text("+", fontSize = 24.sp) }
    }
}

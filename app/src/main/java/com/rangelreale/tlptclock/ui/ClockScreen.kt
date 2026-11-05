package com.rangelreale.tlptclock.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rangelreale.tlptclock.R
import com.rangelreale.tlptclock.clock.ClockActions
import com.rangelreale.tlptclock.clock.ClockUiState
import com.rangelreale.tlptclock.clock.MAX_LENGTH_MINUTES
import com.rangelreale.tlptclock.clock.MIN_LENGTH_MINUTES
import com.rangelreale.tlptclock.clock.formatSeconds

private const val BIG_STEP_MINUTES = 5

/**
 * Four 56 dp step buttons, four 4 dp gaps and 80 dp for "99 min" in the length button, so that a
 * 360 dp wide phone in portrait gets one row. Lengths of 100 minutes or more may touch the edges.
 */
private val ONE_ROW_LENGTH_WIDTH = 320.dp
private const val WARNING_SECONDS = 60L
private const val URGENT_SECONDS = 10L

@Composable
fun ClockScreen(ui: ClockUiState, actions: ClockActions, onOpenSettings: () -> Unit = {}) {
    var editingLength by rememberSaveable { mutableStateOf(false) }
    var confirmingReset by rememberSaveable { mutableStateOf(false) }
    var confirmingNewGame by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp),
    ) {
        if (maxWidth > maxHeight) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(3f)) {
                    LevelRow(ui, actions)
                    TimeDisplay(ui, Modifier.weight(1f)) { editingLength = true }
                }
                Column(
                    modifier = Modifier
                        .weight(2f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Controls(
                        ui,
                        actions,
                        { editingLength = true },
                        { confirmingReset = true },
                        { confirmingNewGame = true },
                        onOpenSettings,
                    )
                }
            }
        } else {
            Column {
                LevelRow(ui, actions)
                TimeDisplay(ui, Modifier.weight(1f)) { editingLength = true }
                Controls(
                    ui,
                    actions,
                    { editingLength = true },
                    { confirmingReset = true },
                    { confirmingNewGame = true },
                    onOpenSettings,
                )
            }
        }
    }

    if (editingLength) {
        LengthDialog(
            initialMinutes = ui.lengthMinutes,
            onConfirm = {
                actions.setLengthMinutes(it)
                editingLength = false
            },
            onDismiss = { editingLength = false },
        )
    }
    if (confirmingReset) {
        ConfirmDialog(
            action = stringResource(R.string.reset_level),
            message = stringResource(R.string.reset_level_message),
            onConfirm = {
                actions.resetLevel()
                confirmingReset = false
            },
            onDismiss = { confirmingReset = false },
        )
    }
    if (confirmingNewGame) {
        ConfirmDialog(
            action = stringResource(R.string.new_game),
            message = stringResource(R.string.new_game_message),
            onConfirm = {
                actions.newGame()
                confirmingNewGame = false
            },
            onDismiss = { confirmingNewGame = false },
        )
    }
}

/** Asks before an action that loses the time played. [action] is the title and the confirm button. */
@Composable
private fun ConfirmDialog(action: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(action) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(action) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun LevelRow(ui: ClockUiState, actions: ClockActions) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ArrowButton("‹", stringResource(R.string.previous_level), ui.level > 1, actions::previousLevel)
        Text(
            text = if (ui.blindsUp) {
                stringResource(R.string.blinds_up, ui.level)
            } else {
                stringResource(R.string.level, ui.level)
            },
            modifier = Modifier.weight(1f),
            color = if (ui.blindsUp) LocalClockColors.current.warning else Color.Unspecified,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        ArrowButton("›", stringResource(R.string.next_level), true, actions::nextLevel)
    }
}

@Composable
private fun ArrowButton(glyph: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .size(56.dp)
            .semantics { contentDescription = label },
    ) {
        Text(glyph, fontSize = 32.sp)
    }
}

/** The time fills the space it has. Tapping it opens the length dialog. */
@Composable
private fun TimeDisplay(ui: ClockUiState, modifier: Modifier, onClick: () -> Unit) {
    val text = formatSeconds(ui.secondsLeft)
    val colors = LocalClockColors.current
    val color = when {
        ui.secondsLeft <= URGENT_SECONDS -> colors.urgent
        ui.secondsLeft <= WARNING_SECONDS -> colors.warning
        else -> colors.time
    }
    val editLabel = stringResource(R.string.set_level_length)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = ui.canEdit, onClickLabel = editLabel, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // A monospace digit is 0.6 em wide. The margin keeps the text from touching the edges.
        val byWidth = maxWidth / (text.length * 0.62f)
        val byHeight = maxHeight * 0.8f
        val size = with(LocalDensity.current) { minOf(byWidth, byHeight).toSp() }
        Text(
            text = text,
            color = color,
            fontSize = size,
            lineHeight = size,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun Controls(
    ui: ClockUiState,
    actions: ClockActions,
    onEditLength: () -> Unit,
    onResetLevel: () -> Unit,
    onNewGame: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.level_length),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
        LengthControls(ui, actions, onEditLength)
        Button(
            onClick = if (ui.isAlarmRinging) actions::stopAlarm else actions::startOrPause,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            colors = if (ui.isAlarmRinging) {
                ButtonDefaults.buttonColors(
                    containerColor = LocalClockColors.current.urgent,
                    contentColor = Color.White,
                )
            } else if (ui.isRunning) {
                ButtonDefaults.buttonColors(
                    containerColor = LocalClockColors.current.warning,
                    contentColor = LocalClockColors.current.onWarning,
                )
            } else {
                ButtonDefaults.buttonColors()
            },
        ) {
            Text(
                text = stringResource(
                    when {
                        ui.isAlarmRinging -> R.string.stop_alarm
                        ui.isRunning -> R.string.pause
                        ui.isAtLevelStart -> R.string.start
                        else -> R.string.resume
                    },
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onResetLevel,
                enabled = ui.canEdit,
                contentPadding = PaddingValues(4.dp),
                modifier = Modifier.weight(1f).height(48.dp),
            ) {
                Text(stringResource(R.string.reset_level), maxLines = 1)
            }
            OutlinedButton(
                onClick = onNewGame,
                enabled = ui.canEdit,
                contentPadding = PaddingValues(4.dp),
                modifier = Modifier.weight(1f).height(48.dp),
            ) {
                Text(stringResource(R.string.new_game), maxLines = 1)
            }
            val settingsLabel = stringResource(R.string.settings)
            OutlinedButton(
                onClick = onOpenSettings,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = settingsLabel },
            ) {
                Icon(Icons.Filled.Settings, contentDescription = null)
            }
        }
    }
}

/** One row with the five-minute steps on the outside when it fits, otherwise two rows. */
@Composable
private fun LengthControls(ui: ClockUiState, actions: ClockActions, onEditLength: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= ONE_ROW_LENGTH_WIDTH) {
            LengthStepper(ui, actions, onEditLength, withBigSteps = true)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LengthStepper(ui, actions, onEditLength, withBigSteps = false)
                BigStepRow(ui, actions)
            }
        }
    }
}

@Composable
private fun LengthStepper(
    ui: ClockUiState,
    actions: ClockActions,
    onEditLength: () -> Unit,
    withBigSteps: Boolean,
) {
    val canShorten = ui.canEdit && ui.lengthMinutes > MIN_LENGTH_MINUTES
    val canLengthen = ui.canEdit && ui.lengthMinutes < MAX_LENGTH_MINUTES
    Row(
        horizontalArrangement = Arrangement.spacedBy(if (withBigSteps) 4.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (withBigSteps) {
            StepButton(
                glyph = "−$BIG_STEP_MINUTES",
                label = stringResource(R.string.much_shorter),
                enabled = canShorten,
            ) { actions.setLengthMinutes(ui.lengthMinutes - BIG_STEP_MINUTES) }
        }
        StepButton(
            glyph = "−",
            label = stringResource(R.string.shorter),
            enabled = canShorten,
        ) { actions.setLengthMinutes(ui.lengthMinutes - 1) }
        OutlinedButton(
            onClick = onEditLength,
            enabled = ui.canEdit,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.weight(1f).height(56.dp),
        ) {
            Text(
                text = stringResource(R.string.length_minutes, ui.lengthMinutes),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
            )
        }
        StepButton(
            glyph = "+",
            label = stringResource(R.string.longer),
            enabled = canLengthen,
        ) { actions.setLengthMinutes(ui.lengthMinutes + 1) }
        if (withBigSteps) {
            StepButton(
                glyph = "+$BIG_STEP_MINUTES",
                label = stringResource(R.string.much_longer),
                enabled = canLengthen,
            ) { actions.setLengthMinutes(ui.lengthMinutes + BIG_STEP_MINUTES) }
        }
    }
}

@Composable
private fun StepButton(glyph: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(),
        modifier = Modifier
            .size(width = 56.dp, height = 56.dp)
            .semantics { contentDescription = label },
    ) {
        // "−5" and "+5" need a smaller size than "−" and "+" to fit the round button.
        if (glyph.length > 1) {
            Text(glyph, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        } else {
            Text(glyph, fontSize = 28.sp)
        }
    }
}

/** Changes the length by five minutes. The view model keeps the result within the limits. */
@Composable
private fun BigStepRow(ui: ClockUiState, actions: ClockActions) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BigStepButton(
            glyph = "−$BIG_STEP_MINUTES",
            label = stringResource(R.string.much_shorter),
            enabled = ui.canEdit && ui.lengthMinutes > MIN_LENGTH_MINUTES,
            modifier = Modifier.weight(1f),
        ) { actions.setLengthMinutes(ui.lengthMinutes - BIG_STEP_MINUTES) }
        BigStepButton(
            glyph = "+$BIG_STEP_MINUTES",
            label = stringResource(R.string.much_longer),
            enabled = ui.canEdit && ui.lengthMinutes < MAX_LENGTH_MINUTES,
            modifier = Modifier.weight(1f),
        ) { actions.setLengthMinutes(ui.lengthMinutes + BIG_STEP_MINUTES) }
    }
}

@Composable
private fun BigStepButton(
    glyph: String,
    label: String,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(),
        modifier = modifier
            .height(48.dp)
            .semantics { contentDescription = label },
    ) {
        Text(glyph, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LengthDialog(initialMinutes: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    // The old value starts selected, so typing replaces it.
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        val value = initialMinutes.toString()
        mutableStateOf(TextFieldValue(value, selection = TextRange(0, value.length)))
    }
    val minutes = field.text.toIntOrNull()?.takeIf { it in MIN_LENGTH_MINUTES..MAX_LENGTH_MINUTES }
    val focus = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_level_length)) },
        text = {
            OutlinedTextField(
                value = field,
                onValueChange = { new -> field = new.copy(text = new.text.filter(Char::isDigit).take(3)) },
                label = {
                    Text(
                        stringResource(
                            R.string.minutes_field,
                            MIN_LENGTH_MINUTES,
                            MAX_LENGTH_MINUTES,
                        ),
                    )
                },
                isError = minutes == null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { minutes?.let(onConfirm) }),
                modifier = Modifier.focusRequester(focus),
            )
            // The dialog content composes separately, so the request must be made in here.
            LaunchedEffect(Unit) { focus.requestFocus() }
        },
        confirmButton = {
            TextButton(enabled = minutes != null, onClick = { minutes?.let(onConfirm) }) {
                Text(stringResource(R.string.set))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

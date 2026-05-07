package com.ritesh.cashiro.presentation.ui.features.settings.webhooks

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.data.webhook.WebhookScheduledTime
import com.ritesh.cashiro.data.webhook.WebhookSettings
import com.ritesh.cashiro.data.webhook.WebhookSyncMode
import com.ritesh.cashiro.data.webhook.WebhookValidation
import com.ritesh.cashiro.presentation.ui.components.GenericTypeSwitcher
import com.ritesh.cashiro.presentation.ui.icons.Bag
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.theme.Spacing

private enum class ScheduleSegment(val mode: WebhookSyncMode, val label: String) {
    Interval(WebhookSyncMode.INTERVAL, "Interval"),
    Scheduled(WebhookSyncMode.SCHEDULED, "Scheduled")
}

private val MIN_INTERVAL_HOURS = WebhookValidation.MIN_INTERVAL_HOURS
private val MAX_INTERVAL_HOURS = WebhookValidation.MAX_INTERVAL_HOURS

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WebhookSyncSettingsCard(
    settings: WebhookSettings,
    onSettingsChange: (WebhookSettings) -> Unit
) {
    val focusManager = LocalFocusManager.current
    
    // Use local state for immediate visual feedback and layout stability
    var selectedMode by remember(settings.syncMode) { mutableStateOf(settings.syncMode) }

    Column(modifier = Modifier.fillMaxWidth()) {
        val segments = ScheduleSegment.entries
        GenericTypeSwitcher(
            selectedIndex = segments.indexOfFirst { it.mode == selectedMode }.coerceAtLeast(0),
            onIndexChange = { index ->
                val target = segments[index].mode
                if (selectedMode != target) {
                    focusManager.clearFocus()
                    selectedMode = target
                    onSettingsChange(settings.copy(syncMode = target))
                }
            },
            options = segments.map { it.label },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        AnimatedContent(
            targetState = selectedMode,
            transitionSpec = {
                // Simultaneous crossfade with no delay to prevent the "flicker gap"
                fadeIn(animationSpec = tween(220))
                    .togetherWith(fadeOut(animationSpec = tween(180)))
                    .using(
                        SizeTransform(
                            clip = false,
                            sizeAnimationSpec = { _, _ -> tween(300) }
                        )
                    )
            },
            contentAlignment = Alignment.TopStart,
            modifier = Modifier.fillMaxWidth(),
            label = "SyncSettingsContent"
        ) { mode ->
            when (mode) {
                WebhookSyncMode.INTERVAL -> {
                    IntervalDetail(
                        initialHours = settings.intervalHours,
                        onCommit = { hours ->
                            onSettingsChange(
                                settings.copy(
                                    syncMode = WebhookSyncMode.INTERVAL,
                                    intervalHours = hours
                                )
                            )
                        }
                    )
                }
                WebhookSyncMode.SCHEDULED -> {
                    ScheduledDetail(
                        times = settings.scheduledTimes,
                        onTimesChange = { newTimes ->
                            onSettingsChange(
                                settings.copy(
                                    syncMode = WebhookSyncMode.SCHEDULED,
                                    scheduledTimes = newTimes
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntervalDetail(
    initialHours: Int,
    onCommit: (Int) -> Unit
) {
    val focusManager = LocalFocusManager.current

    var text by remember(initialHours) {
        mutableStateOf(initialHours.coerceIn(MIN_INTERVAL_HOURS, MAX_INTERVAL_HOURS).toString())
    }

    val errorMessage = WebhookValidation.validateIntervalHours(text)

    fun commit() {
        val parsed = text.toIntOrNull()
        if (parsed != null && parsed in MIN_INTERVAL_HOURS..MAX_INTERVAL_HOURS) {
            onCommit(parsed)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = "Run automatically on a repeating interval. Range: $MIN_INTERVAL_HOURS–$MAX_INTERVAL_HOURS hours.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        CashiroTextField(
            value = text,
            onValueChange = { input ->
                text = input.filter { it.isDigit() }.take(2)
            },
            label = "Every",
            suffix = { Text("hours") },
            leading = Icons.Rounded.Bolt,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    commit()
                    focusManager.clearFocus()
                }
            ),
            shape = singleFieldShape(),
            errorMessage = errorMessage,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (!state.isFocused) commit()
                }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduledDetail(
    times: List<WebhookScheduledTime>,
    onTimesChange: (List<WebhookScheduledTime>) -> Unit
) {
    var pickerOpen by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = "Run at fixed times each day. Falls back to inexact delivery if exact alarm permission is unavailable.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (times.isEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "No times added yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                times.forEachIndexed { index, time ->
                    ScheduledTimeRow(
                        time = time,
                        onToggle = { enabled ->
                            onTimesChange(times.map {
                                if (it.id == time.id) it.copy(enabled = enabled) else it
                            })
                        },
                        onDelete = {
                            onTimesChange(times.filter { it.id != time.id })
                        }
                    )
                    if (index < times.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xs))
        Button(
            onClick = { pickerOpen = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text("Add Schedule Time")
        }
    }

    if (pickerOpen) {
        TimePickerDialog(
            initialHour = 9,
            initialMinute = 0,
            onConfirm = { h, m ->
                pickerOpen = false
                if (times.none { it.hour == h && it.minute == m }) {
                    onTimesChange(times + WebhookScheduledTime(hour = h, minute = m))
                }
            },
            onDismiss = { pickerOpen = false }
        )
    }
}

@Composable
private fun ScheduledTimeRow(
    time: WebhookScheduledTime,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            text = time.displayTime(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = if (time.enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Box(modifier = Modifier.weight(1f))
        Switch(
            checked = time.enabled,
            onCheckedChange = onToggle,
            modifier = Modifier.semantics {
                contentDescription = "${time.displayTime()} schedule, " +
                    if (time.enabled) "enabled" else "disabled"
            }
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.semantics {
                contentDescription = "Remove ${time.displayTime()} schedule"
            }
        ) {
            Icon(
                Iconax.Bag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Pick a time") },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = state)
            }
        }
    )
}

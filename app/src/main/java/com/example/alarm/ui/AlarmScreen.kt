package com.example.alarm.ui

import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.data.AlarmEntity
import com.example.alarm.data.RepeatMode
import com.example.alarm.domain.AlarmScheduleCalculator
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    alarms: List<AlarmEntity>,
    canScheduleExact: Boolean,
    canUseFullScreenIntent: Boolean,
    hasNotificationPermission: Boolean,
    onRequestExactAccess: () -> Unit,
    onRequestFullScreenAccess: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSave: (AlarmEntity) -> Unit,
    onToggle: (AlarmEntity, Boolean) -> Unit,
    onDelete: (AlarmEntity) -> Unit,
) {
    var editorOpen by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<AlarmEntity?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Alarms") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingAlarm = null
                editorOpen = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add alarm")
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
        ) {
            if (!canScheduleExact || !canUseFullScreenIntent || !hasNotificationPermission) {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Alarm permissions", style = MaterialTheme.typography.titleMedium)
                        if (!canScheduleExact) {
                            Text("Exact alarm access is needed for on-time delivery.")
                            TextButton(onClick = onRequestExactAccess) { Text("Allow exact alarms") }
                        }
                        if (!hasNotificationPermission) {
                            Text("Notifications are needed for snooze and dismiss controls.")
                            TextButton(onClick = onRequestNotificationPermission) {
                                Text("Allow notifications")
                            }
                        }
                        if (!canUseFullScreenIntent) {
                            Text("Allow full-screen alarms to show controls over the lock screen.")
                            TextButton(onClick = onRequestFullScreenAccess) {
                                Text("Allow full-screen alarms")
                            }
                        }
                    }
                }
            }

            if (alarms.isEmpty()) {
                Text(
                    "No alarms yet",
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 64.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmRow(
                            alarm = alarm,
                            onClick = {
                                editingAlarm = alarm
                                editorOpen = true
                            },
                            onToggle = { enabled -> onToggle(alarm, enabled) },
                        )
                    }
                }
            }
        }
    }

    if (editorOpen) {
        AlarmEditorDialog(
            alarm = editingAlarm,
            onDismiss = { editorOpen = false },
            onSave = {
                onSave(it)
                editorOpen = false
            },
            onDelete = { alarm ->
                onDelete(alarm)
                editorOpen = false
            },
        )
    }
}

@Composable
private fun AlarmRow(alarm: AlarmEntity, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val context = LocalContext.current
    val timePattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    val time = remember(alarm.hour, alarm.minute, timePattern) {
        LocalTime.of(alarm.hour, alarm.minute).format(DateTimeFormatter.ofPattern(timePattern))
    }

    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(time, fontSize = 32.sp, fontWeight = FontWeight.Light)
                Text(alarm.label.ifBlank { "Alarm" }, style = MaterialTheme.typography.titleSmall)
                Text(
                    repeatDescription(alarm),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = alarm.enabled, onCheckedChange = onToggle)
            IconButton(onClick = onClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit ${alarm.label}")
            }
        }
    }
}

private fun repeatDescription(alarm: AlarmEntity): String = when (alarm.repeatMode) {
    RepeatMode.ONCE -> "Once"
    RepeatMode.EVERY_DAYS -> "Every ${alarm.intervalDays} day${if (alarm.intervalDays == 1) "" else "s"}"
    RepeatMode.WEEKDAYS -> DayOfWeek.values()
        .filter { alarm.repeatDaysMask and (1 shl (it.value - 1)) != 0 }
        .joinToString(" ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
        .ifBlank { "Choose repeat days" }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditorDialog(
    alarm: AlarmEntity?,
    onDismiss: () -> Unit,
    onSave: (AlarmEntity) -> Unit,
    onDelete: (AlarmEntity) -> Unit,
) {
    val context = LocalContext.current
    val initialTime = remember(alarm?.id) {
        if (alarm != null) LocalTime.of(alarm.hour, alarm.minute)
        else LocalTime.now().plusMinutes(5).withSecond(0).withNano(0)
    }
    var hour by remember(alarm?.id) { mutableStateOf(initialTime.hour) }
    var minute by remember(alarm?.id) { mutableStateOf(initialTime.minute) }
    var label by remember(alarm?.id) { mutableStateOf(alarm?.label ?: "Alarm") }
    var repeatMode by remember(alarm?.id) { mutableStateOf(alarm?.repeatMode ?: RepeatMode.ONCE) }
    var selectedDays by remember(alarm?.id) {
        mutableStateOf(
            DayOfWeek.values().filterTo(mutableSetOf()) { day ->
                alarm != null && alarm.repeatDaysMask and (1 shl (day.value - 1)) != 0
            },
        )
    }
    var intervalText by remember(alarm?.id) {
        mutableStateOf((alarm?.intervalDays ?: 1).toString())
    }
    var snoozeText by remember(alarm?.id) {
        mutableStateOf((alarm?.snoozeMinutes ?: 5).toString())
    }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val interval = intervalText.toIntOrNull()
    val snooze = snoozeText.toIntOrNull()
    val validRepeat = when (repeatMode) {
        RepeatMode.ONCE -> true
        RepeatMode.WEEKDAYS -> selectedDays.isNotEmpty()
        RepeatMode.EVERY_DAYS -> interval != null && interval in 1..365
    }
    val valid = validRepeat && snooze != null && snooze in 1..60

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (alarm == null) "New alarm" else "Edit alarm") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().height(480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, selectedHour, selectedMinute ->
                                hour = selectedHour
                                minute = selectedMinute
                            },
                            hour,
                            minute,
                            DateFormat.is24HourFormat(context),
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
                    Text(LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern(pattern)))
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("Repeat", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RepeatMode.values().forEach { mode ->
                        FilterChip(
                            selected = repeatMode == mode,
                            onClick = { repeatMode = mode },
                            label = {
                                Text(
                                    when (mode) {
                                        RepeatMode.ONCE -> "Once"
                                        RepeatMode.WEEKDAYS -> "Weekdays"
                                        RepeatMode.EVERY_DAYS -> "Every N days"
                                    },
                                )
                            },
                        )
                    }
                }

                if (repeatMode == RepeatMode.WEEKDAYS) {
                    Text("Repeat on", style = MaterialTheme.typography.bodyMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DayOfWeek.values().forEach { day ->
                            val selected = day in selectedDays
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedDays = selectedDays.toMutableSet().apply {
                                        if (selected) remove(day) else add(day)
                                    }
                                },
                                label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
                            )
                        }
                    }
                    if (selectedDays.isEmpty()) {
                        Text("Select at least one day", color = MaterialTheme.colorScheme.error)
                    }
                }

                if (repeatMode == RepeatMode.EVERY_DAYS) {
                    OutlinedTextField(
                        value = intervalText,
                        onValueChange = { intervalText = it.filter(Char::isDigit).take(3) },
                        label = { Text("Repeat every (days)") },
                        supportingText = { Text("Enter 1 to 365 days") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OutlinedTextField(
                    value = snoozeText,
                    onValueChange = { snoozeText = it.filter(Char::isDigit).take(2) },
                    label = { Text("Snooze (minutes)") },
                    supportingText = { Text("Enter 1 to 60 minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val now = Instant.now()
                    val zone = ZoneId.systemDefault()
                    val anchorDate = alarm?.anchorDateEpochDay?.let(java.time.LocalDate::ofEpochDay)
                    val preserveAnchor = alarm != null && anchorDate != null &&
                        alarm.repeatMode == repeatMode &&
                        (repeatMode != RepeatMode.ONCE ||
                            (alarm.hour == hour && alarm.minute == minute &&
                                anchorDate.atTime(alarm.hour, alarm.minute).atZone(zone).toInstant().isAfter(now)))
                    val initialAnchor = if (preserveAnchor) anchorDate!! else
                        AlarmScheduleCalculator.initialDate(hour, minute, now, zone)
                    onSave(
                        AlarmEntity(
                            id = alarm?.id ?: 0,
                            hour = hour,
                            minute = minute,
                            label = label.trim().ifBlank { "Alarm" },
                            enabled = alarm?.enabled ?: true,
                            repeatMode = repeatMode,
                            repeatDaysMask = selectedDays.fold(0) { mask, day ->
                                mask or (1 shl (day.value - 1))
                            },
                            intervalDays = interval ?: 1,
                            anchorDateEpochDay = initialAnchor.toEpochDay(),
                            snoozeMinutes = snooze ?: 5,
                        ),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (alarm != null) {
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete alarm")
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )

    if (showDeleteConfirmation && alarm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete alarm?") },
            text = { Text("${alarm.label.ifBlank { "Alarm" }} will be removed.") },
            confirmButton = {
                TextButton(onClick = { onDelete(alarm) }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
            },
        )
    }
}
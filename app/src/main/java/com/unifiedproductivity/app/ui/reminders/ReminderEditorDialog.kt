package com.unifiedproductivity.app.ui.reminders

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.unifiedproductivity.app.data.entity.Reminder
import com.unifiedproductivity.app.data.model.Priority
import com.unifiedproductivity.app.data.model.RecurrenceFrequency
import com.unifiedproductivity.app.ui.common.LeadTimePicker
import com.unifiedproductivity.app.ui.common.LocationField
import com.unifiedproductivity.app.ui.common.RecurrencePicker
import com.unifiedproductivity.app.ui.common.pickDate
import com.unifiedproductivity.app.ui.common.pickTime
import com.unifiedproductivity.app.util.DateTimeUtils

/**
 * Create/edit dialog for a reminder, Apple Reminders-style: title, notes, priority,
 * a real date picker + optional time-of-day, location, repeat, and a reminder
 * lead time. Passing a non-null [initial] edits that reminder in place.
 */
@Composable
fun ReminderEditorDialog(
    initial: Reminder?,
    defaultListId: String?,
    onDismiss: () -> Unit,
    onSave: (Reminder, Boolean) -> Unit
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var priority by remember { mutableStateOf(initial?.priority ?: Priority.NONE) }
    var due by remember { mutableStateOf(initial?.dueDate) }
    var hasTime by remember { mutableStateOf(initial?.hasTime ?: false) }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var amountText by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    var blockTime by remember { mutableStateOf(false) }
    var recurrence by remember { mutableStateOf(initial?.recurrence ?: RecurrenceFrequency.NONE) }
    var recurrenceInterval by remember { mutableIntStateOf(initial?.recurrenceInterval ?: 1) }
    var leadMinutes by remember {
        mutableStateOf(initial?.notifyMinutesBefore?.firstOrNull()?.toIntOrNull())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Reminder" else "Edit Reminder") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("What needs doing?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Notes (optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                LocationField(
                    value = location,
                    onValueChange = { location = it },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input -> if (input.all { it.isDigit() }) amountText = input },
                    placeholder = { Text("Cost (optional)") },
                    leadingIcon = { Icon(Icons.Filled.AttachMoney, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Priority", style = MaterialTheme.typography.bodyMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    Priority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.label) }
                        )
                    }
                }

                HorizontalDivider()

                Text("Due", style = MaterialTheme.typography.bodyMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    AssistChip(
                        onClick = {
                            pickDate(context, due ?: DateTimeUtils.endOfToday(), isDark) { picked ->
                                // Keep an existing time-of-day; otherwise anchor to end of day.
                                due = if (hasTime) picked else DateTimeUtils.endOfDay(picked)
                            }
                        },
                        leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                        label = {
                            Text(due?.let { DateTimeUtils.formatDayMonth(it) } ?: "Add date")
                        }
                    )
                    if (due != null) {
                        AssistChip(
                            onClick = {
                                pickTime(context, due ?: System.currentTimeMillis(), isDark) { picked ->
                                    due = picked
                                    hasTime = true
                                }
                            },
                            leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                            label = {
                                Text(if (hasTime && due != null) DateTimeUtils.formatTime(due!!) else "Add time")
                            }
                        )
                        AssistChip(
                            onClick = { due = null; hasTime = false; recurrence = RecurrenceFrequency.NONE },
                            leadingIcon = { Icon(Icons.Filled.Close, contentDescription = null) },
                            label = { Text("Clear") }
                        )
                    }
                }

                if (due != null) {
                    RecurrencePicker(
                        frequency = recurrence,
                        interval = recurrenceInterval,
                        onFrequencyChange = { recurrence = it },
                        onIntervalChange = { recurrenceInterval = it }
                    )
                    LeadTimePicker(
                        label = "Remind me",
                        selectedMinutes = leadMinutes,
                        onSelect = { leadMinutes = it }
                    )
                    if (initial?.linkedEventId == null) {
                        FilterChip(
                            selected = blockTime,
                            onClick = { blockTime = !blockTime },
                            label = { Text("Block time on Calendar") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val listId = initial?.listId ?: defaultListId ?: return@TextButton
                    if (title.isBlank()) return@TextButton
                    val base = initial ?: Reminder(listId = listId)
                    onSave(
                        base.copy(
                            title = title.trim(),
                            description = description.trim(),
                            priority = priority,
                            dueDate = due,
                            hasTime = hasTime,
                            location = location.trim().ifBlank { null },
                            amount = amountText.toLongOrNull(),
                            recurrence = if (due != null) recurrence else RecurrenceFrequency.NONE,
                            recurrenceInterval = recurrenceInterval,
                            notifyMinutesBefore = leadMinutes?.let { listOf(it.toString()) } ?: emptyList()
                        ),
                        blockTime
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

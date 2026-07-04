package com.unifiedproductivity.app.ui.calendar

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.unifiedproductivity.app.data.entity.Event
import com.unifiedproductivity.app.ui.common.LocationField
import com.unifiedproductivity.app.ui.common.pickDate
import com.unifiedproductivity.app.ui.common.pickTime
import com.unifiedproductivity.app.util.DateTimeUtils

private const val HOUR_MS = 60 * 60 * 1000L

/**
 * Create/edit dialog for a calendar event: title, location, date, real start/end
 * time pickers, all-day toggle, and (when creating) "attach note" which spins up a
 * linked meeting note. Passing a non-null [initial] edits that event.
 */
@Composable
fun EventEditorDialog(
    initial: Event?,
    day: Long,
    defaultCalendarId: String?,
    onDismiss: () -> Unit,
    onSave: (Event, Boolean) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var allDay by remember { mutableStateOf(initial?.isAllDay ?: false) }
    var attachNote by remember { mutableStateOf(false) }
    var start by remember {
        mutableLongStateOf(initial?.startDateTime ?: (DateTimeUtils.startOfDay(day) + 9 * HOUR_MS))
    }
    var end by remember {
        mutableLongStateOf(initial?.endDateTime ?: (DateTimeUtils.startOfDay(day) + 10 * HOUR_MS))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Event" else "Edit Event") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Event title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LocationField(
                    value = location,
                    onValueChange = { location = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    AssistChip(
                        onClick = {
                            pickDate(context, start) { picked ->
                                // Move both ends to the picked day, keeping times.
                                val dayShift = DateTimeUtils.startOfDay(picked) - DateTimeUtils.startOfDay(start)
                                start += dayShift
                                end += dayShift
                            }
                        },
                        leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                        label = { Text(DateTimeUtils.formatDayMonth(start)) }
                    )
                    FilterChip(
                        selected = allDay,
                        onClick = { allDay = !allDay },
                        label = { Text("All day") }
                    )
                }

                if (!allDay) {
                    Text("Time", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = {
                                pickTime(context, start) { picked ->
                                    val duration = end - start
                                    start = picked
                                    end = picked + duration
                                }
                            },
                            leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                            label = { Text("Starts ${DateTimeUtils.formatTime(start)}") }
                        )
                        AssistChip(
                            onClick = {
                                pickTime(context, end) { picked ->
                                    end = if (picked > start) picked else start + HOUR_MS
                                }
                            },
                            leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                            label = { Text("Ends ${DateTimeUtils.formatTime(end)}") }
                        )
                    }
                }

                if (initial == null) {
                    FilterChip(
                        selected = attachNote,
                        onClick = { attachNote = !attachNote },
                        label = { Text("Attach meeting note") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Blank id is resolved to a real calendar by the ViewModel on save.
                    val calendarId = initial?.calendarId ?: defaultCalendarId ?: ""
                    if (title.isBlank()) return@TextButton
                    val startFinal = if (allDay) DateTimeUtils.startOfDay(start) else start
                    val endFinal = if (allDay) DateTimeUtils.endOfDay(start) else end
                    val base = initial ?: Event(
                        calendarId = calendarId,
                        startDateTime = startFinal,
                        endDateTime = endFinal
                    )
                    onSave(
                        base.copy(
                            title = title.trim(),
                            location = location.trim().ifBlank { null },
                            isAllDay = allDay,
                            startDateTime = startFinal,
                            endDateTime = endFinal
                        ),
                        attachNote
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

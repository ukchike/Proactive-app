package com.unifiedproductivity.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unifiedproductivity.app.data.entity.Event
import com.unifiedproductivity.app.data.entity.Reminder
import com.unifiedproductivity.app.ui.theme.PriorityHigh
import com.unifiedproductivity.app.util.DateTimeUtils

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNewNote: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenCalendar: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Today",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = DateTimeUtils.formatDate(System.currentTimeMillis()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        item {
            QuickActions(
                onNote = onNewNote,
                onReminder = onOpenReminders,
                onEvent = onOpenCalendar
            )
        }

        if (state.overdueReminders.isNotEmpty()) {
            item { SectionHeader("Overdue", PriorityHigh, Icons.Filled.Warning) }
            items(state.overdueReminders, key = { "od-${it.id}" }) { reminder ->
                ReminderRow(reminder, overdue = true, onClick = onOpenReminders)
            }
        }

        item { SectionHeader("Schedule", MaterialTheme.colorScheme.secondary, Icons.Filled.CalendarMonth) }
        if (state.todayEvents.isEmpty()) {
            item { EmptyHint("No events today") }
        } else {
            items(state.todayEvents, key = { "ev-${it.id}" }) { event ->
                EventRow(event, onClick = onOpenCalendar)
            }
        }

        item { SectionHeader("Due today", MaterialTheme.colorScheme.tertiary, Icons.Filled.CheckCircle) }
        if (state.todayReminders.isEmpty()) {
            item { EmptyHint("Nothing due today — nice.") }
        } else {
            items(state.todayReminders, key = { "td-${it.id}" }) { reminder ->
                ReminderRow(reminder, overdue = false, onClick = onOpenReminders)
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun QuickActions(onNote: () -> Unit, onReminder: () -> Unit, onEvent: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickAction("Note", Icons.Filled.Description, MaterialTheme.colorScheme.primary, Modifier.weight(1f), onNote)
        QuickAction("Task", Icons.Filled.CheckCircle, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f), onReminder)
        QuickAction("Event", Icons.Filled.CalendarMonth, MaterialTheme.colorScheme.secondary, Modifier.weight(1f), onEvent)
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(onClick = onClick, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = tint)
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SectionHeader(title: String, accent: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EventRow(event: Event, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondary,
                shape = CircleShape,
                modifier = Modifier.size(10.dp)
            ) {}
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(event.title.ifBlank { "(untitled event)" }, style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (event.isAllDay) "All day"
                    else "${DateTimeUtils.formatTime(event.startDateTime)} – ${DateTimeUtils.formatTime(event.endDateTime)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun ReminderRow(reminder: Reminder, overdue: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (overdue) PriorityHigh else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(reminder.title.ifBlank { "(untitled)" }, style = MaterialTheme.typography.bodyLarge)
                reminder.dueDate?.let {
                    Text(
                        DateTimeUtils.formatDueLabel(it, reminder.hasTime),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (overdue) PriorityHigh else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

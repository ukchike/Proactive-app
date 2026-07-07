package com.unifiedproductivity.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unifiedproductivity.app.data.entity.BudgetItem
import com.unifiedproductivity.app.data.entity.Event
import com.unifiedproductivity.app.data.entity.Note
import com.unifiedproductivity.app.data.entity.Reminder
import com.unifiedproductivity.app.ui.notes.eisenhowerColor
import com.unifiedproductivity.app.ui.theme.AccentGray
import com.unifiedproductivity.app.ui.theme.AccentGreen
import com.unifiedproductivity.app.ui.theme.CalendarAccent
import com.unifiedproductivity.app.ui.theme.NotesAccent
import com.unifiedproductivity.app.ui.theme.PriorityHigh
import com.unifiedproductivity.app.ui.theme.RemindersAccent
import com.unifiedproductivity.app.util.CurrencyFormatter
import com.unifiedproductivity.app.util.DateTimeUtils

/**
 * iOS-style Home hub: app-like tiles that open each module full screen, then only
 * what matters now — high-priority items and what's due today.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenNotes: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenNote: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Home", style = MaterialTheme.typography.headlineMedium)
            Text(
                DateTimeUtils.formatDate(System.currentTimeMillis()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        // App-like module tiles.
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModuleTile("Notes", Icons.Filled.Description, NotesAccent, Modifier.weight(1f), onOpenNotes)
                ModuleTile("Reminders", Icons.Filled.CheckCircle, RemindersAccent, Modifier.weight(1f), onOpenReminders)
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModuleTile("Calendar", Icons.Filled.CalendarMonth, CalendarAccent, Modifier.weight(1f), onOpenCalendar)
                ModuleTile("Budget", Icons.Filled.AccountBalanceWallet, AccentGreen, Modifier.weight(1f), onOpenBudget)
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModuleTile("Settings", Icons.Filled.Settings, AccentGray, Modifier.weight(1f), onOpenSettings)
            }
        }

        // ----- Finances -----
        item { SectionHeader("Finances", AccentGreen, Icons.Filled.AccountBalanceWallet) }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FinanceSummaryCard(
                    label = "Outstanding Income",
                    amount = state.outstandingIncome,
                    color = AccentGreen,
                    onClick = onOpenBudget,
                    modifier = Modifier.weight(1f)
                )
                FinanceSummaryCard(
                    label = "Outstanding Expenses",
                    amount = state.outstandingExpenses,
                    color = PriorityHigh,
                    onClick = onOpenBudget,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ----- High priority -----
        val hasPriority = state.priorityNotes.isNotEmpty() || state.priorityReminders.isNotEmpty()
        item { SectionHeader("Priority", PriorityHigh, Icons.Filled.Flag) }
        if (!hasPriority) {
            item { EmptyHint("Nothing urgent right now") }
        } else {
            items(state.priorityNotes, key = { "pn-${it.id}" }) { note ->
                PriorityNoteRow(note, onClick = { onOpenNote(note.id) })
            }
            items(state.priorityReminders, key = { "pr-${it.id}" }) { reminder ->
                ReminderRow(reminder, overdue = DateTimeUtils.isOverdue(reminder.dueDate), onClick = onOpenReminders)
            }
        }

        // ----- Due -----
        if (state.overdueReminders.isNotEmpty()) {
            item { SectionHeader("Overdue", PriorityHigh, Icons.Filled.Warning) }
            items(state.overdueReminders, key = { "od-${it.id}" }) { reminder ->
                ReminderRow(reminder, overdue = true, onClick = onOpenReminders)
            }
        }

        item { SectionHeader("Today", CalendarAccent, Icons.Filled.CalendarMonth) }
        if (state.todayEvents.isEmpty() && state.todayReminders.isEmpty() && state.todayBudgetItems.isEmpty()) {
            item { EmptyHint("Nothing due today — nice.") }
        } else {
            items(state.todayEvents, key = { "ev-${it.id}" }) { event ->
                EventRow(event, onClick = onOpenCalendar)
            }
            items(state.todayReminders, key = { "td-${it.id}" }) { reminder ->
                ReminderRow(reminder, overdue = false, onClick = onOpenReminders)
            }
            items(state.todayBudgetItems, key = { "bd-${it.id}" }) { item ->
                BudgetDueRow(item, onClick = onOpenBudget)
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ModuleTile(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(onClick = onClick, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SectionHeader(title: String, accent: Color, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PriorityNoteRow(note: Note, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Description,
                contentDescription = null,
                tint = NotesAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    note.title.ifBlank { "(untitled note)" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    note.eisenhower.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = eisenhowerColor(note.eisenhower)
                )
            }
        }
    }
}

@Composable
private fun EventRow(event: Event, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = CalendarAccent, shape = CircleShape, modifier = Modifier.size(10.dp)) {}
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
        }
    }
}

@Composable
private fun ReminderRow(reminder: Reminder, overdue: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (overdue) PriorityHigh else RemindersAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(reminder.title.ifBlank { "(untitled)" }, style = MaterialTheme.typography.bodyLarge)
                reminder.dueDate?.let {
                    Text(
                        DateTimeUtils.formatDueLabel(it, reminder.hasTime),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (overdue) PriorityHigh
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FinanceSummaryCard(
    label: String,
    amount: Long,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(onClick = onClick, modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = color)
            Text(
                CurrencyFormatter.format(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun BudgetDueRow(item: BudgetItem, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.AccountBalanceWallet,
                contentDescription = null,
                tint = AccentGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.size(12.dp))
            Text(item.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                CurrencyFormatter.format(item.amount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
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

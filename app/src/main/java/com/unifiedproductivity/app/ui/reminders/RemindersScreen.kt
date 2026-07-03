package com.unifiedproductivity.app.ui.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unifiedproductivity.app.data.entity.Reminder
import com.unifiedproductivity.app.data.model.Priority
import com.unifiedproductivity.app.data.model.SmartList
import com.unifiedproductivity.app.ui.theme.PriorityHigh
import com.unifiedproductivity.app.ui.theme.PriorityLow
import com.unifiedproductivity.app.ui.theme.PriorityMedium
import com.unifiedproductivity.app.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(viewModel: RemindersViewModel) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    val counts by viewModel.smartListCounts.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Reminders", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = MaterialTheme.colorScheme.tertiary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New reminder")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SmartList.entries.toList()) { smart ->
                    val selected = (filter as? ReminderFilter.Smart)?.list == smart
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.selectSmartList(smart) },
                        label = {
                            val c = counts[smart] ?: 0
                            Text(if (c > 0) "${smart.label} ($c)" else smart.label)
                        }
                    )
                }
                items(lists, key = { it.id }) { list ->
                    val selected = (filter as? ReminderFilter.UserList)?.listId == list.id
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.selectUserList(list.id) },
                        label = { Text("${list.icon} ${list.name}") }
                    )
                }
            }

            if (reminders.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Nothing here yet",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(reminders, key = { it.id }) { reminder ->
                        ReminderItem(
                            reminder = reminder,
                            onToggle = { viewModel.toggleComplete(reminder) },
                            onFlag = { viewModel.toggleFlag(reminder) },
                            onDelete = { viewModel.delete(reminder.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddReminderDialog(
            onDismiss = { showAdd = false },
            onSave = { reminder, blockTime ->
                viewModel.save(reminder, blockTime)
                showAdd = false
            },
            defaultListId = viewModel.defaultListId()
        )
    }
}

@Composable
private fun ReminderItem(
    reminder: Reminder,
    onToggle: () -> Unit,
    onFlag: () -> Unit,
    onDelete: () -> Unit
) {
    Card {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggle) {
                Icon(
                    if (reminder.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = "Toggle complete",
                    tint = priorityColor(reminder.priority)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    reminder.title.ifBlank { "(untitled)" },
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else null,
                    color = if (reminder.isCompleted)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.onSurface
                )
                reminder.dueDate?.let {
                    val overdue = DateTimeUtils.isOverdue(it) && !reminder.isCompleted
                    Text(
                        DateTimeUtils.formatDueLabel(it, reminder.hasTime),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (overdue) PriorityHigh
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
                if (reminder.blockedBy.isNotEmpty()) {
                    Text(
                        "Blocked",
                        style = MaterialTheme.typography.labelSmall,
                        color = PriorityMedium
                    )
                }
            }
            IconButton(onClick = onFlag) {
                Icon(
                    Icons.Filled.Flag,
                    contentDescription = "Flag",
                    tint = if (reminder.isFlagged) PriorityMedium
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
private fun priorityColor(priority: Priority): Color = when (priority) {
    Priority.HIGH -> PriorityHigh
    Priority.MEDIUM -> PriorityMedium
    Priority.LOW -> PriorityLow
    Priority.NONE -> MaterialTheme.colorScheme.tertiary
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReminderDialog(
    onDismiss: () -> Unit,
    onSave: (Reminder, Boolean) -> Unit,
    defaultListId: String?
) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.NONE) }
    var due by remember { mutableStateOf<Long?>(null) }
    var blockTime by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("What needs doing?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Priority", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Priority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.label) }
                        )
                    }
                }

                Text("Due", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DueChip("None", due == null) { due = null }
                    DueChip("Today", due == DateTimeUtils.endOfToday()) { due = DateTimeUtils.endOfToday() }
                    DueChip("Tomorrow", due == DateTimeUtils.endOfToday() + DateTimeUtils.DAY_MS) {
                        due = DateTimeUtils.endOfToday() + DateTimeUtils.DAY_MS
                    }
                }

                if (due != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                    if (title.isNotBlank() && defaultListId != null) {
                        onSave(
                            Reminder(
                                title = title.trim(),
                                listId = defaultListId,
                                priority = priority,
                                dueDate = due,
                                hasTime = false
                            ),
                            blockTime
                        )
                    }
                }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DueChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

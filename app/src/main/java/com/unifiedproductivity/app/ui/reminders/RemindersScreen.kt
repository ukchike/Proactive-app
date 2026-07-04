package com.unifiedproductivity.app.ui.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.unifiedproductivity.app.ui.common.SwipeToDelete
import com.unifiedproductivity.app.ui.theme.PriorityHigh
import com.unifiedproductivity.app.ui.theme.PriorityLow
import com.unifiedproductivity.app.ui.theme.PriorityMedium
import com.unifiedproductivity.app.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(viewModel: RemindersViewModel, onBack: () -> Unit) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    val counts by viewModel.smartListCounts.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Reminder?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Reminders", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Home")
                    }
                }
            )
        },
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
                        "Nothing here yet — tap + to add",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(reminders, key = { it.id }) { reminder ->
                        SwipeToDelete(onDelete = { viewModel.delete(reminder.id) }) {
                            ReminderItem(
                                reminder = reminder,
                                onClick = { editing = reminder },
                                onToggle = { viewModel.toggleComplete(reminder) },
                                onFlag = { viewModel.toggleFlag(reminder) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd || editing != null) {
        ReminderEditorDialog(
            initial = editing,
            defaultListId = viewModel.defaultListId(),
            onDismiss = { showAdd = false; editing = null },
            onSave = { reminder, blockTime ->
                viewModel.save(reminder, blockTime)
                showAdd = false
                editing = null
            }
        )
    }
}

@Composable
private fun ReminderItem(
    reminder: Reminder,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onFlag: () -> Unit
) {
    Card(onClick = onClick) {
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
                if (!reminder.location.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            reminder.location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                if (reminder.blockedBy.isNotEmpty()) {
                    Text("Blocked", style = MaterialTheme.typography.labelSmall, color = PriorityMedium)
                }
            }
            if (reminder.linkedEventId != null) {
                Icon(
                    Icons.Filled.Event,
                    contentDescription = "Time blocked on calendar",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onFlag) {
                Icon(
                    Icons.Filled.Flag,
                    contentDescription = "Flag",
                    tint = if (reminder.isFlagged) PriorityMedium
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
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

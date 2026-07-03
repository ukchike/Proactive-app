package com.unifiedproductivity.app.ui.calendar

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unifiedproductivity.app.data.entity.Event
import com.unifiedproductivity.app.ui.util.parseHexColor
import com.unifiedproductivity.app.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onOpenNote: (String) -> Unit
) {
    val visibleMonth by viewModel.visibleMonth.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val monthEvents by viewModel.monthEvents.collectAsStateWithLifecycle()
    val dayEvents by viewModel.selectedDayEvents.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Event?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(DateTimeUtils.formatMonthYear(visibleMonth), fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { viewModel.goToToday() }) { Text("Today") }
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
                    }
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New event")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            WeekdayHeader()
            MonthGrid(
                visibleMonth = visibleMonth,
                selectedDay = selectedDay,
                eventDays = remember(monthEvents) {
                    monthEvents.map { DateTimeUtils.startOfDay(it.startDateTime) }.toSet()
                },
                onSelectDay = viewModel::selectDay
            )
            Spacer(Modifier.height(8.dp))
            Text(
                DateTimeUtils.formatDate(selectedDay),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            if (dayEvents.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Text(
                        "No events — tap + to schedule one",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(dayEvents, key = { it.id }) { event ->
                        EventItem(
                            event = event,
                            color = parseHexColor(event.color ?: viewModel.calendarColor(event.calendarId)),
                            onClick = { editing = event },
                            onOpenNote = { viewModel.openLinkedNote(event, onOpenNote) },
                            onDelete = { viewModel.deleteEvent(event.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAdd || editing != null) {
        EventEditorDialog(
            initial = editing,
            day = selectedDay,
            defaultCalendarId = viewModel.defaultCalendarId(),
            onDismiss = { showAdd = false; editing = null },
            onSave = { event, attachNote ->
                viewModel.saveEvent(event, attachNote)
                showAdd = false
                editing = null
            }
        )
    }
}

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
            Text(
                d,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun MonthGrid(
    visibleMonth: Long,
    selectedDay: Long,
    eventDays: Set<Long>,
    onSelectDay: (Long) -> Unit
) {
    val firstWeekday = DateTimeUtils.firstWeekdayOfMonth(visibleMonth)
    val daysInMonth = DateTimeUtils.daysInMonth(visibleMonth)
    val totalCells = firstWeekday + daysInMonth
    val rows = (totalCells + 6) / 7
    val today = DateTimeUtils.startOfDay(System.currentTimeMillis())

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - firstWeekday + 1
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (dayNumber in 1..daysInMonth) {
                            val dayStart = DateTimeUtils.startOfDay(
                                DateTimeUtils.startOfMonth(visibleMonth) + (dayNumber - 1) * DateTimeUtils.DAY_MS
                            )
                            DayCell(
                                day = dayNumber,
                                isSelected = dayStart == selectedDay,
                                isToday = dayStart == today,
                                hasEvent = eventDays.contains(dayStart),
                                onClick = { onSelectDay(dayStart) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasEvent: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = when {
                isSelected -> MaterialTheme.colorScheme.secondary
                isToday -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                else -> Color.Transparent
            },
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    day.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onSecondary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(5.dp)
                .clip(CircleShape)
                .then(
                    if (hasEvent) Modifier.background(MaterialTheme.colorScheme.secondary)
                    else Modifier
                )
        )
    }
}

@Composable
private fun EventItem(
    event: Event,
    color: Color,
    onClick: () -> Unit,
    onOpenNote: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    Card(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(event.title.ifBlank { "(untitled event)" }, style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (event.isAllDay) "All day"
                    else "${DateTimeUtils.formatTime(event.startDateTime)} – ${DateTimeUtils.formatTime(event.endDateTime)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (!event.location.isNullOrBlank()) {
                    Text(event.location, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                if (event.linkedReminderId != null) {
                    Text(
                        "Linked to a reminder",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            if (!event.location.isNullOrBlank()) {
                IconButton(onClick = {
                    val uri = Uri.parse("geo:0,0?q=${Uri.encode(event.location)}")
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (e: ActivityNotFoundException) {
                        // No maps app installed — nothing to do.
                    }
                }) {
                    Icon(Icons.Filled.Map, contentDescription = "Open in maps",
                        tint = MaterialTheme.colorScheme.secondary)
                }
            }
            IconButton(onClick = onOpenNote) {
                Icon(Icons.Filled.Description, contentDescription = "Meeting note",
                    tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            }
        }
    }
}

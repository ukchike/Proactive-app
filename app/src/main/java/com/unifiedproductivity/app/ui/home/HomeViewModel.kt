package com.unifiedproductivity.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unifiedproductivity.app.data.entity.Event
import com.unifiedproductivity.app.data.entity.Note
import com.unifiedproductivity.app.data.entity.Reminder
import com.unifiedproductivity.app.data.repository.CalendarRepository
import com.unifiedproductivity.app.data.repository.NotesRepository
import com.unifiedproductivity.app.data.repository.RemindersRepository
import com.unifiedproductivity.app.util.DateTimeUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * State for the Home hub: high-priority items (urgent notes + high/flagged tasks)
 * and what's due — overdue tasks, today's tasks, and today's events.
 */
data class HomeUiState(
    val priorityNotes: List<Note> = emptyList(),
    val priorityReminders: List<Reminder> = emptyList(),
    val overdueReminders: List<Reminder> = emptyList(),
    val todayReminders: List<Reminder> = emptyList(),
    val todayEvents: List<Event> = emptyList()
)

class HomeViewModel(
    remindersRepository: RemindersRepository,
    calendarRepository: CalendarRepository,
    notesRepository: NotesRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        combine(
            notesRepository.observeUrgentNotes(),
            remindersRepository.observeHighPriority(),
            remindersRepository.observeOverdue(),
            remindersRepository.observeToday(),
            calendarRepository.observeEventsInRange(
                DateTimeUtils.startOfToday(),
                DateTimeUtils.endOfToday()
            )
        ) { urgentNotes, highPriority, overdue, today, events ->
            HomeUiState(
                priorityNotes = urgentNotes,
                priorityReminders = highPriority,
                overdueReminders = overdue,
                todayReminders = today,
                todayEvents = events
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
}

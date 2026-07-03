package com.unifiedproductivity.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unifiedproductivity.app.data.entity.Event
import com.unifiedproductivity.app.data.entity.Reminder
import com.unifiedproductivity.app.data.repository.CalendarRepository
import com.unifiedproductivity.app.data.repository.RemindersRepository
import com.unifiedproductivity.app.util.DateTimeUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Aggregated state for the unified Home dashboard. */
data class HomeUiState(
    val todayEvents: List<Event> = emptyList(),
    val todayReminders: List<Reminder> = emptyList(),
    val overdueReminders: List<Reminder> = emptyList()
)

class HomeViewModel(
    remindersRepository: RemindersRepository,
    calendarRepository: CalendarRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        combine(
            calendarRepository.observeEventsInRange(
                DateTimeUtils.startOfToday(),
                DateTimeUtils.endOfToday()
            ),
            remindersRepository.observeToday(),
            remindersRepository.observeOverdue()
        ) { events, todayReminders, overdue ->
            HomeUiState(
                todayEvents = events,
                todayReminders = todayReminders,
                overdueReminders = overdue
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
}

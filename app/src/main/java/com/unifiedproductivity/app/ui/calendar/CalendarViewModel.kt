package com.unifiedproductivity.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unifiedproductivity.app.data.entity.CalendarEntity
import com.unifiedproductivity.app.data.entity.Event
import com.unifiedproductivity.app.data.repository.CalendarRepository
import com.unifiedproductivity.app.integration.LinkService
import com.unifiedproductivity.app.util.DateTimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val repository: CalendarRepository,
    private val linkService: LinkService
) : ViewModel() {

    /** First-of-month timestamp for the month currently displayed. */
    private val _visibleMonth = MutableStateFlow(DateTimeUtils.startOfMonth(System.currentTimeMillis()))
    val visibleMonth: StateFlow<Long> = _visibleMonth.asStateFlow()

    /** Day the user has tapped in the grid (defaults to today). */
    private val _selectedDay = MutableStateFlow(DateTimeUtils.startOfDay(System.currentTimeMillis()))
    val selectedDay: StateFlow<Long> = _selectedDay.asStateFlow()

    val calendars: StateFlow<List<CalendarEntity>> =
        repository.observeCalendars()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** All events within the visible month (used to draw event dots). */
    val monthEvents: StateFlow<List<Event>> =
        _visibleMonth.flatMapLatest { month ->
            repository.observeEventsInRange(month, DateTimeUtils.addMonths(month, 1))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Events on the selected day (the agenda list under the grid). */
    val selectedDayEvents: StateFlow<List<Event>> =
        _selectedDay.flatMapLatest { day ->
            repository.observeEventsInRange(day, DateTimeUtils.endOfDay(day))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun previousMonth() {
        _visibleMonth.value = DateTimeUtils.addMonths(_visibleMonth.value, -1)
    }

    fun nextMonth() {
        _visibleMonth.value = DateTimeUtils.addMonths(_visibleMonth.value, 1)
    }

    fun goToToday() {
        val now = System.currentTimeMillis()
        _visibleMonth.value = DateTimeUtils.startOfMonth(now)
        _selectedDay.value = DateTimeUtils.startOfDay(now)
    }

    fun selectDay(dayStart: Long) { _selectedDay.value = dayStart }

    fun saveEvent(event: Event, attachNote: Boolean = false) = viewModelScope.launch {
        // A blank calendarId means the screen had no calendar loaded yet (this was
        // the "events not saving" bug) — resolve to a real calendar, never drop.
        val resolved = if (event.calendarId.isBlank()) {
            event.copy(calendarId = repository.ensureDefaultCalendar().id)
        } else {
            event
        }
        repository.saveEvent(resolved)
        if (attachNote) linkService.createNoteForEvent(resolved)
    }

    /** Open the event's linked meeting note, creating it on first tap. */
    fun openLinkedNote(event: Event, onReady: (String) -> Unit) = viewModelScope.launch {
        val note = linkService.getOrCreateNoteForEvent(event)
        onReady(note.id)
    }

    fun deleteEvent(id: String) = viewModelScope.launch { repository.deleteEvent(id) }

    fun defaultCalendarId(): String? = calendars.value.firstOrNull()?.id

    fun calendarColor(calendarId: String): String =
        calendars.value.firstOrNull { it.id == calendarId }?.color ?: "#009688"
}

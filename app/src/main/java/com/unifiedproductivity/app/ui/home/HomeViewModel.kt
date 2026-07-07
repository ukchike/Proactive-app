package com.unifiedproductivity.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unifiedproductivity.app.data.entity.BudgetItem
import com.unifiedproductivity.app.data.entity.Event
import com.unifiedproductivity.app.data.entity.Note
import com.unifiedproductivity.app.data.entity.Reminder
import com.unifiedproductivity.app.data.repository.BudgetRepository
import com.unifiedproductivity.app.data.repository.CalendarRepository
import com.unifiedproductivity.app.data.repository.NotesRepository
import com.unifiedproductivity.app.data.repository.RemindersRepository
import com.unifiedproductivity.app.util.BudgetCalculations
import com.unifiedproductivity.app.util.DateTimeUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * State for the Home hub: high-priority items (urgent notes + high/flagged tasks),
 * what's due — overdue tasks, today's tasks/events/bills — and outstanding finances.
 */
data class HomeUiState(
    val priorityNotes: List<Note> = emptyList(),
    val priorityReminders: List<Reminder> = emptyList(),
    val overdueReminders: List<Reminder> = emptyList(),
    val todayReminders: List<Reminder> = emptyList(),
    val todayEvents: List<Event> = emptyList(),
    val todayBudgetItems: List<BudgetItem> = emptyList(),
    val outstandingIncome: Long = 0,
    val outstandingExpenses: Long = 0
)

/** Grouping for the "core" (non-budget) combine — kotlinx.coroutines' typed combine tops out at 5 flows. */
private data class CoreState(
    val urgentNotes: List<Note>,
    val highPriority: List<Reminder>,
    val overdue: List<Reminder>,
    val today: List<Reminder>,
    val events: List<Event>
)

private data class BudgetState(val allItems: List<BudgetItem>, val todayItems: List<BudgetItem>)

class HomeViewModel(
    remindersRepository: RemindersRepository,
    calendarRepository: CalendarRepository,
    notesRepository: NotesRepository,
    budgetRepository: BudgetRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        combine(
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
                CoreState(urgentNotes, highPriority, overdue, today, events)
            },
            combine(
                budgetRepository.observeAllItems(),
                budgetRepository.observeItemsDueBetween(
                    DateTimeUtils.startOfToday(),
                    DateTimeUtils.endOfToday()
                )
            ) { allItems, todayItems -> BudgetState(allItems, todayItems) }
        ) { core, budget ->
            HomeUiState(
                priorityNotes = core.urgentNotes,
                priorityReminders = core.highPriority,
                overdueReminders = core.overdue,
                todayReminders = core.today,
                todayEvents = core.events,
                todayBudgetItems = budget.todayItems.filter { !it.isCompleted },
                outstandingIncome = BudgetCalculations.outstandingIncome(budget.allItems),
                outstandingExpenses = BudgetCalculations.outstandingExpenses(budget.allItems)
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
}

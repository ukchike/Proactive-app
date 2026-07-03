package com.unifiedproductivity.app.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unifiedproductivity.app.data.entity.Reminder
import com.unifiedproductivity.app.data.entity.ReminderList
import com.unifiedproductivity.app.data.model.SmartList
import com.unifiedproductivity.app.data.repository.RemindersRepository
import com.unifiedproductivity.app.integration.LinkService
import com.unifiedproductivity.app.util.DateTimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Selection is either a smart list or a user-created list (mutually exclusive). */
sealed interface ReminderFilter {
    data class Smart(val list: SmartList) : ReminderFilter
    data class UserList(val listId: String) : ReminderFilter
}

@OptIn(ExperimentalCoroutinesApi::class)
class RemindersViewModel(
    private val repository: RemindersRepository,
    private val linkService: LinkService
) : ViewModel() {

    private val _filter = MutableStateFlow<ReminderFilter>(ReminderFilter.Smart(SmartList.TODAY))
    val filter: StateFlow<ReminderFilter> = _filter.asStateFlow()

    val lists: StateFlow<List<ReminderList>> =
        repository.observeLists()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<Reminder>> =
        _filter.flatMapLatest { filter -> flowFor(filter) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Live counts for the smart-list header chips. */
    val smartListCounts: StateFlow<Map<SmartList, Int>> =
        combine(
            repository.observeToday().map { it.size },
            repository.observeOverdue().map { it.size },
            repository.observeFlagged().map { it.size },
            repository.observeAll()
        ) { today, overdue, flagged, all ->
            mapOf(
                SmartList.TODAY to today,
                SmartList.OVERDUE to overdue,
                SmartList.FLAGGED to flagged,
                SmartList.SCHEDULED to all.count { !it.isCompleted && it.dueDate != null && it.dueDate >= DateTimeUtils.endOfToday() },
                SmartList.ALL to all.count { !it.isCompleted },
                SmartList.COMPLETED to all.count { it.isCompleted }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private fun flowFor(filter: ReminderFilter): Flow<List<Reminder>> = when (filter) {
        is ReminderFilter.UserList -> repository.observeByList(filter.listId)
        is ReminderFilter.Smart -> when (filter.list) {
            SmartList.TODAY -> repository.observeToday()
            SmartList.OVERDUE -> repository.observeOverdue()
            SmartList.FLAGGED -> repository.observeFlagged()
            SmartList.SCHEDULED -> repository.observeAll()
                .map { list -> list.filter { !it.isCompleted && it.dueDate != null && it.dueDate >= DateTimeUtils.endOfToday() } }
            SmartList.COMPLETED -> repository.observeAll().map { list -> list.filter { it.isCompleted } }
            SmartList.ALL -> repository.observeAll().map { list -> list.filter { !it.isCompleted } }
        }
    }

    fun selectSmartList(list: SmartList) { _filter.value = ReminderFilter.Smart(list) }

    fun selectUserList(listId: String) { _filter.value = ReminderFilter.UserList(listId) }

    fun toggleComplete(reminder: Reminder) = viewModelScope.launch {
        // Completing (transitioning from incomplete) also archives any linked focus block.
        if (!reminder.isCompleted) linkService.onReminderCompleted(reminder)
        repository.toggleComplete(reminder)
    }

    fun toggleFlag(reminder: Reminder) = viewModelScope.launch {
        repository.save(reminder.copy(isFlagged = !reminder.isFlagged))
    }

    fun delete(id: String) = viewModelScope.launch { repository.delete(id) }

    fun save(reminder: Reminder, blockCalendarTime: Boolean = false) = viewModelScope.launch {
        repository.save(reminder)
        if (blockCalendarTime && reminder.dueDate != null) {
            linkService.blockTimeForReminder(reminder)
        }
    }

    fun createList(name: String, color: String, icon: String) = viewModelScope.launch {
        repository.saveList(ReminderList(name = name, color = color, icon = icon))
    }

    fun observeSubtasks(reminderId: String) = repository.observeSubtasks(reminderId)

    fun defaultListId(): String? = lists.value.firstOrNull()?.id
}

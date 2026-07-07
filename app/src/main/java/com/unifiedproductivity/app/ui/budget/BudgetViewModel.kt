package com.unifiedproductivity.app.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unifiedproductivity.app.data.entity.BudgetItem
import com.unifiedproductivity.app.data.entity.BudgetList
import com.unifiedproductivity.app.data.repository.BudgetRepository
import com.unifiedproductivity.app.util.BudgetCalculations
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Aggregated state for the Budget screen: every list, its items, and running totals. */
data class BudgetUiState(
    val lists: List<BudgetList> = emptyList(),
    val itemsByList: Map<String, List<BudgetItem>> = emptyMap(),
    val outstandingIncome: Long = 0,
    val outstandingExpenses: Long = 0
)

class BudgetViewModel(private val repository: BudgetRepository) : ViewModel() {

    val uiState: StateFlow<BudgetUiState> =
        combine(repository.observeLists(), repository.observeAllItems()) { lists, items ->
            BudgetUiState(
                lists = lists,
                itemsByList = items.groupBy { it.listId },
                outstandingIncome = BudgetCalculations.outstandingIncome(items),
                outstandingExpenses = BudgetCalculations.outstandingExpenses(items)
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetUiState())

    fun createList(name: String) = viewModelScope.launch {
        repository.saveList(BudgetList(name = name))
    }

    fun deleteList(id: String) = viewModelScope.launch { repository.deleteList(id) }

    fun saveItem(item: BudgetItem) = viewModelScope.launch { repository.saveItem(item) }

    fun toggleComplete(item: BudgetItem) = viewModelScope.launch { repository.toggleComplete(item) }

    fun deleteItem(id: String) = viewModelScope.launch { repository.deleteItem(id) }
}

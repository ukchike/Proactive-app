package com.unifiedproductivity.app.util

import com.unifiedproductivity.app.data.entity.BudgetItem
import com.unifiedproductivity.app.data.model.BudgetItemType

/** Pure totals/progress math shared by the Budget screen and the Home dashboard. */
object BudgetCalculations {

    /** Sum of income items not yet marked received. */
    fun outstandingIncome(items: List<BudgetItem>): Long =
        items.filter { it.type == BudgetItemType.INCOME && !it.isCompleted }.sumOf { it.amount }

    /** Sum of expense items not yet marked paid. */
    fun outstandingExpenses(items: List<BudgetItem>): Long =
        items.filter { it.type == BudgetItemType.EXPENSE && !it.isCompleted }.sumOf { it.amount }

    data class ListProgress(
        val completedItems: Int,
        val totalItems: Int,
        val completedAmount: Long,
        val totalAmount: Long
    ) {
        val itemProgress: Float get() = if (totalItems > 0) completedItems.toFloat() / totalItems else 0f
        val amountProgress: Float get() = if (totalAmount > 0) completedAmount.toFloat() / totalAmount else 0f
    }

    /** Completion progress (by item count and by amount) for one list's items. */
    fun listProgress(items: List<BudgetItem>): ListProgress {
        val completed = items.filter { it.isCompleted }
        return ListProgress(
            completedItems = completed.size,
            totalItems = items.size,
            completedAmount = completed.sumOf { it.amount },
            totalAmount = items.sumOf { it.amount }
        )
    }
}

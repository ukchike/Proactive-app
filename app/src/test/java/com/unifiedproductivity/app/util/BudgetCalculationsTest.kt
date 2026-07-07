package com.unifiedproductivity.app.util

import com.unifiedproductivity.app.data.entity.BudgetItem
import com.unifiedproductivity.app.data.model.BudgetItemType
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetCalculationsTest {

    private fun item(
        amount: Long,
        type: BudgetItemType = BudgetItemType.EXPENSE,
        completed: Boolean = false
    ) = BudgetItem(listId = "list", amount = amount, type = type, isCompleted = completed)

    @Test
    fun `outstanding expenses sums only incomplete expense items`() {
        val items = listOf(
            item(1000, BudgetItemType.EXPENSE, completed = false),
            item(2000, BudgetItemType.EXPENSE, completed = true), // paid — excluded
            item(500, BudgetItemType.INCOME, completed = false)   // income — excluded
        )
        assertEquals(1000L, BudgetCalculations.outstandingExpenses(items))
    }

    @Test
    fun `outstanding income sums only incomplete income items`() {
        val items = listOf(
            item(3000, BudgetItemType.INCOME, completed = false),
            item(1000, BudgetItemType.INCOME, completed = true), // received — excluded
            item(200, BudgetItemType.EXPENSE, completed = false)
        )
        assertEquals(3000L, BudgetCalculations.outstandingIncome(items))
    }

    @Test
    fun `list progress tracks both item count and amount`() {
        val items = listOf(
            item(100, completed = true),
            item(300, completed = true),
            item(600, completed = false)
        )
        val progress = BudgetCalculations.listProgress(items)
        assertEquals(2, progress.completedItems)
        assertEquals(3, progress.totalItems)
        assertEquals(400L, progress.completedAmount)
        assertEquals(1000L, progress.totalAmount)
        assertEquals(400f / 1000f, progress.amountProgress, 0.0001f)
        assertEquals(2f / 3f, progress.itemProgress, 0.0001f)
    }

    @Test
    fun `empty list yields zero progress without dividing by zero`() {
        val progress = BudgetCalculations.listProgress(emptyList())
        assertEquals(0, progress.totalItems)
        assertEquals(0f, progress.itemProgress, 0.0001f)
        assertEquals(0f, progress.amountProgress, 0.0001f)
    }
}

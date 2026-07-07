package com.unifiedproductivity.app.data.repository

import com.unifiedproductivity.app.data.dao.BudgetItemDao
import com.unifiedproductivity.app.data.dao.BudgetListDao
import com.unifiedproductivity.app.data.entity.BudgetItem
import com.unifiedproductivity.app.data.entity.BudgetList
import kotlinx.coroutines.flow.Flow

/** Data-access layer for the Budget module. */
class BudgetRepository(
    private val listDao: BudgetListDao,
    private val itemDao: BudgetItemDao
) {
    fun observeLists(): Flow<List<BudgetList>> = listDao.observeAll()

    suspend fun saveList(list: BudgetList) = listDao.upsert(list)

    suspend fun deleteList(id: String) = listDao.softDelete(id)

    // ----- Items -----

    /** Every item across all lists — used for dashboard/Home totals. */
    fun observeAllItems(): Flow<List<BudgetItem>> = itemDao.observeAll()

    fun observeItemsInList(listId: String): Flow<List<BudgetItem>> = itemDao.observeByList(listId)

    /** Items due in [from, to) — surfaced on the Calendar alongside events. */
    fun observeItemsDueBetween(from: Long, to: Long): Flow<List<BudgetItem>> =
        itemDao.observeDueBetween(from, to)

    suspend fun saveItem(item: BudgetItem) =
        itemDao.upsert(item.copy(modifiedAt = System.currentTimeMillis()))

    suspend fun toggleComplete(item: BudgetItem) =
        itemDao.upsert(item.copy(isCompleted = !item.isCompleted, modifiedAt = System.currentTimeMillis()))

    suspend fun deleteItem(id: String) = itemDao.softDelete(id)
}

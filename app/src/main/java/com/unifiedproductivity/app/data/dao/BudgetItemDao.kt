package com.unifiedproductivity.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.unifiedproductivity.app.data.entity.BudgetItem
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetItemDao {

    /** All non-deleted items across every list — used for dashboard totals. */
    @Query("SELECT * FROM budget_items WHERE deletedAt IS NULL")
    fun observeAll(): Flow<List<BudgetItem>>

    @Query("SELECT * FROM budget_items WHERE deletedAt IS NULL AND listId = :listId ORDER BY createdAt")
    fun observeByList(listId: String): Flow<List<BudgetItem>>

    /** Items due within [from, to) — surfaced on the Calendar alongside events. */
    @Query(
        "SELECT * FROM budget_items WHERE deletedAt IS NULL AND dueDate IS NOT NULL " +
            "AND dueDate >= :from AND dueDate < :to ORDER BY dueDate"
    )
    fun observeDueBetween(from: Long, to: Long): Flow<List<BudgetItem>>

    @Query("SELECT * FROM budget_items")
    suspend fun getAllRaw(): List<BudgetItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: BudgetItem)

    @Update
    suspend fun update(item: BudgetItem)

    @Query("UPDATE budget_items SET deletedAt = :timestamp, modifiedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())
}

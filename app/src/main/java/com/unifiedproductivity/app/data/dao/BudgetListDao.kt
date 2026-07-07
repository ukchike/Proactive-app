package com.unifiedproductivity.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.unifiedproductivity.app.data.entity.BudgetList
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetListDao {

    @Query("SELECT * FROM budget_lists WHERE deletedAt IS NULL ORDER BY createdAt")
    fun observeAll(): Flow<List<BudgetList>>

    @Query("SELECT * FROM budget_lists")
    suspend fun getAllRaw(): List<BudgetList>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(list: BudgetList)

    @Update
    suspend fun update(list: BudgetList)

    @Query("UPDATE budget_lists SET deletedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())
}

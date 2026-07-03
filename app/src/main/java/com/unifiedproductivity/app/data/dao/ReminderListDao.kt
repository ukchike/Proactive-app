package com.unifiedproductivity.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.unifiedproductivity.app.data.entity.ReminderList
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderListDao {

    @Query("SELECT * FROM reminder_lists WHERE deletedAt IS NULL ORDER BY createdAt")
    fun observeAll(): Flow<List<ReminderList>>

    @Query("SELECT COUNT(*) FROM reminder_lists WHERE deletedAt IS NULL")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(list: ReminderList)

    @Update
    suspend fun update(list: ReminderList)

    @Query("UPDATE reminder_lists SET deletedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())
}

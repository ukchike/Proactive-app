package com.unifiedproductivity.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.unifiedproductivity.app.data.entity.CalendarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarDao {

    @Query("SELECT * FROM calendars WHERE deletedAt IS NULL ORDER BY createdAt")
    fun observeAll(): Flow<List<CalendarEntity>>

    @Query("SELECT COUNT(*) FROM calendars WHERE deletedAt IS NULL")
    suspend fun count(): Int

    @Query("SELECT * FROM calendars WHERE name = :name AND deletedAt IS NULL LIMIT 1")
    suspend fun findByName(name: String): CalendarEntity?

    @Query("SELECT * FROM calendars WHERE deletedAt IS NULL ORDER BY createdAt LIMIT 1")
    suspend fun getFirst(): CalendarEntity?

    @Query("SELECT * FROM calendars")
    suspend fun getAllRaw(): List<CalendarEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(calendar: CalendarEntity)

    @Update
    suspend fun update(calendar: CalendarEntity)

    @Query("UPDATE calendars SET deletedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())
}

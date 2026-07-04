package com.unifiedproductivity.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.unifiedproductivity.app.data.entity.Reminder
import com.unifiedproductivity.app.data.entity.Subtask
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE deletedAt IS NULL ORDER BY isCompleted, priority, dueDate IS NULL, dueDate")
    fun observeAll(): Flow<List<Reminder>>

    @Query(
        "SELECT * FROM reminders WHERE deletedAt IS NULL AND listId = :listId " +
            "ORDER BY isCompleted, priority, dueDate IS NULL, dueDate"
    )
    fun observeByList(listId: String): Flow<List<Reminder>>

    @Query(
        "SELECT * FROM reminders WHERE deletedAt IS NULL AND isCompleted = 0 AND dueDate IS NOT NULL " +
            "AND dueDate BETWEEN :startOfDay AND :endOfDay ORDER BY dueDate"
    )
    fun observeDueBetween(startOfDay: Long, endOfDay: Long): Flow<List<Reminder>>

    @Query(
        "SELECT * FROM reminders WHERE deletedAt IS NULL AND isCompleted = 0 " +
            "AND dueDate IS NOT NULL AND dueDate < :now ORDER BY dueDate"
    )
    fun observeOverdue(now: Long): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE deletedAt IS NULL AND isFlagged = 1 AND isCompleted = 0 ORDER BY dueDate")
    fun observeFlagged(): Flow<List<Reminder>>

    /** High-priority or flagged open tasks, surfaced on the Home dashboard. */
    @Query(
        "SELECT * FROM reminders WHERE deletedAt IS NULL AND isCompleted = 0 " +
            "AND (priority = 'HIGH' OR isFlagged = 1) " +
            "ORDER BY dueDate IS NULL, dueDate"
    )
    fun observeHighPriority(): Flow<List<Reminder>>

    @Query(
        "SELECT * FROM reminders WHERE deletedAt IS NULL " +
            "AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') " +
            "ORDER BY isCompleted, priority"
    )
    fun search(query: String): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Reminder?

    /** Non-completed reminders whose due date is still in the future (for alarm rescheduling). */
    @Query(
        "SELECT * FROM reminders WHERE deletedAt IS NULL AND isCompleted = 0 " +
            "AND dueDate IS NOT NULL AND dueDate > :now"
    )
    suspend fun getUpcoming(now: Long): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<Reminder?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: Reminder)

    @Update
    suspend fun update(reminder: Reminder)

    @Query("UPDATE reminders SET deletedAt = :timestamp, modifiedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())

    // ----- Subtasks -----

    @Query("SELECT * FROM subtasks WHERE reminderId = :reminderId ORDER BY position")
    fun observeSubtasks(reminderId: String): Flow<List<Subtask>>

    @Query("SELECT * FROM subtasks WHERE reminderId = :reminderId ORDER BY position")
    suspend fun getSubtasks(reminderId: String): List<Subtask>

    @Query("SELECT * FROM reminders")
    suspend fun getAllRaw(): List<Reminder>

    @Query("SELECT * FROM subtasks")
    suspend fun getAllSubtasksRaw(): List<Subtask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubtask(subtask: Subtask)

    @Query("DELETE FROM subtasks WHERE id = :id")
    suspend fun deleteSubtask(id: String)

    @Transaction
    suspend fun replaceSubtasks(reminderId: String, subtasks: List<Subtask>) {
        clearSubtasks(reminderId)
        subtasks.forEach { upsertSubtask(it) }
    }

    @Query("DELETE FROM subtasks WHERE reminderId = :reminderId")
    suspend fun clearSubtasks(reminderId: String)
}

package com.unifiedproductivity.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.unifiedproductivity.app.data.entity.Event
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    /** Events overlapping the [from, to) window, from visible calendars only. */
    @Query(
        "SELECT e.* FROM events e JOIN calendars c ON e.calendarId = c.id " +
            "WHERE e.deletedAt IS NULL AND c.isVisible = 1 " +
            "AND e.startDateTime < :to AND e.endDateTime >= :from " +
            "ORDER BY e.startDateTime"
    )
    fun observeInRange(from: Long, to: Long): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE deletedAt IS NULL ORDER BY startDateTime")
    fun observeAll(): Flow<List<Event>>

    @Query(
        "SELECT * FROM events WHERE deletedAt IS NULL " +
            "AND (title LIKE '%' || :query || '%' OR location LIKE '%' || :query || '%') " +
            "ORDER BY startDateTime"
    )
    fun search(query: String): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Event?

    @Query("SELECT * FROM events")
    suspend fun getAllRaw(): List<Event>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<Event?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: Event)

    @Update
    suspend fun update(event: Event)

    @Query("UPDATE events SET deletedAt = :timestamp, modifiedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())
}

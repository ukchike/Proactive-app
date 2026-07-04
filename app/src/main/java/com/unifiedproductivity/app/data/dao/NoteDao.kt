package com.unifiedproductivity.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.unifiedproductivity.app.data.entity.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query(
        "SELECT * FROM notes WHERE deletedAt IS NULL AND isArchived = 0 " +
            "ORDER BY isPinned DESC, modifiedAt DESC"
    )
    fun observeAll(): Flow<List<Note>>

    @Query(
        "SELECT * FROM notes WHERE deletedAt IS NULL AND isArchived = 0 " +
            "AND (folderId = :folderId OR (:folderId IS NULL AND folderId IS NULL)) " +
            "ORDER BY isPinned DESC, modifiedAt DESC"
    )
    fun observeByFolder(folderId: String?): Flow<List<Note>>

    @Query(
        "SELECT * FROM notes WHERE deletedAt IS NULL AND isArchived = 0 " +
            "AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') " +
            "ORDER BY isPinned DESC, modifiedAt DESC"
    )
    fun search(query: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Note?

    /** Every row including archived/soft-deleted — used for Drive backup/sync. */
    @Query("SELECT * FROM notes")
    suspend fun getAllRaw(): List<Note>

    /** The meeting note linked to a calendar event, if one exists. */
    @Query("SELECT * FROM notes WHERE linkedEventId = :eventId AND deletedAt IS NULL LIMIT 1")
    suspend fun findByLinkedEvent(eventId: String): Note?

    /** Urgent-quadrant notes, surfaced on the Home dashboard. */
    @Query(
        "SELECT * FROM notes WHERE deletedAt IS NULL AND isArchived = 0 " +
            "AND eisenhower IN ('URGENT_IMPORTANT', 'URGENT_NOT_IMPORTANT') " +
            "ORDER BY modifiedAt DESC"
    )
    fun observeUrgent(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<Note?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: Note)

    @Update
    suspend fun update(note: Note)

    /** Soft delete so the removal can sync to other devices. */
    @Query("UPDATE notes SET deletedAt = :timestamp, modifiedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())
}

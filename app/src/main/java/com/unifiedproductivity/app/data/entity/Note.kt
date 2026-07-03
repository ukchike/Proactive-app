package com.unifiedproductivity.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.unifiedproductivity.app.data.model.NoteType
import java.util.UUID

/**
 * A single note. Content is stored as Markdown text for the MVP (rich-text WYSIWYG
 * is a later phase). Soft deletes ([deletedAt]) let deletions propagate across
 * devices during Google Drive sync.
 */
@Entity(
    tableName = "notes",
    indices = [Index("folderId"), Index("modifiedAt")]
)
data class Note(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val folderId: String? = null,
    val tags: List<String> = emptyList(),
    val type: NoteType = NoteType.FREE_FORM,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    /** Optional bidirectional link to a calendar event (meeting notes). */
    val linkedEventId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

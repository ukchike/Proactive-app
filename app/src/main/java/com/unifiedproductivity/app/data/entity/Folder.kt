package com.unifiedproductivity.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

/** A note folder. Supports unlimited nesting via [parentFolderId]. */
@Serializable
@Entity(
    tableName = "folders",
    indices = [Index("parentFolderId")]
)
data class Folder(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val parentFolderId: String? = null,
    /** Optional hex color, e.g. "#4A6FA5". */
    val color: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

package com.unifiedproductivity.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/** A user-created list that reminders belong to (e.g. "Tax Deadlines"). */
@Entity(tableName = "reminder_lists")
data class ReminderList(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    /** Hex color for visual categorization. */
    val color: String = "#FF9500",
    /** Emoji shown next to the list name. */
    val icon: String = "📋",
    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

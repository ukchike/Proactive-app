package com.unifiedproductivity.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

/** A calendar that events belong to (Work, Personal, Academic, Focus Time, ...). */
@Serializable
@Entity(tableName = "calendars")
data class CalendarEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val color: String = "#009688",
    val description: String? = null,
    val isVisible: Boolean = true,
    val isShared: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

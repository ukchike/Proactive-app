package com.unifiedproductivity.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.unifiedproductivity.app.data.model.RecurrenceFrequency
import java.util.UUID

/**
 * A calendar event. [startDateTime]/[endDateTime] are epoch millis. All-day events
 * span the day of [startDateTime] and ignore the time component.
 */
@Entity(
    tableName = "events",
    indices = [Index("calendarId"), Index("startDateTime")]
)
data class Event(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val calendarId: String,
    val title: String = "",
    val description: String = "",
    val startDateTime: Long,
    val endDateTime: Long,
    val isAllDay: Boolean = false,
    val timeZone: String = "Africa/Lagos",
    val location: String? = null,
    /** Optional hex color override; falls back to the calendar's color when null. */
    val color: String? = null,
    val recurrence: RecurrenceFrequency = RecurrenceFrequency.NONE,
    val recurrenceInterval: Int = 1,
    val reminderMinutesBefore: List<String> = emptyList(),
    val attendees: List<String> = emptyList(),
    /** Optional bidirectional link back to a reminder (focus-time block). */
    val linkedReminderId: String? = null,
    val travelTimeMinutes: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

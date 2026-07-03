package com.unifiedproductivity.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.unifiedproductivity.app.data.model.Priority
import com.unifiedproductivity.app.data.model.RecurrenceFrequency
import com.unifiedproductivity.app.sync.ConflictResolver
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A task/reminder. [dueDate] is an epoch-millis timestamp; when [hasTime] is false
 * the time component is ignored (all-day due date). Dependencies are modeled with
 * [blockedBy]: a reminder is "blocked" while any id it lists is still incomplete.
 */
@Serializable
@Entity(
    tableName = "reminders",
    indices = [Index("listId"), Index("dueDate"), Index("isCompleted")]
)
data class Reminder(
    @PrimaryKey override val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val listId: String,
    val dueDate: Long? = null,
    /** Whether [dueDate] carries a meaningful time-of-day. */
    val hasTime: Boolean = false,
    val priority: Priority = Priority.NONE,
    val isFlagged: Boolean = false,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val tags: List<String> = emptyList(),
    /** Ids of reminders that must complete before this one is unblocked. */
    val blockedBy: List<String> = emptyList(),
    /** Minutes before [dueDate] to fire a local notification, empty = none. */
    val notifyMinutesBefore: List<String> = emptyList(),
    val estimatedMinutes: Int? = null,
    val actualMinutes: Int? = null,
    val recurrence: RecurrenceFrequency = RecurrenceFrequency.NONE,
    val recurrenceInterval: Int = 1,
    /** Free-form place/address (e.g. "Client office, Victoria Island"). */
    val location: String? = null,
    /** Optional link to a calendar "focus time" block. */
    val linkedEventId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    override val modifiedAt: Long = System.currentTimeMillis(),
    override val deletedAt: Long? = null
) : ConflictResolver.Versioned

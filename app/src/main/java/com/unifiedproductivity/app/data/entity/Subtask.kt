package com.unifiedproductivity.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** A checklist item belonging to a [Reminder]. Deleted with its parent. */
@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = Reminder::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("reminderId")]
)
data class Subtask(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val reminderId: String,
    val title: String = "",
    val isCompleted: Boolean = false,
    val position: Int = 0
)

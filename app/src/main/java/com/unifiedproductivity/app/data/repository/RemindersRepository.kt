package com.unifiedproductivity.app.data.repository

import com.unifiedproductivity.app.data.dao.ReminderDao
import com.unifiedproductivity.app.data.dao.ReminderListDao
import com.unifiedproductivity.app.data.entity.Reminder
import com.unifiedproductivity.app.data.entity.ReminderList
import com.unifiedproductivity.app.data.entity.Subtask
import com.unifiedproductivity.app.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow

/** Data-access layer for the Reminders module, including smart-list queries. */
class RemindersRepository(
    private val reminderDao: ReminderDao,
    private val listDao: ReminderListDao
) {
    fun observeAll(): Flow<List<Reminder>> = reminderDao.observeAll()

    fun observeByList(listId: String): Flow<List<Reminder>> = reminderDao.observeByList(listId)

    fun observeToday(): Flow<List<Reminder>> {
        val start = DateTimeUtils.startOfToday()
        return reminderDao.observeDueBetween(start, DateTimeUtils.endOfToday())
    }

    fun observeOverdue(): Flow<List<Reminder>> =
        reminderDao.observeOverdue(DateTimeUtils.startOfToday())

    fun observeFlagged(): Flow<List<Reminder>> = reminderDao.observeFlagged()

    fun search(query: String): Flow<List<Reminder>> = reminderDao.search(query.trim())

    fun observeReminder(id: String): Flow<Reminder?> = reminderDao.observeById(id)

    suspend fun getReminder(id: String): Reminder? = reminderDao.getById(id)

    /** Upcoming, incomplete reminders with a future due date — used to reschedule alarms. */
    suspend fun getUpcoming(): List<Reminder> =
        reminderDao.getUpcoming(System.currentTimeMillis())

    suspend fun save(reminder: Reminder) =
        reminderDao.upsert(reminder.copy(modifiedAt = System.currentTimeMillis()))

    suspend fun delete(id: String) = reminderDao.softDelete(id)

    /** Toggle completion. Recurring tasks roll forward to the next occurrence. */
    suspend fun toggleComplete(reminder: Reminder) {
        if (!reminder.isCompleted && reminder.recurrence != com.unifiedproductivity.app.data.model.RecurrenceFrequency.NONE) {
            val next = DateTimeUtils.nextOccurrence(
                reminder.dueDate,
                reminder.recurrence,
                reminder.recurrenceInterval
            )
            reminderDao.upsert(
                reminder.copy(dueDate = next, modifiedAt = System.currentTimeMillis())
            )
        } else {
            val completed = !reminder.isCompleted
            reminderDao.upsert(
                reminder.copy(
                    isCompleted = completed,
                    completedAt = if (completed) System.currentTimeMillis() else null,
                    modifiedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /** Whether every reminder listed in [Reminder.blockedBy] is complete. */
    suspend fun isBlocked(reminder: Reminder): Boolean {
        if (reminder.blockedBy.isEmpty()) return false
        return reminder.blockedBy.any { blockerId ->
            reminderDao.getById(blockerId)?.isCompleted == false
        }
    }

    // ----- Subtasks -----

    fun observeSubtasks(reminderId: String): Flow<List<Subtask>> =
        reminderDao.observeSubtasks(reminderId)

    suspend fun saveSubtask(subtask: Subtask) = reminderDao.upsertSubtask(subtask)

    suspend fun deleteSubtask(id: String) = reminderDao.deleteSubtask(id)

    suspend fun replaceSubtasks(reminderId: String, subtasks: List<Subtask>) =
        reminderDao.replaceSubtasks(reminderId, subtasks)

    // ----- Lists -----

    fun observeLists(): Flow<List<ReminderList>> = listDao.observeAll()

    suspend fun saveList(list: ReminderList) = listDao.upsert(list)

    suspend fun deleteList(id: String) = listDao.softDelete(id)

    suspend fun listCount(): Int = listDao.count()
}

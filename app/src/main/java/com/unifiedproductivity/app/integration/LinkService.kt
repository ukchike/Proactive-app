package com.unifiedproductivity.app.integration

import com.unifiedproductivity.app.data.entity.CalendarEntity
import com.unifiedproductivity.app.data.entity.Event
import com.unifiedproductivity.app.data.entity.Note
import com.unifiedproductivity.app.data.entity.Reminder
import com.unifiedproductivity.app.data.model.NoteType
import com.unifiedproductivity.app.data.repository.CalendarRepository
import com.unifiedproductivity.app.data.repository.NotesRepository
import com.unifiedproductivity.app.data.repository.RemindersRepository
import com.unifiedproductivity.app.util.DateTimeUtils

/**
 * The cross-module glue described in the spec's "Cross-Module Integration" section:
 *   - Reminder  ->  Calendar focus-time block
 *   - Calendar event  ->  Meeting note
 * Links are bidirectional: each side stores the other's id.
 */
class LinkService(
    private val reminders: RemindersRepository,
    private val calendar: CalendarRepository,
    private val notes: NotesRepository
) {
    companion object {
        const val FOCUS_CALENDAR_NAME = "Focus Time"
        private const val DEFAULT_BLOCK_MINUTES = 30
        private const val PRIORITY_HIGH_COLOR = "#FF3B30"
        private const val PRIORITY_MEDIUM_COLOR = "#FF9500"
        private const val PRIORITY_LOW_COLOR = "#007AFF"
    }

    /**
     * Block time on the calendar for a reminder with a due date. Creates (or reuses)
     * the dedicated "Focus Time" calendar and links both entities together.
     */
    suspend fun blockTimeForReminder(reminder: Reminder): Event? {
        val due = reminder.dueDate ?: return null
        val focusCalendar = ensureFocusCalendar()

        val durationMs = (reminder.estimatedMinutes ?: DEFAULT_BLOCK_MINUTES) * 60_000L
        val start = due
        val event = Event(
            calendarId = focusCalendar.id,
            title = "[TASK] ${reminder.title}",
            description = reminder.description,
            startDateTime = start,
            endDateTime = start + durationMs,
            color = colorForPriority(reminder.priority),
            linkedReminderId = reminder.id
        )
        calendar.saveEvent(event)
        reminders.save(reminder.copy(linkedEventId = event.id))
        return event
    }

    /** When a reminder is completed, archive (soft-delete) its focus-time block. */
    suspend fun onReminderCompleted(reminder: Reminder) {
        reminder.linkedEventId?.let { calendar.deleteEvent(it) }
    }

    /**
     * Create a meeting note pre-filled from an event and link the two. Returns the
     * new note so the caller can open the editor.
     */
    suspend fun createNoteForEvent(event: Event): Note {
        val note = Note(
            title = "${event.title} — ${DateTimeUtils.formatDate(event.startDateTime)}",
            type = NoteType.MEETING,
            content = buildString {
                appendLine("# ${event.title}")
                appendLine()
                appendLine("Date: ${DateTimeUtils.formatDate(event.startDateTime)}")
                appendLine("Time: ${DateTimeUtils.formatTime(event.startDateTime)}")
                if (!event.location.isNullOrBlank()) appendLine("Location: ${event.location}")
                if (event.attendees.isNotEmpty()) {
                    appendLine("Attendees: ${event.attendees.joinToString(", ")}")
                }
                appendLine()
                appendLine("## Notes")
                appendLine()
            },
            linkedEventId = event.id
        )
        notes.saveNote(note)
        return note
    }

    private suspend fun ensureFocusCalendar(): CalendarEntity {
        calendar.findCalendarByName(FOCUS_CALENDAR_NAME)?.let { return it }
        val created = CalendarEntity(name = FOCUS_CALENDAR_NAME, color = "#5856D6")
        calendar.saveCalendar(created)
        return created
    }

    private fun colorForPriority(priority: com.unifiedproductivity.app.data.model.Priority): String =
        when (priority) {
            com.unifiedproductivity.app.data.model.Priority.HIGH -> PRIORITY_HIGH_COLOR
            com.unifiedproductivity.app.data.model.Priority.MEDIUM -> PRIORITY_MEDIUM_COLOR
            else -> PRIORITY_LOW_COLOR
        }
}

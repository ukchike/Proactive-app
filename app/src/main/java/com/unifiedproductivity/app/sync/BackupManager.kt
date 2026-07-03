package com.unifiedproductivity.app.sync

import com.unifiedproductivity.app.data.AppDatabase
import com.unifiedproductivity.app.data.entity.CalendarEntity
import com.unifiedproductivity.app.data.entity.Event
import com.unifiedproductivity.app.data.entity.Folder
import com.unifiedproductivity.app.data.entity.Note
import com.unifiedproductivity.app.data.entity.Reminder
import com.unifiedproductivity.app.data.entity.ReminderList
import com.unifiedproductivity.app.data.entity.Subtask
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Turns the local database into portable JSON snapshots and merges remote snapshots
 * back in (last-write-wins via [ConflictResolver]). The three files mirror the layout
 * the spec describes under `/Unified Productivity App/sync/`.
 */
class BackupManager(private val db: AppDatabase) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }

    @Serializable
    data class NotesSnapshot(val notes: List<Note> = emptyList(), val folders: List<Folder> = emptyList())

    @Serializable
    data class RemindersSnapshot(
        val reminders: List<Reminder> = emptyList(),
        val subtasks: List<Subtask> = emptyList(),
        val lists: List<ReminderList> = emptyList()
    )

    @Serializable
    data class CalendarSnapshot(
        val calendars: List<CalendarEntity> = emptyList(),
        val events: List<Event> = emptyList()
    )

    companion object {
        const val NOTES_FILE = "notes.json"
        const val REMINDERS_FILE = "reminders.json"
        const val CALENDAR_FILE = "calendar.json"
    }

    /** Snapshot the whole local database as filename -> JSON (used for backups). */
    suspend fun exportSnapshots(): Map<String, String> = mapOf(
        NOTES_FILE to json.encodeToString(
            NotesSnapshot(db.noteDao().getAllRaw(), db.folderDao().getAllRaw())
        ),
        REMINDERS_FILE to json.encodeToString(
            RemindersSnapshot(
                db.reminderDao().getAllRaw(),
                db.reminderDao().getAllSubtasksRaw(),
                db.reminderListDao().getAllRaw()
            )
        ),
        CALENDAR_FILE to json.encodeToString(
            CalendarSnapshot(db.calendarDao().getAllRaw(), db.eventDao().getAllRaw())
        )
    )

    /**
     * Merge remote JSON snapshots with the local database, apply the merged result
     * locally, and return the merged snapshots to push back to Drive.
     */
    suspend fun mergeAndApply(remoteFiles: Map<String, String?>): Map<String, String> {
        // ----- Notes + Folders -----
        val remoteNotes = remoteFiles[NOTES_FILE]?.let { json.decodeFromString<NotesSnapshot>(it) }
        val mergedNotes = ConflictResolver.merge(db.noteDao().getAllRaw(), remoteNotes?.notes.orEmpty())
        val mergedFolders = unionById(
            db.folderDao().getAllRaw(), remoteNotes?.folders.orEmpty(), { it.id }, { it.deletedAt }
        )
        mergedNotes.forEach { db.noteDao().upsert(it) }
        mergedFolders.forEach { db.folderDao().upsert(it) }

        // ----- Reminders + Subtasks + Lists -----
        val remoteReminders = remoteFiles[REMINDERS_FILE]?.let { json.decodeFromString<RemindersSnapshot>(it) }
        val mergedReminders = ConflictResolver.merge(db.reminderDao().getAllRaw(), remoteReminders?.reminders.orEmpty())
        val mergedLists = unionById(
            db.reminderListDao().getAllRaw(), remoteReminders?.lists.orEmpty(), { it.id }, { it.deletedAt }
        )
        val mergedSubtasks = unionById(
            db.reminderDao().getAllSubtasksRaw(), remoteReminders?.subtasks.orEmpty(), { it.id }, { null }
        )
        mergedLists.forEach { db.reminderListDao().upsert(it) }
        mergedReminders.forEach { db.reminderDao().upsert(it) }
        // Only re-insert subtasks whose parent reminder still exists (FK safety).
        val reminderIds = mergedReminders.map { it.id }.toHashSet()
        mergedSubtasks.filter { it.reminderId in reminderIds }.forEach { db.reminderDao().upsertSubtask(it) }

        // ----- Calendars + Events -----
        val remoteCal = remoteFiles[CALENDAR_FILE]?.let { json.decodeFromString<CalendarSnapshot>(it) }
        val mergedCalendars = unionById(
            db.calendarDao().getAllRaw(), remoteCal?.calendars.orEmpty(), { it.id }, { it.deletedAt }
        )
        val mergedEvents = ConflictResolver.merge(db.eventDao().getAllRaw(), remoteCal?.events.orEmpty())
        mergedCalendars.forEach { db.calendarDao().upsert(it) }
        mergedEvents.forEach { db.eventDao().upsert(it) }

        return mapOf(
            NOTES_FILE to json.encodeToString(NotesSnapshot(mergedNotes, mergedFolders)),
            REMINDERS_FILE to json.encodeToString(RemindersSnapshot(mergedReminders, mergedSubtasks, mergedLists)),
            CALENDAR_FILE to json.encodeToString(CalendarSnapshot(mergedCalendars, mergedEvents))
        )
    }

    /** Replace local data with the given remote snapshots (used by "Restore"). */
    suspend fun restoreFrom(remoteFiles: Map<String, String?>) {
        remoteFiles[NOTES_FILE]?.let { json.decodeFromString<NotesSnapshot>(it) }?.let { snap ->
            snap.folders.forEach { db.folderDao().upsert(it) }
            snap.notes.forEach { db.noteDao().upsert(it) }
        }
        remoteFiles[REMINDERS_FILE]?.let { json.decodeFromString<RemindersSnapshot>(it) }?.let { snap ->
            snap.lists.forEach { db.reminderListDao().upsert(it) }
            snap.reminders.forEach { db.reminderDao().upsert(it) }
            val ids = snap.reminders.map { it.id }.toHashSet()
            snap.subtasks.filter { it.reminderId in ids }.forEach { db.reminderDao().upsertSubtask(it) }
        }
        remoteFiles[CALENDAR_FILE]?.let { json.decodeFromString<CalendarSnapshot>(it) }?.let { snap ->
            snap.calendars.forEach { db.calendarDao().upsert(it) }
            snap.events.forEach { db.eventDao().upsert(it) }
        }
    }

    /**
     * Union two id-keyed lists for entities that don't carry a modifiedAt timestamp.
     * Prefers the local copy, adds remote-only rows, and lets a remote soft-delete win.
     */
    private fun <T> unionById(
        local: List<T>,
        remote: List<T>,
        id: (T) -> String,
        deletedAt: (T) -> Long?
    ): List<T> {
        val map = LinkedHashMap<String, T>()
        local.forEach { map[id(it)] = it }
        remote.forEach { r ->
            val existing = map[id(r)]
            map[id(r)] = when {
                existing == null -> r
                deletedAt(r) != null && deletedAt(existing) == null -> r
                else -> existing
            }
        }
        return map.values.toList()
    }
}

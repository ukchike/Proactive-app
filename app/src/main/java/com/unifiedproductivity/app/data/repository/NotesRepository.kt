package com.unifiedproductivity.app.data.repository

import com.unifiedproductivity.app.data.dao.FolderDao
import com.unifiedproductivity.app.data.dao.NoteDao
import com.unifiedproductivity.app.data.entity.Folder
import com.unifiedproductivity.app.data.entity.Note
import kotlinx.coroutines.flow.Flow

/** Data-access layer for the Notes module. */
class NotesRepository(
    private val noteDao: NoteDao,
    private val folderDao: FolderDao
) {
    fun observeNotes(): Flow<List<Note>> = noteDao.observeAll()

    fun observeNotesInFolder(folderId: String?): Flow<List<Note>> =
        noteDao.observeByFolder(folderId)

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.search(query.trim())

    fun observeNote(id: String): Flow<Note?> = noteDao.observeById(id)

    suspend fun getNote(id: String): Note? = noteDao.getById(id)

    suspend fun saveNote(note: Note) {
        noteDao.upsert(note.copy(modifiedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNote(id: String) = noteDao.softDelete(id)

    suspend fun togglePin(note: Note) =
        noteDao.update(note.copy(isPinned = !note.isPinned, modifiedAt = System.currentTimeMillis()))

    // ----- Folders -----

    fun observeFolders(): Flow<List<Folder>> = folderDao.observeAll()

    suspend fun saveFolder(folder: Folder) = folderDao.upsert(folder)

    suspend fun deleteFolder(id: String) = folderDao.softDelete(id)
}

package com.unifiedproductivity.app.di

import android.content.Context
import com.unifiedproductivity.app.data.AppDatabase
import com.unifiedproductivity.app.data.repository.CalendarRepository
import com.unifiedproductivity.app.data.repository.NotesRepository
import com.unifiedproductivity.app.data.repository.RemindersRepository
import com.unifiedproductivity.app.integration.LinkService

/**
 * Lightweight manual dependency container. Avoids pulling in a DI framework for
 * the MVP while still giving every screen a single shared graph of repositories.
 */
class AppContainer(context: Context) {

    private val database = AppDatabase.getInstance(context)

    val notesRepository = NotesRepository(database.noteDao(), database.folderDao())

    val remindersRepository =
        RemindersRepository(database.reminderDao(), database.reminderListDao())

    val calendarRepository =
        CalendarRepository(database.calendarDao(), database.eventDao())

    val linkService = LinkService(remindersRepository, calendarRepository, notesRepository)
}

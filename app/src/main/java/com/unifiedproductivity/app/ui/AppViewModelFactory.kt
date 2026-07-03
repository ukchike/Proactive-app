package com.unifiedproductivity.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.unifiedproductivity.app.di.AppContainer
import com.unifiedproductivity.app.ui.calendar.CalendarViewModel
import com.unifiedproductivity.app.ui.home.HomeViewModel
import com.unifiedproductivity.app.ui.notes.NotesViewModel
import com.unifiedproductivity.app.ui.reminders.RemindersViewModel
import com.unifiedproductivity.app.ui.settings.SettingsViewModel

/** Builds the module ViewModels, injecting repositories from [AppContainer]. */
class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(NotesViewModel::class.java) ->
            NotesViewModel(container.notesRepository) as T

        modelClass.isAssignableFrom(RemindersViewModel::class.java) ->
            RemindersViewModel(
                container.remindersRepository,
                container.linkService,
                container.reminderScheduler
            ) as T

        modelClass.isAssignableFrom(CalendarViewModel::class.java) ->
            CalendarViewModel(container.calendarRepository, container.linkService) as T

        modelClass.isAssignableFrom(HomeViewModel::class.java) ->
            HomeViewModel(
                container.remindersRepository,
                container.calendarRepository
            ) as T

        modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
            SettingsViewModel(container.driveSyncManager) as T

        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}

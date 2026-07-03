package com.unifiedproductivity.app

import android.app.Application
import com.unifiedproductivity.app.di.AppContainer
import com.unifiedproductivity.app.data.entity.CalendarEntity
import com.unifiedproductivity.app.data.entity.ReminderList
import com.unifiedproductivity.app.sync.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Application entry point. Owns the [AppContainer] and seeds default data. */
class UnifiedProductivityApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        seedDefaults()
        SyncWorker.schedule(this)
    }

    /** Create a starter reminder list and calendar on first launch. */
    private fun seedDefaults() {
        appScope.launch {
            if (container.remindersRepository.listCount() == 0) {
                container.remindersRepository.saveList(
                    ReminderList(name = "Reminders", color = "#FF9500", icon = "📋")
                )
            }
            if (container.calendarRepository.calendarCount() == 0) {
                container.calendarRepository.saveCalendar(
                    CalendarEntity(name = "Personal", color = "#009688")
                )
                container.calendarRepository.saveCalendar(
                    CalendarEntity(name = "Work", color = "#4A6FA5")
                )
            }
        }
    }
}

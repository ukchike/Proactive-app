package com.unifiedproductivity.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.unifiedproductivity.app.data.dao.CalendarDao
import com.unifiedproductivity.app.data.dao.EventDao
import com.unifiedproductivity.app.data.dao.FolderDao
import com.unifiedproductivity.app.data.dao.NoteDao
import com.unifiedproductivity.app.data.dao.ReminderDao
import com.unifiedproductivity.app.data.dao.ReminderListDao
import com.unifiedproductivity.app.data.entity.CalendarEntity
import com.unifiedproductivity.app.data.entity.Event
import com.unifiedproductivity.app.data.entity.Folder
import com.unifiedproductivity.app.data.entity.Note
import com.unifiedproductivity.app.data.entity.Reminder
import com.unifiedproductivity.app.data.entity.ReminderList
import com.unifiedproductivity.app.data.entity.Subtask

@Database(
    entities = [
        Note::class,
        Folder::class,
        Reminder::class,
        Subtask::class,
        ReminderList::class,
        CalendarEntity::class,
        Event::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun reminderDao(): ReminderDao
    abstract fun reminderListDao(): ReminderListDao
    abstract fun calendarDao(): CalendarDao
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "unified_productivity.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}

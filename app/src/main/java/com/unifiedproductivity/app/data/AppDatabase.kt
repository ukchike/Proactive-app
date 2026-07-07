package com.unifiedproductivity.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.unifiedproductivity.app.data.dao.BudgetItemDao
import com.unifiedproductivity.app.data.dao.BudgetListDao
import com.unifiedproductivity.app.data.dao.CalendarDao
import com.unifiedproductivity.app.data.dao.EventDao
import com.unifiedproductivity.app.data.dao.FolderDao
import com.unifiedproductivity.app.data.dao.NoteDao
import com.unifiedproductivity.app.data.dao.ReminderDao
import com.unifiedproductivity.app.data.dao.ReminderListDao
import com.unifiedproductivity.app.data.entity.BudgetItem
import com.unifiedproductivity.app.data.entity.BudgetList
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
        Event::class,
        BudgetList::class,
        BudgetItem::class
    ],
    version = 5,
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
    abstract fun budgetListDao(): BudgetListDao
    abstract fun budgetItemDao(): BudgetItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** v1 → v2: reminders gained a free-form location column. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN location TEXT")
            }
        }

        /** v2 → v3: notes gained an Eisenhower priority quadrant. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN eisenhower TEXT NOT NULL DEFAULT 'NONE'")
            }
        }

        /** v3 → v4: notes gained a checklist (checkable to-do lines). */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN checklistItems TEXT NOT NULL DEFAULT '[]'")
            }
        }

        /** v4 → v5: Budget module (lists + income/expense items); reminders gained an optional cost. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN amount INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS budget_lists (" +
                        "id TEXT NOT NULL PRIMARY KEY, " +
                        "name TEXT NOT NULL, " +
                        "createdAt INTEGER NOT NULL, " +
                        "deletedAt INTEGER)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS budget_items (" +
                        "id TEXT NOT NULL PRIMARY KEY, " +
                        "listId TEXT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "amount INTEGER NOT NULL, " +
                        "type TEXT NOT NULL, " +
                        "isCompleted INTEGER NOT NULL, " +
                        "dueDate INTEGER, " +
                        "createdAt INTEGER NOT NULL, " +
                        "modifiedAt INTEGER NOT NULL, " +
                        "deletedAt INTEGER)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_budget_items_listId ON budget_items(listId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_budget_items_dueDate ON budget_items(dueDate)")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "unified_productivity.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

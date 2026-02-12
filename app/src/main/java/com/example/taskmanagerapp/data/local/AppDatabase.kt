package com.example.taskmanagerapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Task::class, ListEntity::class, MemberEntity::class, CalendarImportMapEntity::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(SyncStateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun listDao(): ListDao
    abstract fun memberDao(): MemberDao
    abstract fun calendarImportMapDao(): CalendarImportMapDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tasks_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .addCallback(PrepopulateCallback)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Prepopulate sample tasks when DB is first created (empty state).
         */
        private object PrepopulateCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val now = System.currentTimeMillis()
                // Insert default list first
                db.execSQL("INSERT OR IGNORE INTO task_lists (id, name, ownerId, updatedAt, remoteId) VALUES (1, 'My Tasks', NULL, $now, NULL)")
                // Insert sample tasks
                db.execSQL("""
                    INSERT INTO tasks (title, description, isDone, priority, dueDate, listId, updatedAt, syncState)
                    VALUES 
                    ('Welcome to Task Manager', 'This is a sample task. Swipe to delete!', 0, 1, NULL, 1, $now, 'SYNCED'),
                    ('Try adding a new task', 'Tap the + button to create your first task', 0, 2, NULL, 1, $now, 'SYNCED')
                """.trimIndent())
            }
        }

        /**
         * Migration 1->2: Schema was unchanged, no-op migration for upgrade path.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // v1->v2 had no schema changes, keeping for upgrade path
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS task_lists (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        ownerId TEXT,
                        updatedAt INTEGER NOT NULL,
                        remoteId TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS list_members (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        listId INTEGER NOT NULL,
                        userId TEXT NOT NULL,
                        role TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                // default list row
                val now = System.currentTimeMillis()
                db.execSQL("INSERT INTO task_lists (id, name, ownerId, updatedAt, remoteId) VALUES (1, 'My Tasks', NULL, $now, NULL)")

                db.execSQL("ALTER TABLE tasks ADD COLUMN listId INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE tasks ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tasks ADD COLUMN syncState TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_listId ON tasks(listId)")
                db.execSQL("UPDATE tasks SET updatedAt = $now")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ensure index exists for listId to match new entity definition.
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_listId ON tasks(listId)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add unique index on (listId, userId) to prevent duplicate members
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_list_members_listId_userId ON list_members(listId, userId)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create calendar_import_map table for tracking imported calendar events
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS calendar_import_map (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        eventId TEXT NOT NULL,
                        taskId INTEGER NOT NULL,
                        importedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_calendar_import_map_eventId ON calendar_import_map(eventId)")
            }
        }
    }
}



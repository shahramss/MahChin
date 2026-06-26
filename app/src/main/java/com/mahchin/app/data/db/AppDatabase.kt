package com.mahchin.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mahchin.app.data.dao.TaskDao
import com.mahchin.app.data.model.DailyTaskInstance
import com.mahchin.app.data.model.MindMapNode
import com.mahchin.app.data.model.MonthlyTemplateTask
import com.mahchin.app.data.model.OneTimeTask
import com.mahchin.app.data.model.Project
import com.mahchin.app.data.model.UserSettings

@Database(
    entities = [
        MonthlyTemplateTask::class,
        DailyTaskInstance::class,
        OneTimeTask::class,
        UserSettings::class,
        Project::class,
        MindMapNode::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `projects` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `isActive` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `mind_map_nodes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `parentId` INTEGER, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `isActive` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mind_map_nodes_projectId` ON `mind_map_nodes` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mind_map_nodes_parentId` ON `mind_map_nodes` (`parentId`)")
                addColumnIfMissing(db, "daily_task_instances", "projectId", "INTEGER")
                addColumnIfMissing(db, "daily_task_instances", "sourceMindMapNodeId", "INTEGER")
                addColumnIfMissing(db, "daily_task_instances", "alarmAtMillis", "INTEGER")
                addColumnIfMissing(db, "one_time_tasks", "projectId", "INTEGER")
                addColumnIfMissing(db, "one_time_tasks", "sourceMindMapNodeId", "INTEGER")
                addColumnIfMissing(db, "one_time_tasks", "alarmAtMillis", "INTEGER")
                addColumnIfMissing(db, "monthly_template_tasks", "projectId", "INTEGER")
                addColumnIfMissing(db, "monthly_template_tasks", "sourceMindMapNodeId", "INTEGER")
                addColumnIfMissing(db, "monthly_template_tasks", "alarmHour", "INTEGER")
                addColumnIfMissing(db, "monthly_template_tasks", "alarmMinute", "INTEGER")
            }
        }

        private fun addColumnIfMissing(db: SupportSQLiteDatabase, table: String, column: String, type: String) {
            db.query("PRAGMA table_info(`$table`)").use { cursor ->
                var exists = false
                while (cursor.moveToNext()) {
                    if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == column) {
                        exists = true
                        break
                    }
                }
                if (!exists) db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $type")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mahchin.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

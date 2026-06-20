package com.mahchin.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mahchin.app.data.dao.TaskDao
import com.mahchin.app.data.model.DailyTaskInstance
import com.mahchin.app.data.model.MonthlyTemplateTask
import com.mahchin.app.data.model.OneTimeTask
import com.mahchin.app.data.model.UserSettings

@Database(
    entities = [
        MonthlyTemplateTask::class,
        DailyTaskInstance::class,
        OneTimeTask::class,
        UserSettings::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mahchin.db"
                )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

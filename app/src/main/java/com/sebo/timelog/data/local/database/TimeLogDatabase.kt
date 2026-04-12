package com.sebo.timelog.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sebo.timelog.data.local.dao.ProjectDao
import com.sebo.timelog.data.local.dao.TimerDao
import com.sebo.timelog.data.local.dao.WorkLogDao
import com.sebo.timelog.data.local.entities.Project
import com.sebo.timelog.data.local.entities.Timer
import com.sebo.timelog.data.local.entities.WorkLog

@Database(
    entities = [Project::class, WorkLog::class, Timer::class],
    version = 1,
    exportSchema = true
)
abstract class TimeLogDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun workLogDao(): WorkLogDao
    abstract fun timerDao(): TimerDao

    companion object {
        private const val DATABASE_NAME = "timelog_database"

        @Volatile
        private var INSTANCE: TimeLogDatabase? = null

        fun getInstance(context: Context): TimeLogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TimeLogDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


package com.sebo.timelog.di

import android.content.Context
import com.sebo.timelog.data.local.database.TimeLogDatabase
import com.sebo.timelog.data.repositories.ProjectRepository
import com.sebo.timelog.data.repositories.TimerRepository
import com.sebo.timelog.data.repositories.WorkLogRepository

class AppContainer(context: Context) {

    private val database = TimeLogDatabase.getInstance(context)

    val projectRepository: ProjectRepository by lazy {
        ProjectRepository(database.projectDao())
    }

    val workLogRepository: WorkLogRepository by lazy {
        WorkLogRepository(database.workLogDao())
    }

    val timerRepository: TimerRepository by lazy {
        TimerRepository(database.timerDao())
    }
}


package com.sebo.timelog.di

import android.content.Context
import com.sebo.timelog.data.local.database.TimeLogDatabase
import com.sebo.timelog.data.remote.AuthService
import com.sebo.timelog.data.remote.SyncService
import com.sebo.timelog.data.remote.SyncStatus
import com.sebo.timelog.data.repositories.ProjectRepository
import com.sebo.timelog.data.repositories.TimerRepository
import com.sebo.timelog.data.repositories.WorkLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppContainer(context: Context) {

    private val database        = TimeLogDatabase.getInstance(context)
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val authService: AuthService? = AuthService.create()

    /** null, wenn Firebase nicht konfiguriert ist (z. B. ohne google-services.json). */
    val syncService: SyncService? = SyncService.create(context)

    private val noServerSyncStatus = MutableStateFlow(SyncStatus.notConfigured())
    val syncStatus: StateFlow<SyncStatus> = syncService?.syncStatus ?: noServerSyncStatus.asStateFlow()

    val projectRepository: ProjectRepository by lazy {
        ProjectRepository(database.projectDao(), syncService)
    }

    val workLogRepository: WorkLogRepository by lazy {
        WorkLogRepository(database.workLogDao(), syncService)
    }

    val timerRepository: TimerRepository by lazy {
        TimerRepository(database.timerDao(), syncService)
    }

    init {
        // Beim App-Start alle vorhandenen Daten einmalig hochladen,
        // damit der Server auch ältere Einträge kennt.
        if (syncService != null) {
            applicationScope.launch {
                try {
                    val projects = projectRepository.getAllProjects().first()
                    val workLogs = workLogRepository.getAllWorkLogs().first()
                    val timer    = timerRepository.getAnyTimer().first()
                    syncService.syncAll(projects, workLogs, timer)
                } catch (e: Exception) {
                    android.util.Log.w("AppContainer", "Initialer Sync fehlgeschlagen: ${e.message}")
                }
            }
        }
    }
}

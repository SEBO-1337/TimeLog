package com.sebo.timelog.di

import android.content.Context
import com.sebo.timelog.data.local.database.TimeLogDatabase
import com.sebo.timelog.data.model.UserRole
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
        ProjectRepository(database.projectDao(), syncService, authService)
    }

    val workLogRepository: WorkLogRepository by lazy {
        WorkLogRepository(database.workLogDao(), database.projectDao(), syncService, authService)
    }

    val timerRepository: TimerRepository by lazy {
        TimerRepository(database.timerDao(), database.projectDao(), syncService)
    }

    init {
        // Beim App-Start zuerst Cloud-Daten lokal herstellen und danach den Merge-Stand hochladen.
        if (syncService != null) {
            applicationScope.launch { runFullSync() }
        }
    }

    /**
     * Führt einen vollständigen bidirektionalen Cloud-Sync durch.
     * Kann manuell (z. B. aus dem Settings-Screen) aufgerufen werden.
     */
    fun triggerManualSync() {
        if (syncService == null) return
        applicationScope.launch { runFullSync() }
    }

    private suspend fun runFullSync() {
        if (syncService == null) return
        try {
            val uid = authService?.currentUser()?.uid.orEmpty()
            val role = authService?.getUserRole() ?: UserRole.ADMIN
            val allowedProjectIds = authService?.getAllowedProjectIds() ?: emptyList()

            val remoteProjects = syncService.fetchProjects(role, uid, allowedProjectIds)
            if (remoteProjects.isNotEmpty()) {
                remoteProjects.forEach { remoteProject ->
                    val existing = database.projectDao().getProjectByCloudIdOnce(remoteProject.cloudId)
                    if (existing == null) {
                        database.projectDao().insert(remoteProject.copy(id = 0))
                    } else {
                        database.projectDao().update(
                            remoteProject.copy(
                                id = existing.id,
                                createdAt = existing.createdAt
                            )
                        )
                    }
                }
            }

            val remoteWorkLogs = syncService.fetchWorkLogs(role, uid, allowedProjectIds)
            if (remoteWorkLogs.isNotEmpty()) {
                remoteWorkLogs.forEach { remoteLog ->
                    try {
                        val localProjectId = when {
                            !remoteLog.projectCloudId.isNullOrBlank() -> {
                                database.projectDao().getProjectByCloudIdOnce(remoteLog.projectCloudId)?.id
                            }
                            remoteLog.workLog.projectId > 0 -> {
                                database.projectDao().getProjectByIdOnce(remoteLog.workLog.projectId)?.id
                            }
                            else -> null
                        }

                        if (localProjectId != null) {
                            database.workLogDao().insert(
                                remoteLog.workLog.copy(projectId = localProjectId)
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("AppContainer", "WorkLog-Import uebersprungen (id=${remoteLog.workLog.id}): ${e.message}")
                    }
                }
            }

            val remoteTimer = syncService.fetchActiveTimer()
            if (remoteTimer != null) {
                val localProjectId = when {
                    !remoteTimer.projectCloudId.isNullOrBlank() -> {
                        database.projectDao().getProjectByCloudIdOnce(remoteTimer.projectCloudId)?.id
                    }
                    remoteTimer.timer.projectId > 0 -> {
                        database.projectDao().getProjectByIdOnce(remoteTimer.timer.projectId)?.id
                    }
                    else -> null
                }
                if (localProjectId != null) {
                    database.timerDao().deleteAll()
                    database.timerDao().insert(remoteTimer.timer.copy(projectId = localProjectId))
                }
            }

            val mergedProjects = projectRepository.getAllProjects().first()
            val mergedWorkLogs = workLogRepository.getAllWorkLogs().first()
            val mergedTimer = timerRepository.getAnyTimer().first()
            syncService.syncAll(mergedProjects, mergedWorkLogs, mergedTimer, uid)
        } catch (e: Exception) {
            android.util.Log.w("AppContainer", "Sync fehlgeschlagen: ${e.message}")
        }
    }
}

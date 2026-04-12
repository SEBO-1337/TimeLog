package com.sebo.timelog.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.sebo.timelog.data.local.entities.Project
import com.sebo.timelog.data.local.entities.Timer
import com.sebo.timelog.data.local.entities.WorkLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SyncService private constructor(firestore: FirebaseFirestore) {

    private val tag = "TimeLogSync"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Gemeinsamer Namespace fuer alle geteilten TimeLog-Daten.
    private val rootDoc = firestore.collection("shared").document("default")

    private val _syncStatus = MutableStateFlow(SyncStatus.idleConfigured())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private fun runSync(operation: String, block: suspend () -> Unit) {
        _syncStatus.value = _syncStatus.value.copy(isSyncing = true)
        scope.launch {
            try {
                block()
                _syncStatus.value = _syncStatus.value.copy(
                    isSyncing = false,
                    lastSuccessAt = System.currentTimeMillis(),
                    lastErrorAt = null,
                    lastErrorMessage = null
                )
            } catch (e: Exception) {
                _syncStatus.value = _syncStatus.value.copy(
                    isSyncing = false,
                    lastErrorAt = System.currentTimeMillis(),
                    lastErrorMessage = e.message
                )
                Log.w(tag, "$operation fehlgeschlagen: ${e.message}")
            }
        }
    }

    fun syncProject(project: Project) {
        runSync("syncProject") {
            val data = mapOf(
                "id" to project.id,
                "name" to project.name,
                "description" to project.description,
                "color" to project.color,
                "hourlyRate" to project.hourlyRate,
                "status" to project.status.name,
                "createdAt" to project.createdAt,
                "updatedAt" to project.updatedAt
            )
            rootDoc.collection("projects").document(project.id.toString()).set(data).await()
        }
    }

    fun deleteProject(projectId: Long) {
        runSync("deleteProject") {
            rootDoc.collection("projects").document(projectId.toString()).delete().await()
        }
    }

    fun syncWorkLog(workLog: WorkLog) {
        runSync("syncWorkLog") {
            val data = mapOf(
                "id" to workLog.id,
                "projectId" to workLog.projectId,
                "description" to workLog.description,
                "hoursWorked" to workLog.hoursWorked,
                "hoursBilled" to workLog.hoursBilled,
                "date" to workLog.date,
                "startTime" to workLog.startTime,
                "endTime" to workLog.endTime,
                "billableStatus" to workLog.billableStatus.name,
                "notes" to workLog.notes,
                "tags" to workLog.tags,
                "createdAt" to workLog.createdAt,
                "updatedAt" to workLog.updatedAt
            )
            rootDoc.collection("workLogs").document(workLog.id.toString()).set(data).await()
        }
    }

    fun deleteWorkLog(workLogId: Long) {
        runSync("deleteWorkLog") {
            rootDoc.collection("workLogs").document(workLogId.toString()).delete().await()
        }
    }

    fun syncTimer(timer: Timer?) {
        runSync("syncTimer") {
            val timerDoc = rootDoc.collection("meta").document("activeTimer")
            if (timer == null) {
                timerDoc.delete().await()
            } else {
                val data = mapOf(
                    "id" to timer.id,
                    "projectId" to timer.projectId,
                    "isRunning" to timer.isRunning,
                    "startTime" to timer.startTime,
                    "pausedDuration" to timer.pausedDuration,
                    "pausedAt" to timer.pausedAt,
                    "description" to timer.description
                )
                timerDoc.set(data).await()
            }
        }
    }

    fun syncAll(projects: List<Project>, workLogs: List<WorkLog>, timer: Timer?) {
        projects.forEach { syncProject(it) }
        workLogs.forEach { syncWorkLog(it) }
        syncTimer(timer)
    }

    companion object {
        fun create(context: Context): SyncService? {
            val app = FirebaseApp.initializeApp(context) ?: return null
            return SyncService(FirebaseFirestore.getInstance(app))
        }
    }
}

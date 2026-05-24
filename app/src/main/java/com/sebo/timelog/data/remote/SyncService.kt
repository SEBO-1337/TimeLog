package com.sebo.timelog.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.sebo.timelog.data.model.UserRole
import com.sebo.timelog.data.local.entities.BillableStatus
import com.sebo.timelog.data.local.entities.Project
import com.sebo.timelog.data.local.entities.ProjectStatus
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
    data class SyncedWorkLog(
        val workLog: WorkLog,
        val projectCloudId: String?
    )

    data class SyncedTimer(
        val timer: Timer,
        val projectCloudId: String?
    )


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
            val docId = project.cloudId.ifBlank { "legacy-${project.id}" }
            val data = mapOf(
                "id" to project.id,
                "cloudId" to docId,
                "name" to project.name,
                "description" to project.description,
                "color" to project.color,
                "hourlyRate" to project.hourlyRate,
                "status" to project.status.name,
                "createdBy" to project.createdBy,
                "createdAt" to project.createdAt,
                "updatedAt" to project.updatedAt
            )
            rootDoc.collection("projects").document(docId).set(data).await()
        }
    }

    fun deleteProject(cloudId: String) {
        runSync("deleteProject") {
            rootDoc.collection("projects").document(cloudId).delete().await()
        }
    }

    fun syncWorkLog(workLog: WorkLog, createdBy: String? = null, projectCloudId: String? = null) {
        runSync("syncWorkLog") {
            val docId = workLog.cloudId.ifBlank { "legacy-${workLog.id}" }
            val data = mapOf(
                "id" to workLog.id,
                "cloudId" to docId,
                "projectId" to workLog.projectId,
                "projectCloudId" to projectCloudId,
                "description" to workLog.description,
                "hoursWorked" to workLog.hoursWorked,
                "hoursBilled" to workLog.hoursBilled,
                "date" to workLog.date,
                "startTime" to workLog.startTime,
                "endTime" to workLog.endTime,
                "billableStatus" to workLog.billableStatus.name,
                "notes" to workLog.notes,
                "tags" to workLog.tags,
                "createdBy" to (createdBy ?: ""),
                "createdAt" to workLog.createdAt,
                "updatedAt" to workLog.updatedAt
            )
            rootDoc.collection("workLogs").document(docId).set(data).await()
        }
    }

    fun deleteWorkLog(workLogCloudId: String) {
        runSync("deleteWorkLog") {
            rootDoc.collection("workLogs").document(workLogCloudId).delete().await()
        }
    }

    fun syncTimer(timer: Timer?, projectCloudId: String? = null) {
        runSync("syncTimer") {
            val timerDoc = rootDoc.collection("meta").document("activeTimer")
            if (timer == null) {
                timerDoc.delete().await()
            } else {
                val data = mapOf(
                    "id" to timer.id,
                    "projectId" to timer.projectId,
                    "projectCloudId" to projectCloudId,
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

    fun syncAll(projects: List<Project>, workLogs: List<WorkLog>, timer: Timer?, workLogCreatedBy: String? = null) {
        val cloudIdsByLocalId = projects.associate { it.id to it.cloudId }
        projects.forEach { syncProject(it) }
        workLogs.forEach { log ->
            syncWorkLog(log, workLogCreatedBy, cloudIdsByLocalId[log.projectId])
        }
        syncTimer(timer, timer?.let { cloudIdsByLocalId[it.projectId] })
    }

    private fun <T> chunkedList(source: List<T>, size: Int): List<List<T>> {
        if (source.isEmpty()) return emptyList()
        val chunks = mutableListOf<List<T>>()
        var index = 0
        while (index < source.size) {
            chunks += source.subList(index, minOf(index + size, source.size))
            index += size
        }
        return chunks
    }

    private fun DocumentSnapshot.longOrNull(field: String): Long? {
        val value = get(field) ?: return null
        return when (value) {
            is Long -> value
            is Int -> value.toLong()
            is Double -> value.toLong()
            is Float -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        }
    }

    private fun DocumentSnapshot.doubleOrNull(field: String): Double? {
        val value = get(field) ?: return null
        return when (value) {
            is Double -> value
            is Float -> value.toDouble()
            is Long -> value.toDouble()
            is Int -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }

    private suspend fun executeProjectQuery(query: Query): List<Project> {
        val snapshot = query.get().await()
        return snapshot.documents.mapNotNull { doc ->
            val cloudId = (doc.getString("cloudId") ?: doc.id).ifBlank { return@mapNotNull null }
            val id = doc.longOrNull("id") ?: 0L
            val name = doc.getString("name") ?: return@mapNotNull null
            val status = try {
                ProjectStatus.valueOf(doc.getString("status") ?: ProjectStatus.ACTIVE.name)
            } catch (_: Exception) {
                ProjectStatus.ACTIVE
            }

            Project(
                id = id,
                name = name,
                description = doc.getString("description"),
                color = doc.getString("color") ?: "#2196F3",
                hourlyRate = doc.doubleOrNull("hourlyRate") ?: 0.0,
                status = status,
                cloudId = cloudId,
                createdBy = doc.getString("createdBy") ?: "",
                createdAt = doc.longOrNull("createdAt") ?: System.currentTimeMillis(),
                updatedAt = doc.longOrNull("updatedAt") ?: System.currentTimeMillis()
            )
        }
    }

    suspend fun fetchProjects(role: UserRole, userId: String, allowedProjectIds: List<String>): List<Project> {
        return try {
            val result = when (role) {
                UserRole.ADMIN -> executeProjectQuery(rootDoc.collection("projects"))
                UserRole.TECHNICIAN -> executeProjectQuery(
                    rootDoc.collection("projects").whereEqualTo("createdBy", userId)
                )
                UserRole.CUSTOMER -> {
                    val chunks = chunkedList(allowedProjectIds, 10)
                    chunks.flatMap { ids ->
                        executeProjectQuery(rootDoc.collection("projects").whereIn(FieldPath.documentId(), ids))
                    }
                }
                UserRole.NEW -> emptyList()
            }

            _syncStatus.value = _syncStatus.value.copy(
                lastSuccessAt = System.currentTimeMillis(),
                lastErrorAt = null,
                lastErrorMessage = null
            )
            result
        } catch (e: Exception) {
            _syncStatus.value = _syncStatus.value.copy(
                lastErrorAt = System.currentTimeMillis(),
                lastErrorMessage = "fetchProjects: ${e.message}"
            )
            Log.w(tag, "fetchProjects fehlgeschlagen: ${e.message}")
            emptyList()
        }
    }

    private suspend fun executeWorkLogQuery(query: Query): List<SyncedWorkLog> {
        val snapshot = query.get().await()
        return snapshot.documents.mapNotNull { doc ->
            val createdAtFallback = doc.longOrNull("createdAt") ?: System.currentTimeMillis()
            val cloudId = (doc.getString("cloudId") ?: doc.id).ifBlank { return@mapNotNull null }
            val id = doc.longOrNull("id") ?: createdAtFallback
            val projectId = doc.longOrNull("projectId") ?: -1L
            val projectCloudId = doc.getString("projectCloudId")
            val hoursWorked = doc.doubleOrNull("hoursWorked") ?: 0.0
            val date = doc.longOrNull("date") ?: createdAtFallback
            val billableStatus = try {
                BillableStatus.valueOf(doc.getString("billableStatus") ?: BillableStatus.UNBILLED.name)
            } catch (_: Exception) {
                BillableStatus.UNBILLED
            }

            SyncedWorkLog(
                workLog = WorkLog(
                    id = id,
                    projectId = projectId,
                    description = doc.getString("description") ?: "",
                    hoursWorked = hoursWorked,
                    hoursBilled = doc.doubleOrNull("hoursBilled") ?: 0.0,
                    date = date,
                    startTime = doc.longOrNull("startTime"),
                    endTime = doc.longOrNull("endTime"),
                    billableStatus = billableStatus,
                    notes = doc.getString("notes"),
                    tags = doc.getString("tags"),
                    cloudId = cloudId,
                    createdAt = createdAtFallback,
                    updatedAt = doc.longOrNull("updatedAt") ?: createdAtFallback
                ),
                projectCloudId = projectCloudId
            )
        }
    }

    suspend fun fetchWorkLogs(role: UserRole, userId: String, allowedProjectIds: List<String>): List<SyncedWorkLog> {
        return try {
            val result = when (role) {
                UserRole.ADMIN -> executeWorkLogQuery(rootDoc.collection("workLogs"))
                UserRole.TECHNICIAN -> executeWorkLogQuery(
                    rootDoc.collection("workLogs").whereEqualTo("createdBy", userId)
                )
                UserRole.CUSTOMER -> {
                    val chunks = chunkedList(allowedProjectIds, 10)
                    chunks.flatMap { ids ->
                        executeWorkLogQuery(rootDoc.collection("workLogs").whereIn("projectCloudId", ids))
                    }
                }
                UserRole.NEW -> emptyList()
            }

            _syncStatus.value = _syncStatus.value.copy(
                lastSuccessAt = System.currentTimeMillis(),
                lastErrorAt = null,
                lastErrorMessage = null
            )
            result
        } catch (e: Exception) {
            _syncStatus.value = _syncStatus.value.copy(
                lastErrorAt = System.currentTimeMillis(),
                lastErrorMessage = "fetchWorkLogs: ${e.message}"
            )
            Log.w(tag, "fetchWorkLogs fehlgeschlagen: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchActiveTimer(): SyncedTimer? {
        return try {
            val doc = rootDoc.collection("meta").document("activeTimer").get().await()
            if (!doc.exists()) return null

            val id = doc.getLong("id") ?: 0L
            val projectId = doc.getLong("projectId") ?: return null

            val timer = Timer(
                id = id,
                projectId = projectId,
                isRunning = doc.getBoolean("isRunning") ?: false,
                startTime = doc.getLong("startTime") ?: return null,
                pausedDuration = doc.getLong("pausedDuration") ?: 0L,
                pausedAt = doc.getLong("pausedAt"),
                description = doc.getString("description")
            )

            _syncStatus.value = _syncStatus.value.copy(
                lastSuccessAt = System.currentTimeMillis(),
                lastErrorAt = null,
                lastErrorMessage = null
            )
            SyncedTimer(
                timer = timer,
                projectCloudId = doc.getString("projectCloudId")
            )
        } catch (e: Exception) {
            _syncStatus.value = _syncStatus.value.copy(
                lastErrorAt = System.currentTimeMillis(),
                lastErrorMessage = "fetchActiveTimer: ${e.message}"
            )
            Log.w(tag, "fetchActiveTimer fehlgeschlagen: ${e.message}")
            null
        }
    }

    companion object {
        fun create(context: Context): SyncService? {
            val app = FirebaseApp.initializeApp(context) ?: return null
            return SyncService(FirebaseFirestore.getInstance(app))
        }
    }
}

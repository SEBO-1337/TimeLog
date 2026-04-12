package com.sebo.timelog.data.remote

import android.util.Log
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Synchronisiert Daten von der Android-App zum lokalen Web-Server.
 * Alle Operationen sind fire-and-forget (blockieren die UI nicht).
 *
 * Der Server läuft auf dem PC im Heimnetz. Die URL wird via BuildConfig
 * aus local.properties gelesen (timelog.server.url=http://192.168.x.x:3000).
 */
class SyncService(private val baseUrl: String) {

    private val TAG = "TimeLogSync"

    // Eigener Scope – unabhängig von ViewModelScope, lebt solange die App läuft
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    private val _syncStatus = MutableStateFlow(SyncStatus.idleConfigured())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    // ── HTTP-Helfer ──────────────────────────────────────────────

    private fun post(path: String, json: JSONObject) {
        _syncStatus.value = _syncStatus.value.copy(isSyncing = true)
        try {
            val body    = json.toString().toRequestBody(JSON)
            val request = Request.Builder().url("$baseUrl$path").post(body).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    _syncStatus.value = _syncStatus.value.copy(
                        isSyncing = false,
                        lastSuccessAt = System.currentTimeMillis(),
                        lastErrorAt = null,
                        lastErrorMessage = null
                    )
                } else {
                    val error = "HTTP ${response.code}"
                    _syncStatus.value = _syncStatus.value.copy(
                        isSyncing = false,
                        lastErrorAt = System.currentTimeMillis(),
                        lastErrorMessage = error
                    )
                    Log.w(TAG, "POST $path fehlgeschlagen: $error")
                }
            }
        } catch (e: Exception) {
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                lastErrorAt = System.currentTimeMillis(),
                lastErrorMessage = e.message
            )
            Log.w(TAG, "POST $path fehlgeschlagen: ${e.message}")
        }
    }

    private fun delete(path: String) {
        _syncStatus.value = _syncStatus.value.copy(isSyncing = true)
        try {
            val request = Request.Builder().url("$baseUrl$path").delete().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    _syncStatus.value = _syncStatus.value.copy(
                        isSyncing = false,
                        lastSuccessAt = System.currentTimeMillis(),
                        lastErrorAt = null,
                        lastErrorMessage = null
                    )
                } else {
                    val error = "HTTP ${response.code}"
                    _syncStatus.value = _syncStatus.value.copy(
                        isSyncing = false,
                        lastErrorAt = System.currentTimeMillis(),
                        lastErrorMessage = error
                    )
                    Log.w(TAG, "DELETE $path fehlgeschlagen: $error")
                }
            }
        } catch (e: Exception) {
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                lastErrorAt = System.currentTimeMillis(),
                lastErrorMessage = e.message
            )
            Log.w(TAG, "DELETE $path fehlgeschlagen: ${e.message}")
        }
    }

    // ── Projekte ─────────────────────────────────────────────────

    fun syncProject(project: Project) = scope.launch {
        post("/api/projects/sync", JSONObject().apply {
            put("id",          project.id)
            put("name",        project.name)
            put("description", project.description)
            put("color",       project.color)
            put("hourlyRate",  project.hourlyRate)
            put("status",      project.status.name)
            put("createdAt",   project.createdAt)
            put("updatedAt",   project.updatedAt)
        })
    }

    fun deleteProject(projectId: Long) = scope.launch {
        delete("/api/projects/$projectId")
    }

    // ── WorkLogs ─────────────────────────────────────────────────

    fun syncWorkLog(workLog: WorkLog) = scope.launch {
        post("/api/worklogs/sync", JSONObject().apply {
            put("id",             workLog.id)
            put("projectId",      workLog.projectId)
            put("description",    workLog.description)
            put("hoursWorked",    workLog.hoursWorked)
            put("hoursBilled",    workLog.hoursBilled)
            put("date",           workLog.date)
            put("startTime",      workLog.startTime)
            put("endTime",        workLog.endTime)
            put("billableStatus", workLog.billableStatus.name)
            put("notes",          workLog.notes)
            put("tags",           workLog.tags)
            put("createdAt",      workLog.createdAt)
            put("updatedAt",      workLog.updatedAt)
        })
    }

    fun deleteWorkLog(workLogId: Long) = scope.launch {
        delete("/api/worklogs/$workLogId")
    }

    // ── Timer ────────────────────────────────────────────────────

    fun syncTimer(timer: Timer?) = scope.launch {
        if (timer == null) {
            delete("/api/timer")
        } else {
            post("/api/timer/sync", JSONObject().apply {
                put("projectId",      timer.projectId)
                put("isRunning",      timer.isRunning)
                put("startTime",      timer.startTime)
                put("pausedDuration", timer.pausedDuration)
                put("pausedAt",       timer.pausedAt)
                put("description",    timer.description)
            })
        }
    }

    // ── Initialer Bulk-Sync (beim App-Start) ─────────────────────

    fun syncAll(projects: List<Project>, workLogs: List<WorkLog>, timer: Timer?) {
        // Projekte zuerst (wegen FK-Beziehungen in der Backend-DB)
        projects.forEach { syncProject(it) }
        workLogs.forEach { syncWorkLog(it) }
        syncTimer(timer)
    }

    companion object {
        /**
         * Erzeugt eine SyncService-Instanz, falls eine Server-URL konfiguriert ist.
         * URL wird aus BuildConfig.TIMELOG_SERVER_URL gelesen (→ local.properties).
         */
        fun create(serverUrl: String): SyncService? {
            val url = serverUrl.trim()
            return if (url.isNotEmpty()) SyncService(url) else null
        }
    }
}


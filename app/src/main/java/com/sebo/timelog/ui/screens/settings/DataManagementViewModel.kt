package com.sebo.timelog.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sebo.timelog.data.local.entities.BillableStatus
import com.sebo.timelog.data.local.entities.Project
import com.sebo.timelog.data.local.entities.ProjectStatus
import com.sebo.timelog.data.local.entities.WorkLog
import com.sebo.timelog.data.repositories.ProjectRepository
import com.sebo.timelog.data.repositories.WorkLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class DataManagementState {
    object Idle : DataManagementState()
    object Loading : DataManagementState()
    data class Success(val message: String) : DataManagementState()
    data class Error(val message: String) : DataManagementState()
    data class BackupReady(val json: String, val fileName: String) : DataManagementState()
    data class CsvReady(val csv: String, val fileName: String) : DataManagementState()
    data class RestoreConfirm(
        val json: String,
        val projectCount: Int,
        val workLogCount: Int
    ) : DataManagementState()
}

class DataManagementViewModel(
    private val projectRepository: ProjectRepository,
    private val workLogRepository: WorkLogRepository
) : ViewModel() {

    private val _state = MutableStateFlow<DataManagementState>(DataManagementState.Idle)
    val state: StateFlow<DataManagementState> = _state.asStateFlow()

    fun dismissState() {
        _state.value = DataManagementState.Idle
    }

    // ── JSON Backup ────────────────────────────────────────────────────────────

    fun prepareBackup() {
        viewModelScope.launch {
            _state.value = DataManagementState.Loading
            try {
                val projects  = projectRepository.getAllProjects().first()
                val workLogs  = workLogRepository.getAllWorkLogs().first()

                val root = JSONObject().apply {
                    put("version", 1)
                    put("exportedAt", System.currentTimeMillis())
                    put("projects", JSONArray().also { arr ->
                        projects.forEach { p ->
                            arr.put(JSONObject().apply {
                                put("id",          p.id)
                                put("name",        p.name)
                                put("description", p.description ?: "")
                                put("color",       p.color)
                                put("hourlyRate",  p.hourlyRate)
                                put("status",      p.status.name)
                                put("cloudId",     p.cloudId)
                                put("createdBy",   p.createdBy)
                                put("createdAt",   p.createdAt)
                                put("updatedAt",   p.updatedAt)
                            })
                        }
                    })
                    put("workLogs", JSONArray().also { arr ->
                        workLogs.forEach { w ->
                            arr.put(JSONObject().apply {
                                put("id",             w.id)
                                put("projectId",      w.projectId)
                                put("description",    w.description)
                                put("hoursWorked",    w.hoursWorked)
                                put("hoursBilled",    w.hoursBilled)
                                put("date",           w.date)
                                if (w.startTime != null) put("startTime", w.startTime) else put("startTime", JSONObject.NULL)
                                if (w.endTime   != null) put("endTime",   w.endTime)   else put("endTime",   JSONObject.NULL)
                                put("billableStatus", w.billableStatus.name)
                                put("notes",          w.notes  ?: "")
                                put("tags",           w.tags   ?: "")
                                put("createdAt",      w.createdAt)
                                put("updatedAt",      w.updatedAt)
                            })
                        }
                    })
                }

                val dateStr  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val fileName = "timelog_backup_$dateStr.json"
                _state.value = DataManagementState.BackupReady(root.toString(2), fileName)
            } catch (e: Exception) {
                _state.value = DataManagementState.Error("Fehler beim Erstellen des Backups: ${e.message}")
            }
        }
    }

    // ── CSV Export ─────────────────────────────────────────────────────────────

    fun prepareCsvExport() {
        viewModelScope.launch {
            _state.value = DataManagementState.Loading
            try {
                val projects   = projectRepository.getAllProjects().first()
                val workLogs   = workLogRepository.getAllWorkLogs().first()
                val projectMap = projects.associateBy { it.id }

                val sb         = StringBuilder()
                sb.appendLine("Datum,Projekt,Beschreibung,Stunden (gearbeitet),Stunden (verrechnet),Abrechnungsstatus,Notizen,Tags")

                val dateFmt = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

                fun String.csvEscape() =
                    if (contains(',') || contains('"') || contains('\n'))
                        "\"${replace("\"", "\"\"")}\""
                    else this

                workLogs.sortedByDescending { it.date }.forEach { w ->
                    val projectName = projectMap[w.projectId]?.name ?: "Unbekannt"
                    val date        = dateFmt.format(Date(w.date))
                    val status      = when (w.billableStatus) {
                        BillableStatus.UNBILLED -> "Nicht abgerechnet"
                        BillableStatus.BILLED   -> "Abgerechnet"
                        BillableStatus.PARTIAL  -> "Teilweise abgerechnet"
                    }
                    sb.appendLine(
                        "${date.csvEscape()}," +
                        "${projectName.csvEscape()}," +
                        "${w.description.csvEscape()}," +
                        "${w.hoursWorked}," +
                        "${w.hoursBilled}," +
                        "${status.csvEscape()}," +
                        "${(w.notes ?: "").csvEscape()}," +
                        "${(w.tags  ?: "").csvEscape()}"
                    )
                }

                val dateStr  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val fileName = "timelog_export_$dateStr.csv"
                _state.value = DataManagementState.CsvReady(sb.toString(), fileName)
            } catch (e: Exception) {
                _state.value = DataManagementState.Error("Fehler beim Erstellen des Exports: ${e.message}")
            }
        }
    }

    // ── Write to URI (called after file picker returns a URI) ──────────────────

    fun writeContentToUri(context: Context, uri: Uri, content: String) {
        viewModelScope.launch {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                }
                _state.value = DataManagementState.Success("Datei erfolgreich gespeichert ✓")
            } catch (e: Exception) {
                _state.value = DataManagementState.Error("Fehler beim Speichern: ${e.message}")
            }
        }
    }

    // ── Restore ────────────────────────────────────────────────────────────────

    fun loadRestoreFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = DataManagementState.Loading
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                    ?: throw Exception("Datei konnte nicht gelesen werden")

                val root         = JSONObject(json)
                val projectCount = root.optJSONArray("projects")?.length()  ?: 0
                val workLogCount = root.optJSONArray("workLogs")?.length()  ?: 0

                _state.value = DataManagementState.RestoreConfirm(json, projectCount, workLogCount)
            } catch (e: Exception) {
                _state.value = DataManagementState.Error("Ungültige Backup-Datei: ${e.message}")
            }
        }
    }

    fun performRestore(json: String) {
        viewModelScope.launch {
            _state.value = DataManagementState.Loading
            try {
                val root           = JSONObject(json)
                val projectsArray  = root.optJSONArray("projects")
                val workLogsArray  = root.optJSONArray("workLogs")
                val oldIdToNewId   = mutableMapOf<Long, Long>()

                if (projectsArray != null) {
                    for (i in 0 until projectsArray.length()) {
                        val p   = projectsArray.getJSONObject(i)
                        val oldId = p.getLong("id")
                        val project = Project(
                            id          = 0,
                            name        = p.getString("name"),
                            description = p.optString("description").takeIf { it.isNotBlank() },
                            color       = p.optString("color", "#2196F3"),
                            hourlyRate  = p.optDouble("hourlyRate", 0.0),
                            status      = runCatching { ProjectStatus.valueOf(p.getString("status")) }
                                              .getOrDefault(ProjectStatus.ACTIVE),
                            cloudId     = p.optString("cloudId", ""),
                            createdBy   = p.optString("createdBy", ""),
                            createdAt   = p.optLong("createdAt",  System.currentTimeMillis()),
                            updatedAt   = p.optLong("updatedAt",  System.currentTimeMillis())
                        )
                        val newId = projectRepository.insert(project)
                        oldIdToNewId[oldId] = newId
                    }
                }

                var importedLogs = 0
                if (workLogsArray != null) {
                    for (i in 0 until workLogsArray.length()) {
                        val w           = workLogsArray.getJSONObject(i)
                        val oldProjectId = w.getLong("projectId")
                        val newProjectId = oldIdToNewId[oldProjectId] ?: continue

                        val workLog = WorkLog(
                            id             = 0,
                            projectId      = newProjectId,
                            description    = w.optString("description", ""),
                            hoursWorked    = w.getDouble("hoursWorked"),
                            hoursBilled    = w.optDouble("hoursBilled", 0.0),
                            date           = w.getLong("date"),
                            startTime      = w.optLong("startTime", 0L).takeIf { it != 0L },
                            endTime        = w.optLong("endTime",   0L).takeIf { it != 0L },
                            billableStatus = runCatching { BillableStatus.valueOf(w.getString("billableStatus")) }
                                                 .getOrDefault(BillableStatus.UNBILLED),
                            notes          = w.optString("notes").takeIf { it.isNotBlank() },
                            tags           = w.optString("tags").takeIf  { it.isNotBlank() },
                            createdAt      = w.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt      = w.optLong("updatedAt", System.currentTimeMillis())
                        )
                        workLogRepository.insert(workLog)
                        importedLogs++
                    }
                }

                _state.value = DataManagementState.Success(
                    "Wiederherstellung abgeschlossen ✓\n" +
                    "${oldIdToNewId.size} Projekte und $importedLogs Arbeitslogs importiert"
                )
            } catch (e: Exception) {
                _state.value = DataManagementState.Error("Fehler bei der Wiederherstellung: ${e.message}")
            }
        }
    }

    // ── Factory ────────────────────────────────────────────────────────────────

    companion object {
        fun factory(
            projectRepository: ProjectRepository,
            workLogRepository:  WorkLogRepository
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DataManagementViewModel(projectRepository, workLogRepository) as T
        }
    }
}


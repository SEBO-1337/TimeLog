package com.sebo.timelog.ui.screens.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sebo.timelog.data.local.entities.Project
import com.sebo.timelog.data.local.entities.ProjectStatus
import com.sebo.timelog.data.repositories.ProjectRepository
import com.sebo.timelog.data.repositories.WorkLogRepository
import com.sebo.timelog.utils.effectiveBilledHours
import com.sebo.timelog.utils.pendingHours
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ProjectsUiState {
    data object Loading : ProjectsUiState()
    data class Success(val projects: List<Project>) : ProjectsUiState()
    data class Error(val message: String) : ProjectsUiState()
}

data class ProjectBillingSummary(
    val totalHours: Double = 0.0,
    val billedHours: Double = 0.0,
    val pendingHours: Double = 0.0,
    val workLogCount: Int = 0,
    val openAmount: Double = 0.0
)

class ProjectsViewModel(
    private val projectRepository: ProjectRepository,
    private val workLogRepository: WorkLogRepository
) : ViewModel() {

    val projects: StateFlow<List<Project>> = projectRepository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectBillingSummaries: StateFlow<Map<Long, ProjectBillingSummary>> = combine(
        projectRepository.getAllProjects(),
        workLogRepository.getAllWorkLogs()
    ) { projects, workLogs ->
        projects.associate { project ->
            val projectLogs = workLogs.filter { it.projectId == project.id }
            val totalHours = projectLogs.sumOf { it.hoursWorked }
            val billedHours = projectLogs.sumOf { it.effectiveBilledHours() }
            val pendingHours = projectLogs.sumOf { it.pendingHours() }

            project.id to ProjectBillingSummary(
                totalHours = totalHours,
                billedHours = billedHours,
                pendingHours = pendingHours,
                workLogCount = projectLogs.size,
                openAmount = pendingHours * project.hourlyRate
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _uiState = MutableStateFlow<ProjectsUiState>(ProjectsUiState.Loading)
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            projectRepository.getAllProjects().collect { projectList ->
                _uiState.value = ProjectsUiState.Success(projectList)
            }
        }
    }

    fun createProject(
        name: String,
        description: String? = null,
        color: String = "#2196F3",
        hourlyRate: Double = 0.0
    ) {
        viewModelScope.launch {
            try {
                val project = Project(
                    name = name.trim(),
                    description = description?.trim()?.ifEmpty { null },
                    color = color,
                    hourlyRate = hourlyRate
                )
                projectRepository.insert(project)
            } catch (e: Exception) {
                _uiState.value = ProjectsUiState.Error("Fehler beim Erstellen: ${e.message}")
            }
        }
    }

    fun updateProject(project: Project) {
        viewModelScope.launch {
            try {
                projectRepository.update(project)
            } catch (e: Exception) {
                _uiState.value = ProjectsUiState.Error("Fehler beim Aktualisieren: ${e.message}")
            }
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            try {
                projectRepository.delete(project)
            } catch (e: Exception) {
                _uiState.value = ProjectsUiState.Error("Fehler beim Löschen: ${e.message}")
            }
        }
    }

    fun archiveProject(project: Project) {
        viewModelScope.launch {
            projectRepository.update(project.copy(status = ProjectStatus.ARCHIVED))
        }
    }

    fun markProjectPendingAsBilled(projectId: Long) {
        viewModelScope.launch {
            try {
                workLogRepository.markProjectPendingAsBilled(projectId)
            } catch (e: Exception) {
                _uiState.value = ProjectsUiState.Error("Fehler beim Abrechnen: ${e.message}")
            }
        }
    }

    companion object {
        fun factory(
            projectRepository: ProjectRepository,
            workLogRepository: WorkLogRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProjectsViewModel(projectRepository, workLogRepository) as T
                }
            }
        }
    }
}


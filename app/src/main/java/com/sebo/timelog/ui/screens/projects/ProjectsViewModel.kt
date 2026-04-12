package com.sebo.timelog.ui.screens.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sebo.timelog.data.local.entities.Project
import com.sebo.timelog.data.local.entities.ProjectStatus
import com.sebo.timelog.data.repositories.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ProjectsUiState {
    data object Loading : ProjectsUiState()
    data class Success(val projects: List<Project>) : ProjectsUiState()
    data class Error(val message: String) : ProjectsUiState()
}

class ProjectsViewModel(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    val projects: StateFlow<List<Project>> = projectRepository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    companion object {
        fun factory(projectRepository: ProjectRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProjectsViewModel(projectRepository) as T
                }
            }
        }
    }
}


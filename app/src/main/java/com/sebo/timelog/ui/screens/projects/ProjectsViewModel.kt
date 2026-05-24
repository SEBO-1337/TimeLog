package com.sebo.timelog.ui.screens.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sebo.timelog.data.local.entities.Project
import com.sebo.timelog.data.local.entities.ProjectStatus
import com.sebo.timelog.data.model.UserRole
import com.sebo.timelog.data.repositories.ProjectRepository
import com.sebo.timelog.data.repositories.WorkLogRepository
import com.sebo.timelog.data.remote.AuthService
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
    private val workLogRepository: WorkLogRepository,
    private val authService: AuthService? = null
) : ViewModel() {

    private val _userRole = MutableStateFlow(UserRole.NEW)
    val userRole: StateFlow<UserRole> = _userRole.asStateFlow()

    private val _userId = MutableStateFlow("")

    val projects: StateFlow<List<Project>> = combine(
        projectRepository.getAllProjects(),
        _userRole,
        _userId
    ) { allProjects, role, userId ->
        when (role) {
            UserRole.ADMIN -> allProjects
            UserRole.TECHNICIAN -> allProjects.filter { it.createdBy == userId }
            UserRole.CUSTOMER -> emptyList()
            UserRole.NEW -> emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectBillingSummaries: StateFlow<Map<Long, ProjectBillingSummary>> = combine(
        projectRepository.getAllProjects(),
        workLogRepository.getAllWorkLogs(),
        _userRole,
        _userId
    ) { allProjects, workLogs, role, userId ->
        val visibleProjects = when (role) {
            UserRole.ADMIN -> allProjects
            UserRole.TECHNICIAN -> allProjects.filter { it.createdBy == userId }
            UserRole.CUSTOMER -> emptyList()
            UserRole.NEW -> emptyList()
        }
        visibleProjects.associate { project ->
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
        // Rolle muss auf Login/Logout reagieren; einmaliges Laden beim Init reicht nicht.
        viewModelScope.launch {
            if (authService == null) {
                _userRole.value = UserRole.ADMIN
                _userId.value = ""
                return@launch
            }

            authService.authState.collect { user ->
                if (user == null) {
                    _userId.value = ""
                    _userRole.value = UserRole.NEW
                } else {
                    _userId.value = user.uid
                    _userRole.value = authService.getUserRole()
                }
            }
        }

        viewModelScope.launch {
            projects.collect { projectList ->
                _uiState.value = ProjectsUiState.Success(projectList)
            }
        }
    }

    fun canCreateProject(): Boolean {
        return when (_userRole.value) {
            UserRole.ADMIN, UserRole.TECHNICIAN -> true
            UserRole.CUSTOMER, UserRole.NEW -> false
        }
    }

    fun canEditProject(project: Project): Boolean {
        return when (_userRole.value) {
            UserRole.ADMIN -> true
            UserRole.TECHNICIAN -> project.createdBy == _userId.value
            UserRole.CUSTOMER -> false
            UserRole.NEW -> false
        }
    }

    fun canDeleteProject(project: Project): Boolean {
        return when (_userRole.value) {
            UserRole.ADMIN -> true
            UserRole.TECHNICIAN -> project.createdBy == _userId.value
            UserRole.CUSTOMER -> false
            UserRole.NEW -> false
        }
    }

    fun createProject(
        name: String,
        description: String? = null,
        color: String = "#2196F3",
        hourlyRate: Double = 0.0
    ) {
        if (!canCreateProject()) {
            _uiState.value = ProjectsUiState.Error("Keine Berechtigung zum Erstellen von Projekten")
            return
        }
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
        if (!canEditProject(project)) {
            _uiState.value = ProjectsUiState.Error("Keine Berechtigung zum Bearbeiten dieses Projekts")
            return
        }
        viewModelScope.launch {
            try {
                projectRepository.update(project)
            } catch (e: Exception) {
                _uiState.value = ProjectsUiState.Error("Fehler beim Aktualisieren: ${e.message}")
            }
        }
    }

    fun deleteProject(project: Project) {
        if (!canDeleteProject(project)) {
            _uiState.value = ProjectsUiState.Error("Keine Berechtigung zum Loeschen dieses Projekts")
            return
        }
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

    /**
     * Rechnet einen Geldbetrag ab. Die entsprechenden Stunden werden auf dem Projekt abgezogen.
     * Wenn mehr Geld ausgezahlt wird als Stunden vorhanden, gehen die Stunden ins Minus.
     */
    fun billProjectByAmount(projectId: Long, amount: Double, hourlyRate: Double) {
        if (hourlyRate <= 0.0) return
        val hoursToBill = amount / hourlyRate
        viewModelScope.launch {
            try {
                workLogRepository.billProjectByHours(projectId, hoursToBill)
            } catch (e: Exception) {
                _uiState.value = ProjectsUiState.Error("Fehler beim Abrechnen: ${e.message}")
            }
        }
    }

    companion object {
        fun factory(
            projectRepository: ProjectRepository,
            workLogRepository: WorkLogRepository,
            authService: AuthService? = null
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProjectsViewModel(projectRepository, workLogRepository, authService) as T
                }
            }
        }
    }
}


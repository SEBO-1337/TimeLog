package com.sebo.timelog.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sebo.timelog.data.local.entities.Project
import com.sebo.timelog.data.local.entities.WorkLog
import com.sebo.timelog.data.repositories.ProjectRepository
import com.sebo.timelog.data.repositories.WorkLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val workLogs: List<WorkLog> = emptyList(),
    val projects: Map<Long, Project> = emptyMap(),
    val isLoading: Boolean = true,
    val filterProjectId: Long? = null
)

class HistoryViewModel(
    private val workLogRepository: WorkLogRepository,
    projectRepository: ProjectRepository
) : ViewModel() {

    private val _filterProjectId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        workLogRepository.getAllWorkLogs(),
        projectRepository.getAllProjects(),
        _filterProjectId
    ) { workLogs, projects, filterProjectId ->
        val projectMap = projects.associateBy { it.id }
        val filteredLogs = if (filterProjectId != null) {
            workLogs.filter { it.projectId == filterProjectId }
        } else {
            workLogs
        }
        HistoryUiState(
            workLogs = filteredLogs,
            projects = projectMap,
            isLoading = false,
            filterProjectId = filterProjectId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    fun setProjectFilter(projectId: Long?) {
        _filterProjectId.value = projectId
    }

    fun deleteWorkLog(workLog: WorkLog) {
        viewModelScope.launch {
            workLogRepository.delete(workLog)
        }
    }

    companion object {
        fun factory(
            workLogRepository: WorkLogRepository,
            projectRepository: ProjectRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HistoryViewModel(workLogRepository, projectRepository) as T
                }
            }
        }
    }
}


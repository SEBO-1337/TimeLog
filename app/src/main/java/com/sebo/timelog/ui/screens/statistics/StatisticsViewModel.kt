package com.sebo.timelog.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sebo.timelog.data.local.entities.Project
import com.sebo.timelog.data.repositories.ProjectRepository
import com.sebo.timelog.data.repositories.WorkLogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StatisticsUiState(
    val totalHours: Double = 0.0,
    val totalEntries: Int = 0,
    val projectStats: List<ProjectStatItem> = emptyList(),
    val isLoading: Boolean = true
)

data class ProjectStatItem(
    val project: Project,
    val totalHours: Double,
    val entryCount: Int,
    val estimatedRevenue: Double
)

class StatisticsViewModel(
    workLogRepository: WorkLogRepository,
    projectRepository: ProjectRepository
) : ViewModel() {

    val uiState: StateFlow<StatisticsUiState> = combine(
        workLogRepository.getAllWorkLogs(),
        projectRepository.getVisibleProjects()
    ) { workLogs, projects ->
        val projectMap = projects.associateBy { it.id }
        val visibleLogs = workLogs.filter { it.projectId in projectMap.keys }
        val totalHours = visibleLogs.sumOf { it.hoursWorked }

        val projectStats = projects.map { project ->
                val projectLogs = visibleLogs.filter { it.projectId == project.id }
            val hours = projectLogs.sumOf { it.hoursWorked }
            ProjectStatItem(
                project = project,
                totalHours = hours,
                entryCount = projectLogs.size,
                estimatedRevenue = hours * project.hourlyRate
            )
        }.filter { it.totalHours > 0 || it.entryCount > 0 }
            .sortedByDescending { it.totalHours }

        StatisticsUiState(
            totalHours = totalHours,
            totalEntries = visibleLogs.size,
            projectStats = projectStats,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatisticsUiState())

    companion object {
        fun factory(
            workLogRepository: WorkLogRepository,
            projectRepository: ProjectRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StatisticsViewModel(workLogRepository, projectRepository) as T
                }
            }
        }
    }
}


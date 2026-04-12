package com.sebo.timelog.data.repositories

import com.sebo.timelog.data.local.dao.ProjectDao
import com.sebo.timelog.data.local.entities.Project
import com.sebo.timelog.data.local.entities.ProjectStatus
import com.sebo.timelog.data.local.relations.ProjectWithWorkLogs
import com.sebo.timelog.data.remote.SyncService
import kotlinx.coroutines.flow.Flow

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val syncService: SyncService? = null
) {

    fun getAllProjects(): Flow<List<Project>> = projectDao.getAllProjects()

    fun getActiveProjects(): Flow<List<Project>> =
        projectDao.getProjectsByStatus(ProjectStatus.ACTIVE)

    fun getProjectById(id: Long): Flow<Project?> = projectDao.getProjectById(id)

    suspend fun getProjectByIdOnce(id: Long): Project? = projectDao.getProjectByIdOnce(id)

    fun getProjectWithWorkLogs(id: Long): Flow<ProjectWithWorkLogs?> =
        projectDao.getProjectWithWorkLogs(id)

    suspend fun insert(project: Project): Long {
        val id = projectDao.insert(project)
        syncService?.syncProject(project.copy(id = id))
        return id
    }

    suspend fun update(project: Project) {
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        projectDao.update(updated)
        syncService?.syncProject(updated)
    }

    suspend fun delete(project: Project) {
        projectDao.delete(project)
        syncService?.deleteProject(project.id)
    }

    suspend fun deleteById(id: Long) {
        projectDao.deleteById(id)
        syncService?.deleteProject(id)
    }

    fun getProjectCount(): Flow<Int> = projectDao.getProjectCount()
}

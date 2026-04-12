package com.sebo.timelog.data.repositories

import com.sebo.timelog.data.local.dao.ProjectDao
import com.sebo.timelog.data.local.entities.Project
import com.sebo.timelog.data.local.entities.ProjectStatus
import com.sebo.timelog.data.local.relations.ProjectWithWorkLogs
import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {

    fun getAllProjects(): Flow<List<Project>> = projectDao.getAllProjects()

    fun getActiveProjects(): Flow<List<Project>> =
        projectDao.getProjectsByStatus(ProjectStatus.ACTIVE)

    fun getProjectById(id: Long): Flow<Project?> = projectDao.getProjectById(id)

    suspend fun getProjectByIdOnce(id: Long): Project? = projectDao.getProjectByIdOnce(id)

    fun getProjectWithWorkLogs(id: Long): Flow<ProjectWithWorkLogs?> =
        projectDao.getProjectWithWorkLogs(id)

    suspend fun insert(project: Project): Long = projectDao.insert(project)

    suspend fun update(project: Project) = projectDao.update(
        project.copy(updatedAt = System.currentTimeMillis())
    )

    suspend fun delete(project: Project) = projectDao.delete(project)

    suspend fun deleteById(id: Long) = projectDao.deleteById(id)

    fun getProjectCount(): Flow<Int> = projectDao.getProjectCount()
}


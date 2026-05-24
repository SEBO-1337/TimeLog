package com.sebo.timelog.data.repositories

import com.sebo.timelog.data.local.dao.ProjectDao
import com.sebo.timelog.data.local.entities.Project
import com.sebo.timelog.data.local.entities.ProjectStatus
import com.sebo.timelog.data.local.relations.ProjectWithWorkLogs
import com.sebo.timelog.data.model.UserRole
import com.sebo.timelog.data.remote.AuthService
import com.sebo.timelog.data.remote.SyncService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val syncService: SyncService? = null,
    private val authService: AuthService? = null
) {

    fun getAllProjects(): Flow<List<Project>> = projectDao.getAllProjects()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getVisibleProjects(): Flow<List<Project>> {
        val auth = authService ?: return getAllProjects()

        return auth.authState.flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                combine(getAllProjects(), auth.observeUserRole()) { allProjects, role ->
                    when (role) {
                        UserRole.ADMIN -> allProjects
                        UserRole.TECHNICIAN -> allProjects.filter { it.createdBy == user.uid }
                        UserRole.CUSTOMER -> emptyList()
                        UserRole.NEW -> emptyList()
                    }
                }
            }
        }
    }

    fun getActiveProjects(): Flow<List<Project>> =
        projectDao.getProjectsByStatus(ProjectStatus.ACTIVE)

    /**
     * Gibt Projekte gefiltert nach Benutzerrolle zurück:
     * - ADMIN: alle Projekte
     * - TECHNICIAN: nur eigene Projekte
     * - CUSTOMER: alle Projekte (Filterung erfolgt über Firestore Rules)
     */
    fun getProjectsForUser(userRole: UserRole, currentUserId: String): Flow<List<Project>> {
        return when (userRole) {
            UserRole.ADMIN -> getAllProjects()
            UserRole.TECHNICIAN -> projectDao.getProjectsByCreator(currentUserId)
            UserRole.CUSTOMER -> flowOf(emptyList())
            UserRole.NEW -> flowOf(emptyList())
        }
    }

    fun getProjectById(id: Long): Flow<Project?> = projectDao.getProjectById(id)

    suspend fun getProjectByIdOnce(id: Long): Project? = projectDao.getProjectByIdOnce(id)

    fun getProjectWithWorkLogs(id: Long): Flow<ProjectWithWorkLogs?> =
        projectDao.getProjectWithWorkLogs(id)

    suspend fun insert(project: Project): Long {
        // Setze createdBy, wenn authService verfügbar
        val cloudId = project.cloudId.ifBlank { UUID.randomUUID().toString() }
        val projectWithCreator = if (authService != null) {
            val uid = authService.currentUser()?.uid ?: ""
            project.copy(createdBy = uid, cloudId = cloudId)
        } else {
            project.copy(cloudId = cloudId)
        }
        val id = projectDao.insert(projectWithCreator)
        syncService?.syncProject(projectWithCreator.copy(id = id))
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

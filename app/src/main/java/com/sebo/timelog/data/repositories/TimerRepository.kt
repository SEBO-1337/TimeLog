package com.sebo.timelog.data.repositories

import com.sebo.timelog.data.local.dao.ProjectDao
import com.sebo.timelog.data.local.dao.TimerDao
import com.sebo.timelog.data.local.entities.Timer
import com.sebo.timelog.data.remote.SyncService
import kotlinx.coroutines.flow.Flow

class TimerRepository(
    private val timerDao: TimerDao,
    private val projectDao: ProjectDao,
    private val syncService: SyncService? = null
) {

    fun getActiveTimer(): Flow<Timer?> = timerDao.getActiveTimer()

    fun getAnyTimer(): Flow<Timer?> = timerDao.getAnyTimer()

    suspend fun getTimerById(id: Long): Timer? = timerDao.getTimerById(id)

    fun getTimerByProject(projectId: Long): Flow<Timer?> = timerDao.getTimerByProject(projectId)

    suspend fun insert(timer: Timer): Long {
        val id = timerDao.insert(timer)
        val projectCloudId = projectDao.getProjectByIdOnce(timer.projectId)?.cloudId
        syncService?.syncTimer(timer.copy(id = id), projectCloudId)
        return id
    }

    suspend fun update(timer: Timer) {
        timerDao.update(timer)
        val projectCloudId = projectDao.getProjectByIdOnce(timer.projectId)?.cloudId
        syncService?.syncTimer(timer, projectCloudId)
    }

    suspend fun delete(timer: Timer) {
        timerDao.delete(timer)
        syncService?.syncTimer(null)
    }

    suspend fun deleteAll() {
        timerDao.deleteAll()
        syncService?.syncTimer(null)
    }

    suspend fun deleteByProjectId(projectId: Long) {
        timerDao.deleteByProjectId(projectId)
        syncService?.syncTimer(null)
    }
}

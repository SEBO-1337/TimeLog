package com.sebo.timelog.data.repositories

import com.sebo.timelog.data.local.dao.WorkLogDao
import com.sebo.timelog.data.local.entities.WorkLog
import com.sebo.timelog.data.remote.SyncService
import kotlinx.coroutines.flow.Flow

class WorkLogRepository(
    private val workLogDao: WorkLogDao,
    private val syncService: SyncService? = null
) {

    fun getAllWorkLogs(): Flow<List<WorkLog>> = workLogDao.getAllWorkLogs()

    fun getWorkLogsByProject(projectId: Long): Flow<List<WorkLog>> =
        workLogDao.getWorkLogsByProject(projectId)

    fun getWorkLogsByDateRange(startDate: Long, endDate: Long): Flow<List<WorkLog>> =
        workLogDao.getWorkLogsByDateRange(startDate, endDate)

    fun getWorkLogsByProjectAndDateRange(
        projectId: Long,
        startDate: Long,
        endDate: Long
    ): Flow<List<WorkLog>> =
        workLogDao.getWorkLogsByProjectAndDateRange(projectId, startDate, endDate)

    fun getWorkLogById(id: Long): Flow<WorkLog?> = workLogDao.getWorkLogById(id)

    fun getTotalHoursForProject(projectId: Long): Flow<Double> =
        workLogDao.getTotalHoursForProject(projectId)

    fun getBilledHoursForProject(projectId: Long): Flow<Double> =
        workLogDao.getBilledHoursForProject(projectId)

    fun getWorkLogCountForProject(projectId: Long): Flow<Int> =
        workLogDao.getWorkLogCountForProject(projectId)

    fun getTotalWorkLogCount(): Flow<Int> = workLogDao.getTotalWorkLogCount()

    fun getTotalHoursAll(): Flow<Double> = workLogDao.getTotalHoursAll()

    fun getLastActivityDate(projectId: Long): Flow<Long?> =
        workLogDao.getLastActivityDate(projectId)

    suspend fun insert(workLog: WorkLog): Long {
        val id = workLogDao.insert(workLog)
        syncService?.syncWorkLog(workLog.copy(id = id))
        return id
    }

    suspend fun insertAll(workLogs: List<WorkLog>) {
        workLogDao.insertAll(workLogs)
        workLogs.forEach { syncService?.syncWorkLog(it) }
    }

    suspend fun update(workLog: WorkLog) {
        val updated = workLog.copy(updatedAt = System.currentTimeMillis())
        workLogDao.update(updated)
        syncService?.syncWorkLog(updated)
    }

    suspend fun delete(workLog: WorkLog) {
        workLogDao.delete(workLog)
        syncService?.deleteWorkLog(workLog.id)
    }

    suspend fun deleteById(id: Long) {
        workLogDao.deleteById(id)
        syncService?.deleteWorkLog(id)
    }
}

package com.sebo.timelog.data.repositories

import com.sebo.timelog.data.local.dao.WorkLogDao
import com.sebo.timelog.data.local.entities.WorkLog
import kotlinx.coroutines.flow.Flow

class WorkLogRepository(private val workLogDao: WorkLogDao) {

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

    suspend fun insert(workLog: WorkLog): Long = workLogDao.insert(workLog)

    suspend fun insertAll(workLogs: List<WorkLog>) = workLogDao.insertAll(workLogs)

    suspend fun update(workLog: WorkLog) = workLogDao.update(
        workLog.copy(updatedAt = System.currentTimeMillis())
    )

    suspend fun delete(workLog: WorkLog) = workLogDao.delete(workLog)

    suspend fun deleteById(id: Long) = workLogDao.deleteById(id)
}


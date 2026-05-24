package com.sebo.timelog.data.repositories

import com.sebo.timelog.data.local.dao.WorkLogDao
import com.sebo.timelog.data.local.entities.WorkLog
import com.sebo.timelog.data.remote.SyncService
import com.sebo.timelog.utils.effectiveBilledHours
import com.sebo.timelog.utils.pendingHours
import com.sebo.timelog.utils.withBilledHours
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

    suspend fun updateBilling(workLog: WorkLog, totalBilledHours: Double) {
        update(workLog.withBilledHours(totalBilledHours))
    }

    suspend fun addBilledHours(workLog: WorkLog, additionalBilledHours: Double) {
        val totalBilledHours = workLog.effectiveBilledHours() + additionalBilledHours.coerceAtLeast(0.0)
        updateBilling(workLog, totalBilledHours)
    }

    suspend fun markAsFullyBilled(workLog: WorkLog) {
        updateBilling(workLog, workLog.hoursWorked)
    }

    suspend fun markProjectPendingAsBilled(projectId: Long) {
        workLogDao.getWorkLogsByProjectOnce(projectId)
            .filter { it.pendingHours() > 0.0001 }
            .forEach { markAsFullyBilled(it) }
    }

    /**
     * Rechnet einen bestimmten Stundenbetrag ab (darf ins Minus gehen).
     * Stunden werden ältesten-zuerst auf die WorkLogs verteilt.
     * Wenn mehr Stunden abgerechnet werden als vorhanden, gehen die restlichen
     * Stunden auf den letzten WorkLog (negative pending hours).
     */
    suspend fun billProjectByHours(projectId: Long, hoursToBill: Double) {
        val workLogs = workLogDao.getWorkLogsByProjectOnce(projectId).sortedBy { it.date }
        if (workLogs.isEmpty()) return

        var remaining = hoursToBill

        // Erst offene Stunden älteste-zuerst abrechnen
        for (log in workLogs) {
            if (remaining <= 0.0001) break
            val alreadyBilled = log.effectiveBilledHours()
            val workedHours = log.hoursWorked.coerceAtLeast(0.0)
            val pendingInLog = workedHours - alreadyBilled
            if (pendingInLog > 0.0001) {
                val toBill = minOf(remaining, pendingInLog)
                update(log.withBilledHours(alreadyBilled + toBill))
                remaining -= toBill
            }
        }

        // Wenn noch Stunden übrig sind → Überzahlung auf den letzten WorkLog buchen
        if (remaining > 0.0001) {
            val lastLog = workLogDao.getWorkLogsByProjectOnce(projectId).maxByOrNull { it.date }
                ?: workLogs.last()
            val newBilled = lastLog.effectiveBilledHours() + remaining
            update(
                lastLog.copy(
                    hoursBilled = newBilled,
                    billableStatus = com.sebo.timelog.data.local.entities.BillableStatus.BILLED,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
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

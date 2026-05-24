package com.sebo.timelog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sebo.timelog.data.local.entities.WorkLog
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkLogDao {

    @Query("SELECT * FROM work_logs ORDER BY date DESC, startTime DESC")
    fun getAllWorkLogs(): Flow<List<WorkLog>>

    @Query("SELECT * FROM work_logs WHERE projectId = :projectId ORDER BY date DESC")
    fun getWorkLogsByProject(projectId: Long): Flow<List<WorkLog>>

    @Query("SELECT * FROM work_logs WHERE projectId = :projectId ORDER BY date DESC")
    suspend fun getWorkLogsByProjectOnce(projectId: Long): List<WorkLog>

    @Query("SELECT * FROM work_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getWorkLogsByDateRange(startDate: Long, endDate: Long): Flow<List<WorkLog>>

    @Query("SELECT * FROM work_logs WHERE projectId = :projectId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getWorkLogsByProjectAndDateRange(projectId: Long, startDate: Long, endDate: Long): Flow<List<WorkLog>>

    @Query("SELECT * FROM work_logs WHERE id = :id")
    fun getWorkLogById(id: Long): Flow<WorkLog?>

    @Query("SELECT * FROM work_logs WHERE id = :id")
    suspend fun getWorkLogByIdOnce(id: Long): WorkLog?

    @Query("SELECT * FROM work_logs WHERE cloudId = :cloudId LIMIT 1")
    suspend fun getWorkLogByCloudIdOnce(cloudId: String): WorkLog?

    @Query("SELECT COALESCE(SUM(hoursWorked), 0.0) FROM work_logs WHERE projectId = :projectId")
    fun getTotalHoursForProject(projectId: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(hoursBilled), 0.0) FROM work_logs WHERE projectId = :projectId")
    fun getBilledHoursForProject(projectId: Long): Flow<Double>

    @Query("SELECT COUNT(*) FROM work_logs WHERE projectId = :projectId")
    fun getWorkLogCountForProject(projectId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM work_logs")
    fun getTotalWorkLogCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(hoursWorked), 0.0) FROM work_logs")
    fun getTotalHoursAll(): Flow<Double>

    @Query("SELECT MAX(date) FROM work_logs WHERE projectId = :projectId")
    fun getLastActivityDate(projectId: Long): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workLog: WorkLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(workLogs: List<WorkLog>)

    @Update
    suspend fun update(workLog: WorkLog)

    @Delete
    suspend fun delete(workLog: WorkLog)

    @Query("DELETE FROM work_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM work_logs WHERE projectId = :projectId")
    suspend fun deleteByProjectId(projectId: Long)
}


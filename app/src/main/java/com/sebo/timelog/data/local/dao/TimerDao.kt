package com.sebo.timelog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sebo.timelog.data.local.entities.Timer
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerDao {

    @Query("SELECT * FROM active_timers WHERE isRunning = 1 LIMIT 1")
    fun getActiveTimer(): Flow<Timer?>

    @Query("SELECT * FROM active_timers LIMIT 1")
    fun getAnyTimer(): Flow<Timer?>

    @Query("SELECT * FROM active_timers WHERE id = :id")
    suspend fun getTimerById(id: Long): Timer?

    @Query("SELECT * FROM active_timers WHERE projectId = :projectId LIMIT 1")
    fun getTimerByProject(projectId: Long): Flow<Timer?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(timer: Timer): Long

    @Update
    suspend fun update(timer: Timer)

    @Delete
    suspend fun delete(timer: Timer)

    @Query("DELETE FROM active_timers")
    suspend fun deleteAll()

    @Query("DELETE FROM active_timers WHERE projectId = :projectId")
    suspend fun deleteByProjectId(projectId: Long)
}


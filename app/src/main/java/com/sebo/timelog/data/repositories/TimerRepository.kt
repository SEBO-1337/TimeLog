package com.sebo.timelog.data.repositories

import com.sebo.timelog.data.local.dao.TimerDao
import com.sebo.timelog.data.local.entities.Timer
import kotlinx.coroutines.flow.Flow

class TimerRepository(private val timerDao: TimerDao) {

    fun getActiveTimer(): Flow<Timer?> = timerDao.getActiveTimer()

    fun getAnyTimer(): Flow<Timer?> = timerDao.getAnyTimer()

    suspend fun getTimerById(id: Long): Timer? = timerDao.getTimerById(id)

    fun getTimerByProject(projectId: Long): Flow<Timer?> = timerDao.getTimerByProject(projectId)

    suspend fun insert(timer: Timer): Long = timerDao.insert(timer)

    suspend fun update(timer: Timer) = timerDao.update(timer)

    suspend fun delete(timer: Timer) = timerDao.delete(timer)

    suspend fun deleteAll() = timerDao.deleteAll()

    suspend fun deleteByProjectId(projectId: Long) = timerDao.deleteByProjectId(projectId)
}


package com.sebo.timelog

import android.app.Application
import com.sebo.timelog.di.AppContainer

import com.sebo.timelog.service.TimerForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TimeLogApplication : Application() {

    lateinit var container: AppContainer
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        observeTimerForService()
    }

    /**
     * Beobachtet den Timer und startet / stoppt den Foreground-Service entsprechend.
     * Der Service stoppt sich selbst, wenn kein Timer mehr vorhanden ist.
     */
    private fun observeTimerForService() {
        appScope.launch {
            container.timerRepository.getAnyTimer().collect { timer ->
                if (timer != null) {
                    startForegroundService(TimerForegroundService.startIntent(this@TimeLogApplication))
                }
            }
        }
    }

    override fun onTerminate() {
        appScope.cancel()
        super.onTerminate()
    }
}

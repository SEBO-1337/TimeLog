package com.sebo.timelog

import android.app.Application
import com.sebo.timelog.di.AppContainer

class TimeLogApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}


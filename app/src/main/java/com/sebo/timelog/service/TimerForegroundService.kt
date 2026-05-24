package com.sebo.timelog.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sebo.timelog.MainActivity
import com.sebo.timelog.R
import com.sebo.timelog.data.local.entities.Timer
import com.sebo.timelog.data.local.entities.WorkLog
import com.sebo.timelog.utils.TimeFormatter
import com.sebo.timelog.utils.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TimerForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "timer_running_channel"
        const val NOTIFICATION_ID = 42

        const val ACTION_START  = "com.sebo.timelog.TIMER_START"
        const val ACTION_PAUSE  = "com.sebo.timelog.TIMER_PAUSE"
        const val ACTION_RESUME = "com.sebo.timelog.TIMER_RESUME"
        const val ACTION_STOP   = "com.sebo.timelog.TIMER_STOP"

        fun startIntent(context: Context) =
            Intent(context, TimerForegroundService::class.java).apply { action = ACTION_START }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observerJob: Job? = null
    private var tickerJob: Job? = null

    @Volatile private var cachedTimer: Timer? = null
    @Volatile private var cachedProjectName: String = "Timer"

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE  -> serviceScope.launch { handlePause() }
            ACTION_RESUME -> serviceScope.launch { handleResume() }
            ACTION_STOP   -> serviceScope.launch { handleStop() }
            else          -> {
                // Muss sofort im Haupt-Thread aufgerufen werden
                startForegroundCompat(buildNotification("Timer", "Starte…", isRunning = true))
                startObserving()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        observerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    // ── Timer-Beobachtung ────────────────────────────────────────────────────

    private fun startObserving() {
        observerJob?.cancel()
        observerJob = serviceScope.launch {
            appContainer.timerRepository.getAnyTimer().collect { timer ->
                if (timer == null) {
                    stopSelf()
                    return@collect
                }
                // Projektnamen laden, wenn sich das Projekt geändert hat
                if (timer.projectId != cachedTimer?.projectId) {
                    cachedProjectName =
                        appContainer.projectRepository.getProjectByIdOnce(timer.projectId)?.name
                            ?: "Timer"
                }
                cachedTimer = timer
            }
        }

        startTicker()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (true) {
                val t = cachedTimer
                if (t != null) {
                    val elapsedMs = computeElapsed(t)
                    val notification = buildNotification(
                        projectName = cachedProjectName,
                        timeText    = TimeFormatter.formatDuration(maxOf(0L, elapsedMs)),
                        isRunning   = t.isRunning
                    )
                    notificationManager.notify(NOTIFICATION_ID, notification)
                }
                delay(1_000)
            }
        }
    }

    // ── Aktionen aus Benachrichtigung ────────────────────────────────────────

    private suspend fun handlePause() {
        val timer = appContainer.timerRepository.getAnyTimer().first() ?: return
        if (!timer.isRunning) return
        val pauseTime = System.currentTimeMillis()
        appContainer.timerRepository.update(
            timer.copy(isRunning = false, pausedAt = pauseTime)
        )
    }

    private suspend fun handleResume() {
        val timer = appContainer.timerRepository.getAnyTimer().first() ?: return
        if (timer.isRunning) return
        val resumeTime = System.currentTimeMillis()
        val pauseStart = timer.pausedAt ?: resumeTime
        val newPausedDuration = timer.pausedDuration + (resumeTime - pauseStart)
        appContainer.timerRepository.update(
            timer.copy(isRunning = true, pausedAt = null, pausedDuration = newPausedDuration)
        )
    }

    private suspend fun handleStop() {
        val timer = appContainer.timerRepository.getAnyTimer().first() ?: return
        val endTime  = System.currentTimeMillis()
        val elapsedMs = computeElapsed(timer, atTime = endTime)
        val hoursWorked = TimeFormatter.roundUpToHalfHour(
            TimeFormatter.millisToHours(maxOf(0L, elapsedMs))
        )
        val workLog = WorkLog(
            projectId   = timer.projectId,
            description = timer.description ?: "",
            hoursWorked = hoursWorked,
            date        = timer.startTime,
            startTime   = timer.startTime,
            endTime     = endTime
        )
        appContainer.workLogRepository.insert(workLog)
        appContainer.timerRepository.deleteAll()
    }

    // ── Hilfsfunktionen ──────────────────────────────────────────────────────

    private fun computeElapsed(timer: Timer, atTime: Long = System.currentTimeMillis()): Long {
        return if (timer.isRunning) {
            atTime - timer.startTime - timer.pausedDuration
        } else {
            val pauseAt = timer.pausedAt ?: atTime
            pauseAt - timer.startTime - timer.pausedDuration
        }
    }

    private fun buildNotification(
        projectName: String,
        timeText: String,
        isRunning: Boolean
    ): Notification {
        // Tap → App öffnen
        val openAppPi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Pause / Fortsetzen
        val pauseResumePi = if (isRunning) {
            PendingIntent.getService(
                this, 1,
                Intent(this, TimerForegroundService::class.java).apply { action = ACTION_PAUSE },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                this, 2,
                Intent(this, TimerForegroundService::class.java).apply { action = ACTION_RESUME },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Stoppen
        val stopPi = PendingIntent.getService(
            this, 3,
            Intent(this, TimerForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (isRunning) "⏱ $timeText" else "⏸ Pausiert • $timeText"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(projectName)
            .setContentText(statusText)
            .setContentIntent(openAppPi)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .addAction(0, if (isRunning) "Pause" else "Fortsetzen", pauseResumePi)
            .addAction(0, "Stoppen", stopPi)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Laufender Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Wird angezeigt, solange ein Timer läuft"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }
}


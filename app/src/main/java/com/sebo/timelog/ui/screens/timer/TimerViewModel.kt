package com.sebo.timelog.ui.screens.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sebo.timelog.data.local.entities.Project
import com.sebo.timelog.data.local.entities.Timer
import com.sebo.timelog.data.local.entities.WorkLog
import com.sebo.timelog.data.repositories.ProjectRepository
import com.sebo.timelog.data.repositories.TimerRepository
import com.sebo.timelog.data.repositories.WorkLogRepository
import com.sebo.timelog.utils.TimeFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class TimerUiState {
    data object Idle : TimerUiState()
    data object Running : TimerUiState()
    data object Paused : TimerUiState()
    data class Error(val message: String) : TimerUiState()
}

class TimerViewModel(
    private val timerRepository: TimerRepository,
    private val workLogRepository: WorkLogRepository,
    projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TimerUiState>(TimerUiState.Idle)
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

    private val _selectedProjectId = MutableStateFlow<Long?>(null)
    val selectedProjectId: StateFlow<Long?> = _selectedProjectId.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    val projects: StateFlow<List<Project>> = projectRepository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentTimer: StateFlow<Timer?> = timerRepository.getAnyTimer()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var tickerJob: Job? = null

    init {
        // Wiederherstellen eines laufenden Timers beim ViewModel-Start
        viewModelScope.launch {
            timerRepository.getAnyTimer().collect { timer ->
                if (timer != null) {
                    _selectedProjectId.value = timer.projectId
                    _description.value = timer.description ?: ""
                    if (timer.isRunning) {
                        _uiState.value = TimerUiState.Running
                        startTicker(timer.startTime, timer.pausedDuration)
                    } else {
                        _uiState.value = TimerUiState.Paused
                        _elapsedMillis.value = System.currentTimeMillis() - timer.startTime - timer.pausedDuration
                    }
                } else {
                    if (_uiState.value != TimerUiState.Idle) {
                        _uiState.value = TimerUiState.Idle
                        _elapsedMillis.value = 0L
                        stopTicker()
                    }
                }
            }
        }
    }

    fun selectProject(projectId: Long) {
        _selectedProjectId.value = projectId
    }

    fun updateDescription(description: String) {
        _description.value = description
    }

    fun startTimer() {
        val projectId = _selectedProjectId.value
        if (projectId == null) {
            _uiState.value = TimerUiState.Error("Bitte wähle ein Projekt aus")
            return
        }

        viewModelScope.launch {
            try {
                val timer = Timer(
                    projectId = projectId,
                    isRunning = true,
                    startTime = System.currentTimeMillis(),
                    description = _description.value.ifEmpty { null }
                )
                timerRepository.deleteAll()
                timerRepository.insert(timer)
                _uiState.value = TimerUiState.Running
            } catch (_: Exception) {
                _uiState.value = TimerUiState.Error("Timer konnte nicht gestartet werden")
            }
        }
    }

    fun pauseTimer() {
        viewModelScope.launch {
            currentTimer.value?.let { timer ->
                val elapsed = System.currentTimeMillis() - timer.startTime - timer.pausedDuration
                timerRepository.update(
                    timer.copy(
                        isRunning = false,
                        pausedDuration = System.currentTimeMillis() - timer.startTime - elapsed
                    )
                )
                _uiState.value = TimerUiState.Paused
                stopTicker()
            }
        }
    }

    fun resumeTimer() {
        viewModelScope.launch {
            currentTimer.value?.let { timer ->
                // Berechne zusätzliche Pausenzeit
                val currentElapsed = _elapsedMillis.value
                val totalTimeFromStart = System.currentTimeMillis() - timer.startTime
                val newPausedDuration = totalTimeFromStart - currentElapsed

                timerRepository.update(
                    timer.copy(
                        isRunning = true,
                        pausedDuration = newPausedDuration
                    )
                )
                _uiState.value = TimerUiState.Running
                startTicker(timer.startTime, newPausedDuration)
            }
        }
    }

    fun stopTimer() {
        viewModelScope.launch {
            currentTimer.value?.let { timer ->
                val endTime = System.currentTimeMillis()
                val totalElapsed = _elapsedMillis.value
                val hoursWorked = TimeFormatter.millisToHours(totalElapsed)

                // WorkLog erstellen
                val workLog = WorkLog(
                    projectId = timer.projectId,
                    description = _description.value.ifEmpty { "" },
                    hoursWorked = hoursWorked,
                    date = timer.startTime,
                    startTime = timer.startTime,
                    endTime = endTime
                )
                workLogRepository.insert(workLog)

                // Timer löschen
                timerRepository.deleteAll()

                // State zurücksetzen
                _uiState.value = TimerUiState.Idle
                _elapsedMillis.value = 0L
                _description.value = ""
                stopTicker()
            }
        }
    }

    fun discardTimer() {
        viewModelScope.launch {
            timerRepository.deleteAll()
            _uiState.value = TimerUiState.Idle
            _elapsedMillis.value = 0L
            _description.value = ""
            stopTicker()
        }
    }

    private fun startTicker(startTime: Long, pausedDuration: Long) {
        stopTicker()
        tickerJob = viewModelScope.launch {
            while (true) {
                _elapsedMillis.value = System.currentTimeMillis() - startTime - pausedDuration
                delay(100) // Update alle 100ms für flüssige Anzeige
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTicker()
    }

    companion object {
        fun factory(
            timerRepository: TimerRepository,
            workLogRepository: WorkLogRepository,
            projectRepository: ProjectRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TimerViewModel(timerRepository, workLogRepository, projectRepository) as T
                }
            }
        }
    }
}


package com.sebo.timelog.ui.screens.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sebo.timelog.ui.components.TimeLogTopAppBar
import com.sebo.timelog.ui.screens.timer.components.ProjectSelector
import com.sebo.timelog.ui.screens.timer.components.StopWatch
import com.sebo.timelog.ui.screens.timer.components.TimerControls

@Composable
fun TimerScreen(
    viewModel: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val elapsedMillis by viewModel.elapsedMillis.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val selectedProjectId by viewModel.selectedProjectId.collectAsState()
    val description by viewModel.description.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val isTimerActive = uiState is TimerUiState.Running || uiState is TimerUiState.Paused

    // Fehler-Snackbar anzeigen
    LaunchedEffect(uiState) {
        if (uiState is TimerUiState.Error) {
            snackbarHostState.showSnackbar((uiState as TimerUiState.Error).message)
        }
    }

    Scaffold(
        topBar = {
            TimeLogTopAppBar(title = "Timer")
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Oberer Bereich: Projekt-Auswahl + Beschreibung
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                ProjectSelector(
                    projects = projects,
                    selectedProjectId = selectedProjectId,
                    onProjectSelected = { viewModel.selectProject(it) },
                    enabled = !isTimerActive,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text("Beschreibung (optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Mitte: Stopwatch
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StopWatch(
                    elapsedMillis = elapsedMillis,
                    isRunning = uiState is TimerUiState.Running
                )

                if (uiState is TimerUiState.Paused) {
                    Text(
                        text = "⏸ Pausiert",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // Unterer Bereich: Controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimerControls(
                    uiState = uiState,
                    onStart = { viewModel.startTimer() },
                    onPause = { viewModel.pauseTimer() },
                    onResume = { viewModel.resumeTimer() },
                    onStop = { viewModel.stopTimer() },
                    onDiscard = { viewModel.discardTimer() }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}


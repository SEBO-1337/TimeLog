package com.sebo.timelog.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sebo.timelog.data.local.entities.WorkLog
import com.sebo.timelog.ui.components.BillingHoursDialog
import com.sebo.timelog.ui.components.ConfirmDeleteDialog
import com.sebo.timelog.ui.components.ConfirmDialog
import com.sebo.timelog.ui.components.EmptyState
import com.sebo.timelog.ui.components.LoadingIndicator
import com.sebo.timelog.ui.components.TimeLogTopAppBar
import com.sebo.timelog.ui.screens.history.components.WorkLogItem
import com.sebo.timelog.utils.effectiveBilledHours
import com.sebo.timelog.utils.pendingHours
import com.sebo.timelog.utils.toFormattedHours

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var workLogToDelete by remember { mutableStateOf<WorkLog?>(null) }
    var workLogToBillPartially by remember { mutableStateOf<WorkLog?>(null) }
    var workLogToBillFully by remember { mutableStateOf<WorkLog?>(null) }

    Scaffold(
        topBar = {
            TimeLogTopAppBar(title = "Verlauf")
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                LoadingIndicator()
            } else {
                // Filter-Chips
                if (uiState.projects.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        item {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = uiState.filterProjectId == null,
                                    onClick = { viewModel.setProjectFilter(null) },
                                    label = { Text("Alle") }
                                )
                                uiState.projects.values.forEach { project ->
                                    FilterChip(
                                        selected = uiState.filterProjectId == project.id,
                                        onClick = { viewModel.setProjectFilter(project.id) },
                                        label = { Text(project.name) }
                                    )
                                }
                            }
                        }
                    }
                }

                // WorkLog-Liste
                if (uiState.workLogs.isEmpty()) {
                    EmptyState(message = "Noch keine Zeiteinträge vorhanden.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.workLogs,
                            key = { it.id }
                        ) { workLog ->
                            WorkLogItem(
                                workLog = workLog,
                                project = uiState.projects[workLog.projectId],
                                onPartialBilling = { workLogToBillPartially = workLog },
                                onMarkAsBilled = { workLogToBillFully = workLog },
                                onDelete = { workLogToDelete = workLog }
                            )
                        }
                    }
                }
            }
        }
    }

    // Lösch-Dialog
    workLogToDelete?.let { workLog ->
        ConfirmDeleteDialog(
            title = "Eintrag löschen",
            message = "Möchtest du diesen Zeiteintrag wirklich löschen?",
            onConfirm = {
                viewModel.deleteWorkLog(workLog)
                workLogToDelete = null
            },
            onDismiss = { workLogToDelete = null }
        )
    }

    workLogToBillPartially?.let { workLog ->
        val pendingHours = workLog.pendingHours()
        BillingHoursDialog(
            title = "Stunden abrechnen",
            message = "Bereits abgerechnet: ${workLog.effectiveBilledHours().toFormattedHours()}\nOffen: ${pendingHours.toFormattedHours()}",
            initialHours = if (pendingHours > 0.0) pendingHours.toString() else "",
            onConfirm = { additionalHours ->
                viewModel.addBilledHours(workLog, additionalHours)
                workLogToBillPartially = null
            },
            onDismiss = { workLogToBillPartially = null }
        )
    }

    workLogToBillFully?.let { workLog ->
        ConfirmDialog(
            title = "Komplett abrechnen",
            message = "Möchtest du die offenen ${workLog.pendingHours().toFormattedHours()} für diesen Eintrag vollständig abrechnen?",
            confirmText = "Abrechnen",
            onConfirm = {
                viewModel.markWorkLogAsBilled(workLog)
                workLogToBillFully = null
            },
            onDismiss = { workLogToBillFully = null }
        )
    }
}


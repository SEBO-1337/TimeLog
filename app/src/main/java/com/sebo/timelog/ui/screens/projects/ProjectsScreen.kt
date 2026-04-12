package com.sebo.timelog.ui.screens.projects

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sebo.timelog.data.local.entities.Project
import com.sebo.timelog.ui.components.ConfirmDeleteDialog
import com.sebo.timelog.ui.components.EmptyState
import com.sebo.timelog.ui.components.LoadingIndicator
import com.sebo.timelog.ui.components.TimeLogTopAppBar
import com.sebo.timelog.ui.screens.projects.components.ProjectForm
import com.sebo.timelog.ui.screens.projects.components.ProjectList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: ProjectsViewModel,
    onProjectClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var projectToEdit by remember { mutableStateOf<Project?>(null) }
    var projectToDelete by remember { mutableStateOf<Project?>(null) }

    Scaffold(
        topBar = {
            TimeLogTopAppBar(title = "Projekte")
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Neues Projekt")
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is ProjectsUiState.Loading -> LoadingIndicator()
                is ProjectsUiState.Error -> {
                    EmptyState(message = state.message)
                }
                is ProjectsUiState.Success -> {
                    if (state.projects.isEmpty()) {
                        EmptyState(
                            message = "Noch keine Projekte.\nErstelle dein erstes Projekt!"
                        )
                    } else {
                        ProjectList(
                            projects = state.projects,
                            onProjectClick = { project -> onProjectClick(project.id) },
                            onEditProject = { project -> projectToEdit = project },
                            onDeleteProject = { project -> projectToDelete = project }
                        )
                    }
                }
            }
        }
    }

    // Dialog: Neues Projekt erstellen
    if (showCreateDialog) {
        ProjectCreateEditDialog(
            title = "Neues Projekt",
            onDismiss = { showCreateDialog = false },
            onSave = { name, description, color, hourlyRate ->
                viewModel.createProject(name, description, color, hourlyRate)
                showCreateDialog = false
            }
        )
    }

    // Dialog: Projekt bearbeiten
    projectToEdit?.let { project ->
        ProjectCreateEditDialog(
            title = "Projekt bearbeiten",
            initialName = project.name,
            initialDescription = project.description ?: "",
            initialColor = project.color,
            initialHourlyRate = project.hourlyRate,
            onDismiss = { projectToEdit = null },
            onSave = { name, description, color, hourlyRate ->
                viewModel.updateProject(
                    project.copy(
                        name = name.trim(),
                        description = description?.trim()?.ifEmpty { null },
                        color = color,
                        hourlyRate = hourlyRate
                    )
                )
                projectToEdit = null
            }
        )
    }

    // Dialog: Projekt löschen bestätigen
    projectToDelete?.let { project ->
        ConfirmDeleteDialog(
            title = "Projekt löschen",
            message = "Möchtest du \"${project.name}\" wirklich löschen?\nAlle zugehörigen Zeiteinträge werden ebenfalls gelöscht.",
            onConfirm = {
                viewModel.deleteProject(project)
                projectToDelete = null
            },
            onDismiss = { projectToDelete = null }
        )
    }
}

@Composable
private fun ProjectCreateEditDialog(
    title: String,
    initialName: String = "",
    initialDescription: String = "",
    initialColor: String = "#2196F3",
    initialHourlyRate: Double = 0.0,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String?, color: String, hourlyRate: Double) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var color by remember { mutableStateOf(initialColor) }
    var hourlyRate by remember {
        mutableStateOf(
            if (initialHourlyRate > 0) initialHourlyRate.toString() else ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            ProjectForm(
                name = name,
                onNameChange = { name = it },
                description = description,
                onDescriptionChange = { description = it },
                hourlyRate = hourlyRate,
                onHourlyRateChange = { hourlyRate = it },
                selectedColor = color,
                onColorSelected = { color = it }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        name,
                        description.ifEmpty { null },
                        color,
                        hourlyRate.toDoubleOrNull() ?: 0.0
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}


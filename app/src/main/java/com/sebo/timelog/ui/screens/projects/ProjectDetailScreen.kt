package com.sebo.timelog.ui.screens.projects

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sebo.timelog.ui.components.LoadingIndicator
import com.sebo.timelog.ui.components.TimeLogDetailTopAppBar

@SuppressLint("DefaultLocale")
@Composable
fun ProjectDetailScreen(
    viewModel: ProjectsViewModel,
    projectId: Long,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()
    val project = projects.find { it.id == projectId }

    Scaffold(
        topBar = {
            TimeLogDetailTopAppBar(
                title = project?.name ?: "Projekt",
                onNavigateBack = onNavigateBack
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (project == null) {
                LoadingIndicator()
            } else {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.headlineMedium
                )
                if (!project.description.isNullOrEmpty()) {
                    Text(
                        text = project.description,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Text(
                    text = "Status: ${project.status.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (project.hourlyRate > 0) {
                    Text(
                        text = String.format("Stundensatz: %.2f €", project.hourlyRate),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}


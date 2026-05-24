package com.sebo.timelog.ui.screens.projects.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sebo.timelog.data.local.entities.Project

@Composable
fun ProjectList(
    projects: List<Project>,
    onProjectClick: (Project) -> Unit,
    canEditProject: (Project) -> Boolean,
    canDeleteProject: (Project) -> Boolean,
    onEditProject: (Project) -> Unit,
    onDeleteProject: (Project) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = projects,
            key = { it.id }
        ) { project ->
            ProjectCard(
                project = project,
                onClick = { onProjectClick(project) },
                canEdit = canEditProject(project),
                canDelete = canDeleteProject(project),
                onEdit = { onEditProject(project) },
                onDelete = { onDeleteProject(project) }
            )
        }
    }
}


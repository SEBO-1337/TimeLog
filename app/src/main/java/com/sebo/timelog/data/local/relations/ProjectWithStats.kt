package com.sebo.timelog.data.local.relations

import com.sebo.timelog.data.local.entities.Project
import com.sebo.timelog.data.local.entities.ProjectStats

data class ProjectWithStats(
    val project: Project,
    val stats: ProjectStats
)


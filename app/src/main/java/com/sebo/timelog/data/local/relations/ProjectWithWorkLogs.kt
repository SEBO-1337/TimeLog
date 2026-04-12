package com.sebo.timelog.data.local.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.sebo.timelog.data.local.entities.Project
import com.sebo.timelog.data.local.entities.WorkLog

data class ProjectWithWorkLogs(
    @Embedded val project: Project,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val workLogs: List<WorkLog>
)


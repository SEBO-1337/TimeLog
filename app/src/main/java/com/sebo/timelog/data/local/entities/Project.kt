package com.sebo.timelog.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ProjectStatus {
    ACTIVE, PAUSED, ARCHIVED
}

@Entity(
    tableName = "projects",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["createdBy"]),
        Index(value = ["cloudId"], unique = true)
    ]
)
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val color: String = "#2196F3",
    val hourlyRate: Double = 0.0,
    val status: ProjectStatus = ProjectStatus.ACTIVE,
    val cloudId: String = "",   // Stabiler Cloud-Schluessel (UUID)
    val createdBy: String = "",  // Firebase UID des Erstellers
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)


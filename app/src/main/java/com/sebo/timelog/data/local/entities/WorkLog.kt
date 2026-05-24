package com.sebo.timelog.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class BillableStatus {
    UNBILLED, BILLED, PARTIAL
}

@Entity(
    tableName = "work_logs",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["date"]),
        Index(value = ["billableStatus"]),
        Index(value = ["cloudId"], unique = true)
    ]
)
data class WorkLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val description: String = "",
    val hoursWorked: Double,
    val hoursBilled: Double = 0.0,
    val date: Long,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val billableStatus: BillableStatus = BillableStatus.UNBILLED,
    val notes: String? = null,
    val tags: String? = null,
    val cloudId: String = "",   // Stabiler Cloud-Schluessel (UUID)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)


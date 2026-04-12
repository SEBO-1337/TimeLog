package com.sebo.timelog.data.local.entities

/**
 * DTO für Projektstatistiken - kein Room Entity
 */
data class ProjectStats(
    val totalHours: Double = 0.0,
    val billedHours: Double = 0.0,
    val pendingHours: Double = 0.0,
    val estimatedRevenue: Double = 0.0,
    val workLogCount: Int = 0,
    val lastActivityDate: Long? = null
)


package com.sebo.timelog.utils

object Constants {
    const val DATABASE_NAME = "timelog_database"

    // Notification Channels
    const val TIMER_CHANNEL_ID = "timer_channel"
    const val TIMER_CHANNEL_NAME = "Timer läuft"
    const val REMINDERS_CHANNEL_ID = "reminders_channel"
    const val REMINDERS_CHANNEL_NAME = "Erinnerungen"

    // Default-Farben für Projekte
    val PROJECT_COLORS = listOf(
        "#2196F3", // Blau
        "#4CAF50", // Grün
        "#FF9800", // Orange
        "#E91E63", // Pink
        "#9C27B0", // Violett
        "#00BCD4", // Cyan
        "#FF5722", // Deep Orange
        "#607D8B", // Blaugrau
        "#795548", // Braun
        "#F44336", // Rot
        "#3F51B5", // Indigo
        "#009688"  // Teal
    )

    // DataStore Keys
    const val DATASTORE_NAME = "timelog_preferences"

    // Limits
    const val MAX_SESSION_HOURS = 24.0
    const val DEFAULT_HOURLY_RATE = 0.0
}


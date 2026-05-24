package com.sebo.timelog.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screens(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Timer : Screens(
        route = "timer",
        title = "Timer",
        icon = Icons.Default.Timer
    )

    data object Projects : Screens(
        route = "projects",
        title = "Projekte",
        icon = Icons.Default.FolderOpen
    )

    data object History : Screens(
        route = "history",
        title = "Verlauf",
        icon = Icons.Default.History
    )

    data object Statistics : Screens(
        route = "statistics",
        title = "Statistiken",
        icon = Icons.Default.BarChart
    )

    data object Settings : Screens(
        route = "settings",
        title = "Einstellungen",
        icon = Icons.Default.Settings
    )

    companion object {
        // Bottom Navigation Items
        val bottomNavItems = listOf(Timer, Projects, History, Statistics, Settings)
    }
}

// Separate Routen für Detail-Screens (nicht in BottomNav)
object DetailRoutes {
    const val PROJECT_DETAIL    = "project_detail/{projectId}"
    const val DATA_MANAGEMENT   = "data_management"

    fun projectDetail(projectId: Long) = "project_detail/$projectId"
}

object AuthRoutes {
    const val LOGIN = "auth_login"
    const val REGISTER = "auth_register"
}


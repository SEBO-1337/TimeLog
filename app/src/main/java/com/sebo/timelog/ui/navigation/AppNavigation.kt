package com.sebo.timelog.ui.navigation

import android.R.attr.type
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sebo.timelog.ui.screens.history.HistoryScreen
import com.sebo.timelog.ui.screens.history.HistoryViewModel
import com.sebo.timelog.ui.screens.projects.ProjectDetailScreen
import com.sebo.timelog.ui.screens.projects.ProjectsScreen
import com.sebo.timelog.ui.screens.projects.ProjectsViewModel
import com.sebo.timelog.ui.screens.settings.SettingsScreen
import com.sebo.timelog.ui.screens.statistics.StatisticsScreen
import com.sebo.timelog.ui.screens.statistics.StatisticsViewModel
import com.sebo.timelog.ui.screens.timer.TimerScreen
import com.sebo.timelog.ui.screens.timer.TimerViewModel
import com.sebo.timelog.utils.appContainer

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val container = context.appContainer

    // Shared ViewModels
    val projectsViewModel: ProjectsViewModel = viewModel(
        factory = ProjectsViewModel.factory(container.projectRepository)
    )

    val timerViewModel: TimerViewModel = viewModel(
        factory = TimerViewModel.factory(
            container.timerRepository,
            container.workLogRepository,
            container.projectRepository
        )
    )

    val historyViewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModel.factory(
            container.workLogRepository,
            container.projectRepository
        )
    )

    val statisticsViewModel: StatisticsViewModel = viewModel(
        factory = StatisticsViewModel.factory(
            container.workLogRepository,
            container.projectRepository
        )
    )

    NavHost(
        navController = navController,
        startDestination = Screens.Timer.route,
        modifier = modifier
    ) {
        composable(Screens.Timer.route) {
            TimerScreen(viewModel = timerViewModel)
        }

        composable(Screens.Projects.route) {
            ProjectsScreen(
                viewModel = projectsViewModel,
                onProjectClick = { projectId ->
                    navController.navigate(DetailRoutes.projectDetail(projectId))
                }
            )
        }

        composable(Screens.History.route) {
            HistoryScreen(viewModel = historyViewModel)
        }

        composable(Screens.Statistics.route) {
            StatisticsScreen(viewModel = statisticsViewModel)
        }

        composable(Screens.Settings.route) {
            SettingsScreen()
        }

        composable(
            route = DetailRoutes.PROJECT_DETAIL,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            ProjectDetailScreen(
                viewModel = projectsViewModel,
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}


package com.sebo.timelog.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.sebo.timelog.ui.screens.auth.AuthViewModel
import com.sebo.timelog.ui.screens.auth.LoginScreen
import com.sebo.timelog.ui.screens.auth.RegisterScreen
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

    val authService = container.authService
    if (authService == null) {
        // Ohne AuthService wird die App wie bisher ohne Login-Gate geladen.
        AppContentNavHost(navController = navController, modifier = modifier)
        return
    }

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.factory(authService)
    )
    val authState by authViewModel.uiState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

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

    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated) {
            if (currentRoute == AuthRoutes.LOGIN || currentRoute == AuthRoutes.REGISTER) {
                navController.navigate(Screens.Timer.route) {
                    popUpTo(AuthRoutes.LOGIN) { inclusive = true }
                    launchSingleTop = true
                }
            }
        } else {
            if (currentRoute != AuthRoutes.LOGIN && currentRoute != AuthRoutes.REGISTER) {
                navController.navigate(AuthRoutes.LOGIN) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (authState.isAuthenticated) Screens.Timer.route else AuthRoutes.LOGIN,
        modifier = modifier
    ) {
        composable(AuthRoutes.LOGIN) {
            LoginScreen(
                uiState = authState,
                onLogin = authViewModel::login,
                onNavigateToRegister = { navController.navigate(AuthRoutes.REGISTER) }
            )
        }

        composable(AuthRoutes.REGISTER) {
            RegisterScreen(
                uiState = authState,
                onRegister = authViewModel::register,
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        addMainGraph(
            navController = navController,
            projectsViewModel = projectsViewModel,
            timerViewModel = timerViewModel,
            historyViewModel = historyViewModel,
            statisticsViewModel = statisticsViewModel,
            onLogout = { authViewModel.logout() }
        )
    }
}

@Composable
private fun AppContentNavHost(
    navController: NavHostController,
    modifier: Modifier
) {
    val context = LocalContext.current
    val container = context.appContainer

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
        addMainGraph(
            navController = navController,
            projectsViewModel = projectsViewModel,
            timerViewModel = timerViewModel,
            historyViewModel = historyViewModel,
            statisticsViewModel = statisticsViewModel,
            onLogout = {}
        )
    }
}

private fun androidx.navigation.NavGraphBuilder.addMainGraph(
    navController: NavHostController,
    projectsViewModel: ProjectsViewModel,
    timerViewModel: TimerViewModel,
    historyViewModel: HistoryViewModel,
    statisticsViewModel: StatisticsViewModel,
    onLogout: () -> Unit
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
        SettingsScreen(onLogout = onLogout)
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


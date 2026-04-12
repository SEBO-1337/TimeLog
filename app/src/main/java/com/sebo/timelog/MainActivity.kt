package com.sebo.timelog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sebo.timelog.ui.components.TimeLogBottomNavigation
import com.sebo.timelog.ui.navigation.AppNavigation
import com.sebo.timelog.ui.navigation.AuthRoutes
import com.sebo.timelog.ui.navigation.DetailRoutes
import com.sebo.timelog.ui.navigation.Screens
import com.sebo.timelog.ui.theme.TimeLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TimeLogTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val isBottomRoute = Screens.bottomNavItems.any { it.route == currentRoute }
                val isAuthRoute = currentRoute == AuthRoutes.LOGIN || currentRoute == AuthRoutes.REGISTER
                val showBottomBar = isBottomRoute && !isAuthRoute && currentRoute != DetailRoutes.PROJECT_DETAIL

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            TimeLogBottomNavigation(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    AppNavigation(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

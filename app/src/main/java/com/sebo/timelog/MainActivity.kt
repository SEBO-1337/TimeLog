package com.sebo.timelog

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val splashView = splashScreenViewProvider.view

            val slideUp = ObjectAnimator.ofFloat(
                splashView,
                View.TRANSLATION_Y,
                0f,
                -(splashView.height * 0.22f)
            )
            val zoomX = ObjectAnimator.ofFloat(splashView, View.SCALE_X, 1f, 1.12f)
            val zoomY = ObjectAnimator.ofFloat(splashView, View.SCALE_Y, 1f, 1.12f)
            val fadeOut = ObjectAnimator.ofFloat(splashView, View.ALPHA, 1f, 0f)

            AnimatorSet().apply {
                duration = 520L
                interpolator = OvershootInterpolator(0.75f)
                playTogether(slideUp, zoomX, zoomY, fadeOut)
                doOnEnd { splashScreenViewProvider.remove() }
                start()
            }
        }

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

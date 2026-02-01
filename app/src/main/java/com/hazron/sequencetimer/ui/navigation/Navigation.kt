package com.hazron.sequencetimer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hazron.sequencetimer.ui.screens.edit.EditTimerScreen
import com.hazron.sequencetimer.ui.screens.home.HomeScreen
import com.hazron.sequencetimer.ui.screens.timer.TimerScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Timer : Screen("timer/{timerId}") {
        fun createRoute(timerId: Long) = "timer/$timerId"
    }
    object EditTimer : Screen("edit/{timerId}") {
        fun createRoute(timerId: Long?) = "edit/${timerId ?: -1}"
    }
}

@Composable
fun SequenceTimerNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onTimerClick = { timerId ->
                    navController.navigate(Screen.Timer.createRoute(timerId))
                },
                onAddTimer = {
                    navController.navigate(Screen.EditTimer.createRoute(null))
                },
                onEditTimer = { timerId ->
                    navController.navigate(Screen.EditTimer.createRoute(timerId))
                }
            )
        }

        composable(
            route = Screen.Timer.route,
            arguments = listOf(
                navArgument("timerId") { type = NavType.LongType }
            )
        ) {
            TimerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditTimer.route,
            arguments = listOf(
                navArgument("timerId") { type = NavType.LongType }
            )
        ) {
            EditTimerScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
    }
}

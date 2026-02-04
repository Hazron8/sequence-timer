package com.hazron.sequencetimer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hazron.sequencetimer.ui.screens.edit.EditTimerScreen
import com.hazron.sequencetimer.ui.screens.home.HomeScreen
import com.hazron.sequencetimer.ui.screens.sequence.SequenceBuilderScreen
import com.hazron.sequencetimer.ui.screens.sequence.SequencePlaybackScreen
import com.hazron.sequencetimer.ui.screens.settings.SettingsScreen
import com.hazron.sequencetimer.ui.screens.timer.TimerScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Timer : Screen("timer/{timerId}") {
        fun createRoute(timerId: Long) = "timer/$timerId"
    }
    object EditTimer : Screen("edit/{timerId}") {
        fun createRoute(timerId: Long?) = "edit/${timerId ?: -1}"
    }
    object SequenceBuilder : Screen("sequence/edit/{sequenceId}") {
        fun createRoute(sequenceId: Long?) = "sequence/edit/${sequenceId ?: -1}"
    }
    object SequencePlayback : Screen("sequence/play/{sequenceId}") {
        fun createRoute(sequenceId: Long) = "sequence/play/$sequenceId"
    }
    object Settings : Screen("settings")
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
                },
                onSequenceClick = { sequenceId ->
                    navController.navigate(Screen.SequencePlayback.createRoute(sequenceId))
                },
                onAddSequence = {
                    navController.navigate(Screen.SequenceBuilder.createRoute(null))
                },
                onEditSequence = { sequenceId ->
                    navController.navigate(Screen.SequenceBuilder.createRoute(sequenceId))
                },
                onSettings = {
                    navController.navigate(Screen.Settings.route)
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

        composable(
            route = Screen.SequenceBuilder.route,
            arguments = listOf(
                navArgument("sequenceId") { type = NavType.LongType }
            )
        ) {
            SequenceBuilderScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SequencePlayback.route,
            arguments = listOf(
                navArgument("sequenceId") { type = NavType.LongType }
            )
        ) {
            SequencePlaybackScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

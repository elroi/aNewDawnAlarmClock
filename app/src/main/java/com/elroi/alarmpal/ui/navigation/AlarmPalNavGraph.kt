package com.elroi.alarmpal.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.elroi.alarmpal.ui.screen.alarm.AlarmListScreen
import com.elroi.alarmpal.ui.screen.onboarding.OnboardingScreen

@Composable
fun AlarmPalNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.AlarmList.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Screen.AlarmList.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AlarmList.route) {
            AlarmListScreen(
                onNavigateToDetail = { alarmId ->
                    navController.navigate(Screen.AlarmDetail.createRoute(alarmId ?: ""))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(
            route = Screen.AlarmDetail.route,
            arguments = Screen.AlarmDetail.arguments
        ) { backStackEntry ->
            val alarmId = backStackEntry.arguments?.getString("alarmId")
            com.elroi.alarmpal.ui.screen.alarm.AlarmDetailScreen(
                alarmId = alarmId,
                onNavigateUp = { navController.navigateUp() }
            )
        }

        composable(Screen.SleepTracking.route) {
            com.elroi.alarmpal.ui.screen.sleep.SleepTrackingScreen()
        }

        composable(Screen.Settings.route) {
            com.elroi.alarmpal.ui.screen.settings.SettingsScreen(
                onNavigateUp = { navController.navigateUp() },
                onNavigateToHelp = { navController.navigate(Screen.Help.route) }
            )
        }

        composable(Screen.Help.route) {
            com.elroi.alarmpal.ui.screen.settings.HelpScreen(navController = navController)
        }
    }
}


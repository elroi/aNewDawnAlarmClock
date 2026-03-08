package com.elroi.alarmpal.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.elroi.alarmpal.ui.screen.alarm.AlarmListScreen
import com.elroi.alarmpal.ui.screen.onboarding.OnboardingScreen

@Composable
fun LemurLoopNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.AlarmList.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(
            route = Screen.Onboarding.route,
            arguments = Screen.Onboarding.arguments
        ) { backStackEntry ->
            val isReplay = backStackEntry.arguments?.getBoolean("isReplay") ?: false
            OnboardingScreen(
                isReplay = isReplay,
                onFinished = { createAlarm ->
                    if (createAlarm) {
                        // First establishing the AlarmList as the base of the backstack
                        navController.navigate(Screen.AlarmList.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                        // Then navigate to the wizard (or detail if they changed setting)
                        navController.navigate(Screen.AlarmWizard.route)
                    } else {
                        navController.navigate(Screen.AlarmList.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.AlarmList.route) {
            AlarmListScreen(
                onNavigateToDetail = { alarmId ->
                    if (alarmId == "WIZARD") {
                        navController.navigate(Screen.AlarmWizard.route)
                    } else {
                        navController.navigate(Screen.AlarmDetail.createRoute(alarmId ?: ""))
                    }
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
                onNavigateUp = { navController.navigateUp() },
                onSwitchToWizard = {
                    navController.navigate(Screen.AlarmWizard.route) {
                        popUpTo(Screen.AlarmDetail.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AlarmWizard.route) {
            com.elroi.alarmpal.ui.screen.alarm.AlarmCreationWizard(
                onFinished = { 
                    navController.popBackStack(Screen.AlarmList.route, false)
                },
                onBack = { navController.popBackStack() },
                onSwitchToSimple = {
                    navController.navigate(Screen.AlarmDetail.createRoute("")) {
                        popUpTo(Screen.AlarmWizard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SleepTracking.route) {
            com.elroi.alarmpal.ui.screen.sleep.SleepTrackingScreen()
        }

        composable(Screen.Settings.route) {
            com.elroi.alarmpal.ui.screen.settings.SettingsScreen(
                onNavigateUp = { navController.navigateUp() },
                onNavigateToHelp = { navController.navigate(Screen.Help.route) },
                onNavigateToOnboarding = { navController.navigate(Screen.Onboarding.createRoute(isReplay = true)) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onNavigateToLogs = { navController.navigate(Screen.DiagnosticLogs.route) }
            )
        }

        composable(Screen.Help.route) {
            com.elroi.alarmpal.ui.screen.settings.HelpScreen(navController = navController)
        }

        composable(Screen.About.route) {
            com.elroi.alarmpal.ui.screen.settings.AboutScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(Screen.DiagnosticLogs.route) {
            com.elroi.alarmpal.ui.screen.settings.DiagnosticLogsScreen(
                onNavigateUp = { navController.navigateUp() }
            )
        }
    }
}


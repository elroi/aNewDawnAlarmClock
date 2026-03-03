package com.elroi.alarmpal.ui.navigation

sealed class Screen(val route: String) {
    object AlarmList : Screen("alarm_list")
    object AlarmDetail : Screen("alarm_detail?alarmId={alarmId}") {
        fun createRoute(alarmId: String) = "alarm_detail?alarmId=$alarmId"
        val arguments = listOf(
            androidx.navigation.navArgument("alarmId") {
                type = androidx.navigation.NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    }
    object SleepTracking : Screen("sleep_tracking")
    object Settings : Screen("settings")
    object Help : Screen("help")
    object Onboarding : Screen("onboarding?isReplay={isReplay}") {
        fun createRoute(isReplay: Boolean = false) = "onboarding?isReplay=$isReplay"
        val arguments = listOf(
            androidx.navigation.navArgument("isReplay") {
                type = androidx.navigation.NavType.BoolType
                defaultValue = false
            }
        )
    }
    object AlarmWizard : Screen("alarm_wizard")
    object About : Screen("about")
}

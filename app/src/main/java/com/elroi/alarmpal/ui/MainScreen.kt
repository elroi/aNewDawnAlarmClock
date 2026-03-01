package com.elroi.alarmpal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.elroi.alarmpal.BuildConfig
import com.elroi.alarmpal.ui.navigation.AlarmPalNavGraph
import com.elroi.alarmpal.ui.navigation.Screen
import com.elroi.alarmpal.ui.viewmodel.OnboardingViewModel

@Composable
fun MainScreen(
    onboardingViewModel: OnboardingViewModel = hiltViewModel()
) {
    val isOnboardingComplete by onboardingViewModel.isOnboardingComplete.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    // Determine start destination: show onboarding only on fresh installs.
    // isOnboardingComplete starts as `false` (DataStore default) and emits the real value quickly.
    // We use a null-initial state trick via StateFlow's initialValue so we don't flicker to
    // AlarmList before DataStore emits. The stateIn in OnboardingViewModel uses WhileSubscribed
    // so the first emission arrives almost immediately.
    val startDestination = if (isOnboardingComplete) {
        Screen.AlarmList.route
    } else {
        Screen.Onboarding.route
    }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val isBottomBarVisible = currentDestination?.hierarchy?.any { 
                it.route == Screen.AlarmList.route || it.route == Screen.SleepTracking.route 
            } == true

            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Box {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = "Alarms") },
                            label = { Text("Alarms") },
                            selected = currentDestination?.hierarchy?.any { it.route == Screen.AlarmList.route } == true,
                            onClick = {
                            navController.navigate(Screen.AlarmList.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Sleep") },
                        label = { Text("Sleep") },
                        selected = currentDestination?.hierarchy?.any { it.route == Screen.SleepTracking.route } == true,
                        onClick = {
                            navController.navigate(Screen.SleepTracking.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                    // Version badge — bottom-end corner of the nav bar
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 6.dp, bottom = 4.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(innerPadding)) {
            AlarmPalNavGraph(
                navController = navController,
                startDestination = startDestination
            )
        }
    }
}


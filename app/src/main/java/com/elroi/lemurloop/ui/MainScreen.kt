package com.elroi.lemurloop.ui

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
import androidx.compose.runtime.remember
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
import com.elroi.lemurloop.BuildConfig
import com.elroi.lemurloop.ui.navigation.LemurLoopNavGraph
import com.elroi.lemurloop.ui.navigation.Screen
import com.elroi.lemurloop.ui.viewmodel.OnboardingViewModel

@Composable
fun MainScreen(
    onboardingViewModel: OnboardingViewModel = hiltViewModel()
) {
    val isOnboardingComplete by onboardingViewModel.isOnboardingComplete.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    
    // Lock the start destination once onboarding status is loaded.
    // This prevents the NavHost from resetting if isOnboardingComplete changes during the session.
    val startDestination = remember(isOnboardingComplete != null) {
        if (isOnboardingComplete == true) {
            Screen.AlarmList.route
        } else {
            Screen.Onboarding.route
        }
    }

    if (isOnboardingComplete == null) {
        // Still loading from DataStore, show a simple splash or empty box
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading...", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    Scaffold { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            LemurLoopNavGraph(
                navController = navController,
                startDestination = startDestination
            )
        }
    }
}


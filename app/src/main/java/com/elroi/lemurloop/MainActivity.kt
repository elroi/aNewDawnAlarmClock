package com.elroi.lemurloop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.elroi.lemurloop.ui.theme.LemurLoopTheme
import dagger.hilt.android.AndroidEntryPoint

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }
        
        super.onCreate(savedInstanceState)
        
        // Artificial delay for branding
        lifecycleScope.launch {
            delay(1500)
            keepSplash = false
        }
        
        setContent {
            LemurLoopTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    com.elroi.lemurloop.ui.MainScreen()
                }
            }
        }
    }
}

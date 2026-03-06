package com.elroi.alarmpal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.elroi.alarmpal.ui.theme.LemurLoopTheme
import dagger.hilt.android.AndroidEntryPoint

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import javax.inject.Inject
import com.elroi.alarmpal.domain.repository.AlarmRepository
import com.elroi.alarmpal.domain.model.Alarm
import java.time.LocalTime
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var alarmRepository: AlarmRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }
        
        super.onCreate(savedInstanceState)
        
        // Artificial delay for branding
        lifecycleScope.launch {
            delay(1500)
            keepSplash = false
            
            // DEMO SEEDING CODE
            val alarms = alarmRepository.getAllAlarms().first()
            if (alarms.isEmpty()) {
                val alarm1 = Alarm(
                    time = LocalTime.of(6, 0),
                    label = "Morning Workout",
                    daysOfWeek = listOf(2, 3, 4, 5, 6), // Mon-Fri
                    mathDifficulty = 3, // Hard
                    isEvasiveSnooze = true,
                    buddyName = "Gym Bro Dave",
                    buddyPhoneNumber = "+1234567890",
                    isBriefingEnabled = true,
                    aiPersona = "COACH"
                )
                val alarm2 = Alarm(
                    time = LocalTime.of(8, 30),
                    label = "Weekend Adventure",
                    daysOfWeek = listOf(1, 7), // Sat-Sun
                    smileToDismiss = true,
                    buddyName = null,
                    isBriefingEnabled = true,
                    aiPersona = "HYPEMAN"
                )
                val alarm3 = Alarm(
                    time = LocalTime.of(4, 15),
                    label = "Early Flight",
                    daysOfWeek = emptyList(), // One-time
                    isGentleWake = true,
                    crescendoDurationMinutes = 5,
                    buddyName = "Wife",
                    buddyPhoneNumber = "+0987654321",
                    isBriefingEnabled = true,
                    aiPersona = "ZEN"
                )
                alarmRepository.insertAlarm(alarm1)
                alarmRepository.insertAlarm(alarm2)
                alarmRepository.insertAlarm(alarm3)
            }
        }
        
        setContent {
            LemurLoopTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    com.elroi.alarmpal.ui.MainScreen()
                }
            }
        }
    }
}

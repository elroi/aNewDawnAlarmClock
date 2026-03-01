package com.elroi.alarmpal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elroi.alarmpal.domain.manager.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {

    val isOnboardingComplete: StateFlow<Boolean> = settingsManager.onboardingCompleteFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    suspend fun completeOnboarding() {
        settingsManager.saveOnboardingComplete()
    }
}

package com.elroi.alarmpal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elroi.alarmpal.domain.manager.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {

    val isOnboardingComplete: StateFlow<Boolean?> = settingsManager.onboardingCompleteFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _userName = kotlinx.coroutines.flow.MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    init {
        viewModelScope.launch {
            // Initialize with the current value from settings
            settingsManager.alarmDefaultsFlow.map { it.briefingUserName }.collect { name ->
                if (_userName.value.isEmpty() && name.isNotEmpty()) {
                    _userName.value = name
                }
            }
        }
    }

    suspend fun completeOnboarding() {
        settingsManager.saveOnboardingComplete()
    }

    fun addGlobalBuddy(name: String, phoneNumber: String) {
        viewModelScope.launch {
            settingsManager.addGlobalBuddy(name, phoneNumber)
        }
    }

    fun updateUserName(name: String) {
        _userName.value = name
        viewModelScope.launch {
            settingsManager.saveBriefingUserName(name)
        }
    }

    fun setAutoLocation(isAuto: Boolean) {
        viewModelScope.launch {
            settingsManager.saveIsAutoLocation(isAuto)
        }
    }
}

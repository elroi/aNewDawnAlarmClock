package com.elroi.alarmpal.domain.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object BriefingStateManager {
    private val _briefingState = MutableStateFlow<BriefingState>(BriefingState.Idle)
    val briefingState: StateFlow<BriefingState> = _briefingState

    private val _liveHealth = MutableStateFlow<Map<String, String>>(emptyMap())
    val liveHealth: StateFlow<Map<String, String>> = _liveHealth

    fun startGenerating(message: String? = "Generating...") {
        _liveHealth.value = mapOf("weather" to "pending", "calendar" to "pending", "ai" to "pending")
        _briefingState.value = BriefingState.Generating(message)
    }

    fun updateStatus(message: String) {
        _briefingState.value = BriefingState.Generating(message)
    }

    fun updateComponentStatus(component: String, status: String) {
        val current = _liveHealth.value.toMutableMap()
        current[component] = status
        _liveHealth.value = current
    }

    fun onBriefingReady(text: String?) {
        _briefingState.value = BriefingState.Ready(text)
    }

    fun clear() {
        _liveHealth.value = emptyMap()
        _briefingState.value = BriefingState.Idle
    }
    
    fun markCompleted() {
        _briefingState.value = BriefingState.Completed
    }
}

sealed class BriefingState {
    object Idle : BriefingState()
    data class Generating(val message: String?) : BriefingState()
    data class Ready(val text: String?) : BriefingState()
    object Completed : BriefingState()
}

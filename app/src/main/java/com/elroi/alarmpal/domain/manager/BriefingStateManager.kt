package com.elroi.alarmpal.domain.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object BriefingStateManager {
    private val _briefingState = MutableStateFlow<BriefingState>(BriefingState.Idle)
    val briefingState: StateFlow<BriefingState> = _briefingState

    fun startGenerating(message: String? = "Generating...") {
        _briefingState.value = BriefingState.Generating(message)
    }

    fun updateStatus(message: String) {
        _briefingState.value = BriefingState.Generating(message)
    }

    fun onBriefingReady(text: String?) {
        _briefingState.value = BriefingState.Ready(text)
    }

    fun clear() {
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

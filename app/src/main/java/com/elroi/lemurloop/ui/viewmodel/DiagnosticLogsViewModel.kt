package com.elroi.lemurloop.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elroi.lemurloop.domain.model.DiagnosticLog
import com.elroi.lemurloop.domain.repository.DiagnosticLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiagnosticLogsViewModel @Inject constructor(
    private val diagnosticLogRepository: DiagnosticLogRepository
) : ViewModel() {

    val logs: StateFlow<List<DiagnosticLog>> = diagnosticLogRepository.getLatestLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearLogs() {
        viewModelScope.launch {
            diagnosticLogRepository.clearAll()
        }
    }

    fun getLogsAsText(): String {
        return logs.value.joinToString("\n") { log ->
            "[${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(log.timestamp))}] ${log.tag}/${log.level}: ${log.message}"
        }
    }
}

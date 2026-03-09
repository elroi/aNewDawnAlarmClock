package com.elroi.lemurloop.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elroi.lemurloop.data.local.dao.DiagnosticLogDao
import com.elroi.lemurloop.data.local.entity.DiagnosticLogEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiagnosticLogsViewModel @Inject constructor(
    private val diagnosticLogDao: DiagnosticLogDao
) : ViewModel() {

    val logs: StateFlow<List<DiagnosticLogEntity>> = diagnosticLogDao.getLatestLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearLogs() {
        viewModelScope.launch {
            diagnosticLogDao.clearAll()
        }
    }

    fun getLogsAsText(): String {
        return logs.value.joinToString("\n") { log ->
            "[${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(log.timestamp))}] ${log.tag}/${log.level}: ${log.message}"
        }
    }
}

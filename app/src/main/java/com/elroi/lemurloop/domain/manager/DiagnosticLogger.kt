package com.elroi.lemurloop.domain.manager

import android.util.Log
import com.elroi.lemurloop.domain.repository.DiagnosticLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticLogger @Inject constructor(
    private val diagnosticLogRepository: DiagnosticLogRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun log(tag: String, message: String, level: String = "INFO") {
        // Always log to Logcat
        when (level) {
            "ERROR" -> Log.e(tag, message)
            "WARN" -> Log.w(tag, message)
            "DEBUG" -> Log.d(tag, message)
            else -> Log.i(tag, message)
        }

        // Persist to repository
        scope.launch {
            try {
                diagnosticLogRepository.appendLog(tag = tag, message = message, level = level)
            } catch (e: Exception) {
                Log.e("DiagnosticLogger", "Failed to persist log: ${e.message}")
            }
        }
    }

    fun error(tag: String, message: String) = log(tag, message, "ERROR")
    fun warn(tag: String, message: String) = log(tag, message, "WARN")
    fun debug(tag: String, message: String) = log(tag, message, "DEBUG")
}

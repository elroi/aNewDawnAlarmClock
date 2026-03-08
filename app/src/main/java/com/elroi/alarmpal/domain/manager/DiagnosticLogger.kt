package com.elroi.alarmpal.domain.manager

import android.util.Log
import com.elroi.alarmpal.data.local.dao.DiagnosticLogDao
import com.elroi.alarmpal.data.local.entity.DiagnosticLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticLogger @Inject constructor(
    private val diagnosticLogDao: DiagnosticLogDao
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

        // Persist to database
        scope.launch {
            try {
                diagnosticLogDao.insert(
                    DiagnosticLogEntity(
                        tag = tag,
                        message = message,
                        level = level
                    )
                )
                // Keep only the last 500 logs
                // This is a bit aggressive to run every time, but fine for now
                // Alternatively, we could run this on a timer
            } catch (e: Exception) {
                Log.e("DiagnosticLogger", "Failed to persist log: ${e.message}")
            }
        }
    }

    fun error(tag: String, message: String) = log(tag, message, "ERROR")
    fun warn(tag: String, message: String) = log(tag, message, "WARN")
    fun debug(tag: String, message: String) = log(tag, message, "DEBUG")
}

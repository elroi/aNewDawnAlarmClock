package com.elroi.alarmpal.domain.manager

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BriefingLogEntry(
    val timestamp: Long,
    val timeFormatted: String,
    val engineUsed: String,
    val isFallbackTriggered: Boolean,
    val result: String,
    val details: String,
    val briefingScript: String = ""
)

@Singleton
class BriefingLogger @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val logFile = File(context.filesDir, "briefing_logs.json")
    private val _logsFlow = MutableStateFlow<List<BriefingLogEntry>>(emptyList())
    val logsFlow: Flow<List<BriefingLogEntry>> = _logsFlow

    init {
        loadLogs()
    }

    private fun loadLogs() {
        if (!logFile.exists()) return
        try {
            val jsonText = logFile.readText()
            val jsonArray = JSONArray(jsonText)
            val logs = mutableListOf<BriefingLogEntry>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                logs.add(
                    BriefingLogEntry(
                        timestamp = obj.optLong("timestamp", 0L),
                        timeFormatted = obj.optString("timeFormatted", ""),
                        engineUsed = obj.optString("engineUsed", ""),
                        isFallbackTriggered = obj.optBoolean("isFallbackTriggered", false),
                        result = obj.optString("result", ""),
                        details = obj.optString("details", ""),
                        briefingScript = obj.optString("briefingScript", "")
                    )
                )
            }
            _logsFlow.value = logs.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun logBriefing(
        engineUsed: String,
        isFallbackTriggered: Boolean,
        result: String,
        details: String,
        briefingScript: String = ""
    ) {
        withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            val timeFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
            val newEntry = BriefingLogEntry(timestamp, timeFormatted, engineUsed, isFallbackTriggered, result, details, briefingScript)
            
            val currentLogs = _logsFlow.value.toMutableList()
            currentLogs.add(0, newEntry)
            
            val trimmedLogs = currentLogs.take(100)
            _logsFlow.value = trimmedLogs
            
            try {
                val jsonArray = JSONArray()
                for (log in trimmedLogs) {
                    val obj = JSONObject()
                    obj.put("timestamp", log.timestamp)
                    obj.put("timeFormatted", log.timeFormatted)
                    obj.put("engineUsed", log.engineUsed)
                    obj.put("isFallbackTriggered", log.isFallbackTriggered)
                    obj.put("result", log.result)
                    obj.put("details", log.details)
                    obj.put("briefingScript", log.briefingScript)
                    jsonArray.put(obj)
                }
                logFile.writeText(jsonArray.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    suspend fun clearLogs() {
        withContext(Dispatchers.IO) {
            if (logFile.exists()) {
                logFile.delete()
            }
            _logsFlow.value = emptyList()
        }
    }
}

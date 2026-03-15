package com.elroi.lemurloop.util

import android.content.Context
import android.util.Log

/**
 * Debug-session logging via Log.d.
 * No debugger needed: run app normally (Run or Debug), then capture logs:
 *   adb logcat -d -s LangDebug167
 *   or: adb logcat -d 2>&1 | grep LangDebug167 > .cursor/debug-167cd6.log
 */
// #region agent log
private fun escapeForJson(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

fun debugLog(ctx: Context?, hypothesisId: String, message: String, data: Map<String, String> = emptyMap()) {
    val dataStr = data.entries.joinToString(",") { """\"${escapeForJson(it.key)}\":\"${escapeForJson(it.value)}\"""" }
    val json = """{"sessionId":"167cd6","hypothesisId":"$hypothesisId","message":"${escapeForJson(message)}","data":{$dataStr},"timestamp":${System.currentTimeMillis()}}"""
    Log.d("LangDebug167", json)
}
// #endregion

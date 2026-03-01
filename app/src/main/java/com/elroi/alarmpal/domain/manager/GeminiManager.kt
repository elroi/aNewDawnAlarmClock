package com.elroi.alarmpal.domain.manager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiManager @Inject constructor(
    private val settingsManager: SettingsManager
) {
    suspend fun generateContent(prompt: String): String? {
        val apiKey = settingsManager.geminiApiKeyFlow.first().trim()
        if (apiKey.isBlank()) return null
        
        // 1. Check if we have a known-working config to save quota
        val cachedModel = settingsManager.workingGeminiModelFlow.first()
        val cachedVersion = settingsManager.workingGeminiVersionFlow.first()
        
        // Skip cache if it's a deprecated model that no longer works for new API keys
        val deprecatedModels = listOf("gemini-2.0-flash", "gemini-2.0-flash-001", "gemini-1.5-flash", "gemini-pro")
        val cacheIsValid = cachedModel != null && cachedVersion != null && cachedModel !in deprecatedModels
        
        if (cacheIsValid) {
            try {
                val response = generateWithKey(apiKey, prompt, cachedModel!!, cachedVersion!!)
                if (!response.isNullOrBlank()) return response
            } catch (e: Exception) {
                // If cached config fails, it might be outdated/expired/quota hit
                if (e.message?.contains("quota", ignoreCase = true) == true) {
                    return "ERROR: Quota Exhausted"
                }
                // Otherwise fall through to full negotiation
            }
        }

        // 2. Multi-stage negotiation (only if no cache or cache failed)
        val configs = listOf(
            "v1beta" to "gemini-2.5-flash",
            "v1beta" to "gemini-2.5-pro",
            "v1beta" to "gemini-2.5-flash-preview-04-17"
        )
        
        var lastError: String? = null
        
        for ((version, model) in configs) {
            android.util.Log.d("GeminiManager", "Trying $model ($version)...")
            try {
                val response = generateWithKey(apiKey, prompt, model, version)
                if (!response.isNullOrBlank()) {
                    android.util.Log.d("GeminiManager", "Success with $model")
                    settingsManager.saveWorkingGeminiConfig(model, version)
                    return response
                } else {
                    android.util.Log.w("GeminiManager", "$model returned blank response")
                    lastError = "$model returned blank"
                }
            } catch (e: Exception) {
                lastError = e.message
                android.util.Log.e("GeminiManager", "$model failed: ${e.message}")
                if (lastError?.contains("quota", ignoreCase = true) == true) break
            }
        }
        
        // All static configs failed — try dynamic discovery as last resort
        android.util.Log.d("GeminiManager", "Static models exhausted. Trying dynamic discovery...")
        val discoveredModels = getAvailableModels(apiKey)
        val textModels = discoveredModels.filter { m ->
            (m.contains("flash") || m.contains("pro")) &&
            !m.contains("image") && !m.contains("vision") && !m.contains("embed")
        }
        android.util.Log.d("GeminiManager", "Discovered usable models: $textModels")
        
        for (model in textModels) {
            android.util.Log.d("GeminiManager", "Trying discovered model: $model")
            try {
                val response = generateWithKey(apiKey, prompt, model, "v1beta")
                if (!response.isNullOrBlank()) {
                    android.util.Log.d("GeminiManager", "Success with discovered model: $model")
                    settingsManager.saveWorkingGeminiConfig(model, "v1beta")
                    return response
                }
            } catch (e: Exception) {
                lastError = e.message
                android.util.Log.e("GeminiManager", "Discovered model $model failed: ${e.message}")
            }
        }
        
        return "ERROR: ${lastError?.take(100)}"
    }

    suspend fun testApiKey(apiKey: String): String? {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) return "API Key cannot be empty."
        
        return try {
            val configs = listOf(
                "v1beta" to "gemini-2.5-flash",
                "v1beta" to "gemini-2.5-pro",
                "v1beta" to "gemini-2.5-flash-preview-04-17"
            )
            
            var latestError: String? = null
            
            for ((version, model) in configs) {
                try {
                    val response = generateWithKey(trimmedKey, "Say 'OK' if you are working.", model, version)
                    if (response != null) {
                        settingsManager.saveWorkingGeminiConfig(model, version)
                        return null // Success!
                    }
                } catch (e: Exception) {
                    latestError = e.message
                    if (latestError?.contains("quota", ignoreCase = true) == true) return latestError
                }
            }
            
            // If we're here, all tries failed. Get available models for debugging.
            val availableModels = getAvailableModels(trimmedKey)
            if (availableModels.isNotEmpty()) {
                "Key found, but models missing on v1/v1beta. Key sees: ${availableModels.take(5).joinToString(", ")}"
            } else {
                latestError ?: "Connection failed (404/400) for all discovered models."
            }
        } catch (e: Exception) {
            android.util.Log.e("GeminiManager", "REST Discovery failed", e)
            e.message ?: "An unknown error occurred during discovery."
        }
    }

    private suspend fun getAvailableModels(apiKey: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val models = json.getJSONArray("models")
                val names = mutableListOf<String>()
                for (i in 0 until models.length()) {
                    names.add(models.getJSONObject(i).getString("name").substringAfter("models/"))
                }
                names
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun generateWithKey(
        apiKey: String, 
        prompt: String, 
        modelName: String = "gemini-1.5-flash", 
        version: String = "v1beta"
    ): String? = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://generativelanguage.googleapis.com/$version/models/$modelName:generateContent?key=$apiKey"
            val url = URL(apiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val requestBody = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                // Add safety settings to be permissive for harmless briefings
                put("safetySettings", org.json.JSONArray().apply {
                    listOf("HARM_CATEGORY_HARASSMENT", "HARM_CATEGORY_HATE_SPEECH", 
                           "HARM_CATEGORY_SEXUALLY_EXPLICIT", "HARM_CATEGORY_DANGEROUS_CONTENT").forEach { category ->
                        put(JSONObject().apply {
                            put("category", category)
                            put("threshold", "BLOCK_NONE")
                        })
                    }
                })
            }

            conn.outputStream.use { it.write(requestBody.toString().toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                val candidates = jsonResponse.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    if (parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).getString("text")
                    }
                }
                null
            } else {
                val errorStream = conn.errorStream?.bufferedReader()?.use { it.readText() }
                val errorMessage = try {
                    val errorJson = JSONObject(errorStream ?: "")
                    errorJson.getJSONObject("error").getString("message")
                } catch (e: Exception) {
                    "Error $responseCode: ${conn.responseMessage}"
                }
                throw Exception(errorMessage)
            }
        } catch (e: Exception) {
            android.util.Log.e("GeminiManager", "Network error in generateWithKey", e)
            throw e
        }
    }
}

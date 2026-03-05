package com.elroi.alarmpal.domain.manager

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiManager @Inject constructor(
    private val settingsManager: SettingsManager
) {
    private suspend fun getWorkingModelConfig(apiKey: String): Pair<String, String>? {
        // 1. Check cache first
        val cachedModel = settingsManager.workingGeminiModelFlow.first()
        val cachedVersion = settingsManager.workingGeminiVersionFlow.first()
        
        if (cachedModel != null && cachedVersion != null) {
            try {
                val response = generateWithKey(apiKey, "OK", cachedModel, cachedVersion)
                if (response != null) return cachedModel to cachedVersion
            } catch (e: Exception) {
                android.util.Log.w("GeminiManager", "Cached config failed, renegotiating...")
            }
        }

        // 2. Prioritized static configs
        val configs = listOf(
            "v1beta" to "gemini-2.5-flash",
            "v1beta" to "gemini-2.5-pro",
            "v1beta" to "gemini-2.0-flash",
            "v1" to "gemini-1.5-flash",
            "v1beta" to "gemini-1.5-flash",
            "v1" to "gemini-1.5-pro"
        )
        
        for ((version, model) in configs) {
            try {
                val response = generateWithKey(apiKey, "OK", model, version)
                if (response != null) {
                    settingsManager.saveWorkingGeminiConfig(model, version)
                    return model to version
                }
            } catch (e: Exception) {
                if (e.message?.contains("quota", ignoreCase = true) == true) throw e
            }
        }
        
        // 3. Dynamic discovery
        val discoveredModels = getAvailableModels(apiKey)
        val textModels = discoveredModels.filter { m ->
            (m.contains("flash") || m.contains("pro")) &&
            !m.contains("image") && !m.contains("vision") && !m.contains("embed")
        }
        
        for (model in textModels) {
            for (version in listOf("v1beta", "v1")) {
                try {
                    val response = generateWithKey(apiKey, "OK", model, version)
                    if (response != null) {
                        settingsManager.saveWorkingGeminiConfig(model, version)
                        return model to version
                    }
                } catch (e: Exception) {}
            }
        }
        
        return null
    }

    fun generateContentStreaming(prompt: String): Flow<String> = flow {
        val apiKey = settingsManager.geminiApiKeyFlow.first().trim()
        if (apiKey.isBlank()) return@flow
        
        val config = getWorkingModelConfig(apiKey) ?: return@flow
        
        val safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.ONLY_HIGH),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH)
        )

        val model = GenerativeModel(
            modelName = config.first,
            apiKey = apiKey,
            safetySettings = safetySettings
        )

        try {
            var fullText = ""
            model.generateContentStream(prompt).collect { chunk ->
                val text = chunk.text ?: ""
                fullText += text
                emit(fullText)
            }
        } catch (e: Exception) {
            android.util.Log.e("GeminiManager", "Streaming error with ${config.first}", e)
            throw e
        }
    }

    suspend fun generateContent(prompt: String): String? {
        val apiKey = settingsManager.geminiApiKeyFlow.first().trim()
        if (apiKey.isBlank()) return null
        
        return try {
            val config = getWorkingModelConfig(apiKey) ?: return "ERROR: No compatible model found"
            generateWithKey(apiKey, prompt, config.first, config.second)
        } catch (e: Exception) {
            val msg = e.message ?: "Unknown error"
            if (msg.contains("quota", ignoreCase = true)) "ERROR: Quota Exhausted"
            else "ERROR: $msg"
        }
    }

    suspend fun testApiKey(apiKey: String): String? {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) return "API Key cannot be empty."
        
        return try {
            val config = getWorkingModelConfig(trimmedKey)
            if (config != null) null else "No compatible models found for this key."
        } catch (e: Exception) {
            android.util.Log.e("GeminiManager", "Test failed", e)
            e.message ?: "An unknown error occurred."
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

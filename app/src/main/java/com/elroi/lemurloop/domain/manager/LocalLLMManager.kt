package com.elroi.lemurloop.domain.manager

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class GeminiNanoStatus {
    CHECKING,
    SUPPORTED,
    NOT_SUPPORTED,
    DOWNLOAD_REQUIRED,
    DOWNLOADING
}

@Singleton
class LocalLLMManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelDownloader: ModelDownloader
) {
    private var model: LlmInference? = null

    /**
     * Lightweight check — only tests if the model file exists on disk.
     * Does NOT load the native model. Safe to call from any context.
     */
    fun isModelDownloaded(): Boolean = modelDownloader.modelFile.exists()


    suspend fun checkStatus(): GeminiNanoStatus = withContext(Dispatchers.IO) {
        if (!modelDownloader.modelFile.exists()) {
            return@withContext GeminiNanoStatus.DOWNLOAD_REQUIRED
        }
        
        // Try to load the model to see if it's supported by the hardware
        try {
            if (model == null) {
                initializeModel()
            }
            if (model != null) {
                GeminiNanoStatus.SUPPORTED
            } else {
                GeminiNanoStatus.NOT_SUPPORTED
            }
        } catch (e: Exception) {
            e.printStackTrace()
            GeminiNanoStatus.NOT_SUPPORTED
        }
    }

    /**
     * Initializes the MediaPipe model from the downloaded .bin file.
     */
    private fun initializeModel() {
        if (model != null) return
        if (!modelDownloader.modelFile.exists()) return
        
        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelDownloader.modelFile.absolutePath)
                .build()
            model = LlmInference.createFromOptions(context, options)
        } catch (e: Exception) {
            android.util.Log.e("LocalLLMManager", "Error initializing MediaPipe model", e)
            model = null
        }
    }

    /**
     * Generates a briefing using the on-device MediaPipe model.
     */
    suspend fun generateContent(prompt: String): String? = withContext(Dispatchers.IO) {
        try {
            if (model == null) initializeModel()
            if (model == null) {
                return@withContext "ERROR: Local LLM failed to initialize. Check Android logcat for MediaPipe errors."
            }
            val response = model?.generateResponse(prompt)
            android.util.Log.d("LocalLLMManager", "Gemma 2B raw output: '$response'")
            response
        } catch (e: Exception) {
            android.util.Log.e("LocalLLMManager", "Error generating content", e)
            "ERROR: Local LLM fail - ${e.message}"
        }
    }

    /**
     * Downloads the on-device model if required via the custom downloader.
     */
    suspend fun downloadModel(onProgress: (Int) -> Unit = {}): Boolean {
        return modelDownloader.downloadModel(onProgress)
    }
}

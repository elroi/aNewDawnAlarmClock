package com.elroi.lemurloop.data.local

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import com.elroi.lemurloop.domain.manager.CloudPersonaVoiceConfig
import com.elroi.lemurloop.domain.manager.SettingsManager
import com.elroi.lemurloop.domain.manager.getCloudPersonaVoiceConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleCloudTtsEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager
) {

    companion object {
        private const val TAG = "GoogleCloudTtsEngine"
        private const val ENDPOINT =
            "https://texttospeech.googleapis.com/v1/text:synthesize"
    }

    /**
     * Pre-computes audio for the given text and persona, returning a temp file that can be
     * played later via MediaPlayer. Returns null if the feature is disabled, the key is missing,
     * or any network/IO error occurs.
     */
    suspend fun synthesizeToFile(
        text: String,
        personaId: String?,
        uiLanguage: String
    ): File? = withContext(Dispatchers.IO) {
        val apiKey = settingsManager.cloudTtsApiKeyFlow.firstOrNullSafe()
        val isEnabled = settingsManager.isCloudTtsEnabledFlow.firstOrNullSafe() ?: false

        if (!isEnabled || apiKey.isNullOrBlank()) {
            Log.d(TAG, "Cloud TTS disabled or API key missing; skipping synthesis.")
            return@withContext null
        }

        synthesizeToFileInternal(
            text = text,
            personaId = personaId,
            uiLanguage = uiLanguage,
            apiKey = apiKey
        )
    }

    /**
     * Used by settings to validate a user-supplied key even when Cloud TTS is not yet enabled.
     * Returns true if Google returns playable audio using the provided key.
     */
    suspend fun testKey(
        text: String,
        personaId: String?,
        uiLanguage: String,
        apiKey: String
    ): Boolean = withContext(Dispatchers.IO) {
        val file = synthesizeToFileInternal(
            text = text,
            personaId = personaId,
            uiLanguage = uiLanguage,
            apiKey = apiKey
        )

        if (file != null && file.exists()) {
            Log.d(TAG, "Cloud TTS testKey succeeded; deleting temp file.")
            file.delete()
            true
        } else {
            false
        }
    }

    private fun synthesizeToFileInternal(
        text: String,
        personaId: String?,
        uiLanguage: String,
        apiKey: String
    ): File? {
        return try {
            val config: CloudPersonaVoiceConfig =
                getCloudPersonaVoiceConfig(personaId ?: "COACH", uiLanguage)

            val languageCode = when (uiLanguage.lowercase(Locale.getDefault())) {
                "en" -> "en-US"
                else -> "en-US"
            }

            val requestJson = JSONObject().apply {
                put("input", JSONObject().apply {
                    put("text", text)
                })
                put("voice", JSONObject().apply {
                    put("languageCode", languageCode)
                    put("name", config.voiceName)
                })
                put("audioConfig", JSONObject().apply {
                    put("audioEncoding", "MP3")
                    put("speakingRate", config.speakingRate)
                    put("pitch", config.pitch)
                })
            }

            val url = URL("$ENDPOINT?key=$apiKey")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 10_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }

            connection.outputStream.use { os ->
                os.write(requestJson.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = try {
                    connection.errorStream?.bufferedReader()?.readText()
                } catch (e: Exception) {
                    null
                }
                Log.w(TAG, "Cloud TTS HTTP $responseCode: $errorBody")
                return null
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val audioContent = json.optString("audioContent", "")
            if (audioContent.isBlank()) {
                Log.w(TAG, "Cloud TTS response missing audioContent")
                return null
            }

            val audioBytes = Base64.decode(audioContent, Base64.DEFAULT)
            val file = File.createTempFile("cloud_tts_", ".mp3", context.cacheDir)
            file.outputStream().use { it.write(audioBytes) }

            Log.d(TAG, "Cloud TTS synthesized audio to ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.w(TAG, "Cloud TTS synthesis failed: ${e.message}", e)
            null
        }
    }

    /**
     * Convenience helper for settings/test flows: synthesizes audio and plays it once via
     * MediaPlayer, then cleans up the temp file.
     */
    suspend fun speakOnce(
        text: String,
        personaId: String?,
        uiLanguage: String
    ) {
        val file = synthesizeToFile(text, personaId, uiLanguage) ?: return
        withContext(Dispatchers.Main) {
            try {
                val player = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    setOnCompletionListener {
                        it.release()
                        file.delete()
                    }
                    setOnErrorListener { mp, _, _ ->
                        mp.release()
                        file.delete()
                        true
                    }
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to play synthesized audio: ${e.message}", e)
                file.delete()
            }
        }
    }
}

private suspend fun <T> Flow<T>.firstOrNullSafe(): T? {
    return try {
        first()
    } catch (e: Exception) {
        null
    }
}


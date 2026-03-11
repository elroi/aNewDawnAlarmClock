package com.elroi.lemurloop.domain.manager

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

interface TtsEngine {
    val onSpeechCompleted: SharedFlow<Unit>
    fun initializeIfNeeded()
    suspend fun speak(text: String, personaId: String? = null)
    fun stop()
    fun shutdown()
}

@Singleton
class TextToSpeechManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager
) : TextToSpeech.OnInitListener, TtsEngine {

    private var tts: TextToSpeech? = null
    private var availableVoices: Set<android.speech.tts.Voice>? = null
    
    // Use an atomic state/flow to wait for initialization
    private val isInitialized = MutableStateFlow(false)
    
    private val _onSpeechCompleted = MutableSharedFlow<Unit>()
    override val onSpeechCompleted: SharedFlow<Unit> = _onSpeechCompleted.asSharedFlow()

    init {
        initializeIfNeeded()
    }

    override fun initializeIfNeeded() {
        if (tts == null) {
            Log.d("TTS_DEBUG", "Initializing new TTS instance")
            isInitialized.value = false
            tts = TextToSpeech(context, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            Log.d("TTS_DEBUG", "TextToSpeech.SUCCESS. Setting language...")
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS_DEBUG", "The Language specified is not supported! result=$result")
            } else {
                Log.d("TTS_DEBUG", "TTS Language set successfully. isInitialized = true")
                try {
                    availableVoices = tts?.voices
                    Log.d("TTS_DEBUG", "Loaded ${availableVoices?.size ?: 0} TTS voices")
                } catch (e: Exception) {
                    Log.w("TTS_DEBUG", "Failed to read available voices: ${e.message}")
                }
                
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d("TTS_DEBUG", "TTS started speaking")
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.d("TTS_DEBUG", "TTS finished speaking")
                        // Fire the completion event
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                            _onSpeechCompleted.emit(Unit)
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        Log.e("TTS_DEBUG", "TTS encountered an error")
                        // Also fire completion on error to not block the UI forever
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                            _onSpeechCompleted.emit(Unit)
                        }
                    }
                })
                
                isInitialized.value = true
            }
        } else {
            Log.e("TTS_DEBUG", "Initialization Failed! status=$status")
        }
    }

    override suspend fun speak(text: String, personaId: String?) {
        initializeIfNeeded()
        Log.d("TTS_DEBUG", "speak() called with text: $text")
        Log.d("TTS_DEBUG", "Waiting for isInitialized to be true... currently=${isInitialized.value}")
        // Wait until TTS engine signals it's ready
        isInitialized.first { it }
        Log.d("TTS_DEBUG", "isInitialized is true! Calling tts?.speak()")

        applyPersonaVoiceConfig(personaId)
        
        // Strip emojis using regex so the TTS doesn't read them out loud
        // This covers most standard emoji blocks
        val cleanText = text.replace(Regex("""[\x{1F600}-\x{1F64F}\x{1F300}-\x{1F5FF}\x{1F680}-\x{1F6FF}\x{1F700}-\x{1F77F}\x{1F780}-\x{1F7FF}\x{1F800}-\x{1F8FF}\x{1F900}-\x{1F9FF}\x{1FA00}-\x{1FA6F}\x{1FA70}-\x{1FAFF}\x{2600}-\x{26FF}\x{2700}-\x{27BF}\x{2300}-\x{23FF}]"""), "")
        
        // Pass a unique utteranceId so that the progress listener tracks it
        val params = android.os.Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "briefing_utterance")
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, "briefing_utterance")
        } else {
            val hashmap = java.util.HashMap<String, String>()
            hashmap[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "briefing_utterance"
            @Suppress("DEPRECATION")
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, hashmap)
        }
    }

    override fun stop() {
        Log.d("TTS_DEBUG", "stop() called")
        tts?.stop()
    }

    override fun shutdown() {
        Log.d("TTS_DEBUG", "shutdown() called")
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized.value = false
    }

    private suspend fun applyPersonaVoiceConfig(explicitPersonaId: String?) {
        val personaId = explicitPersonaId ?: try {
            settingsManager.alarmDefaultsFlow.first().aiPersona
        } catch (e: Exception) {
            Log.w("TTS_DEBUG", "Failed to read aiPersona from settings: ${e.message}")
            null
        }

        val config = PersonaVoiceConfig.forPersona(personaId)

        try {
            val ttsInstance = tts
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP && ttsInstance != null) {
                val voices = availableVoices ?: ttsInstance.voices
                val bestVoice = selectBestVoice(voices, config)
                if (bestVoice != null) {
                    ttsInstance.voice = bestVoice
                    Log.d("TTS_DEBUG", "Applied persona voice: ${bestVoice.name} for persona=$personaId")
                } else {
                    ttsInstance.language = Locale.US
                    Log.d("TTS_DEBUG", "No matching persona voice, falling back to Locale.US for persona=$personaId")
                }
            } else {
                ttsInstance?.language = Locale.US
            }
        } catch (e: Exception) {
            Log.w("TTS_DEBUG", "Failed to apply persona voice config: ${e.message}")
        }

        try {
            val ttsInstance = tts
            ttsInstance?.setPitch(config.pitch)
            ttsInstance?.setSpeechRate(config.speechRate)
            Log.d("TTS_DEBUG", "Applied persona pitch=${config.pitch}, rate=${config.speechRate} for persona=$personaId")
        } catch (e: Exception) {
            Log.w("TTS_DEBUG", "Failed to apply pitch/speechRate: ${e.message}")
        }
    }

    private fun selectBestVoice(
        voices: Set<android.speech.tts.Voice>?,
        config: PersonaVoiceConfig
    ): android.speech.tts.Voice? {
        if (voices.isNullOrEmpty()) return null

        val filtered = voices.filter { voice ->
            val isEnglish = voice.locale.language == Locale.ENGLISH.language
            val isOffline = !voice.isNetworkConnectionRequired
            isEnglish && isOffline
        }

        val candidates = if (filtered.isNotEmpty()) filtered else voices.toList()

        val pattern = config.preferredVoiceNamePattern?.lowercase()
        val withPattern = pattern?.let { p ->
            candidates.filter { voice -> voice.name.lowercase().contains(p) }
        } ?: emptyList()

        val preferredList = if (withPattern.isNotEmpty()) withPattern else candidates

        return preferredList.minByOrNull { voice ->
            val qualityScore = when (voice.quality) {
                android.speech.tts.Voice.QUALITY_VERY_HIGH -> 0
                android.speech.tts.Voice.QUALITY_HIGH -> 1
                android.speech.tts.Voice.QUALITY_NORMAL -> 2
                else -> 3
            }
            val latencyScore = when (voice.latency) {
                android.speech.tts.Voice.LATENCY_VERY_LOW -> 0
                android.speech.tts.Voice.LATENCY_LOW -> 1
                android.speech.tts.Voice.LATENCY_NORMAL -> 2
                else -> 3
            }
            qualityScore * 10 + latencyScore
        }
    }
}

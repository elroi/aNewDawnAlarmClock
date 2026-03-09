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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Singleton
class TextToSpeechManager @Inject constructor(
    @ApplicationContext private val context: Context
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    
    // Use an atomic state/flow to wait for initialization
    private val isInitialized = MutableStateFlow(false)
    
    private val _onSpeechCompleted = MutableSharedFlow<Unit>()
    val onSpeechCompleted = _onSpeechCompleted.asSharedFlow()

    init {
        initializeIfNeeded()
    }

    fun initializeIfNeeded() {
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

    suspend fun speak(text: String) {
        initializeIfNeeded()
        Log.d("TTS_DEBUG", "speak() called with text: $text")
        Log.d("TTS_DEBUG", "Waiting for isInitialized to be true... currently=${isInitialized.value}")
        // Wait until TTS engine signals it's ready
        isInitialized.first { it }
        Log.d("TTS_DEBUG", "isInitialized is true! Calling tts?.speak()")
        
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

    fun stop() {
        Log.d("TTS_DEBUG", "stop() called")
        tts?.stop()
    }

    fun shutdown() {
        Log.d("TTS_DEBUG", "shutdown() called")
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized.value = false
    }
}

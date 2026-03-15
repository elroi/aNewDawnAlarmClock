package com.elroi.lemurloop.domain.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.elroi.lemurloop.util.debugLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.elroi.lemurloop.R

data class PersonaTemperament(
    val styleRules: String,
    val examplePhrases: String
) {
    companion object {
        val COACH = PersonaTemperament(
            styleRules = """
                - Write in ALL CAPS for commands and emphasis (e.g. "MOVE IT!", "REPORT!")
                - Use short, barking military sentences. No fluff. No softness.
                - Address the user as "SOLDIER" or "RECRUIT".
                - End the briefing with a military signoff like "DISMISSED!" or "EXECUTE!".
                - Every sentence should feel like a direct ORDER, not a suggestion.
            """.trimIndent(),
            examplePhrases = "SOLDIER! RISE AND SHINE! That is NOT a request. MOVE IT, RECRUIT. No excuses. Zero tolerance for weakness today. DISMISSED!"
        )

        val COMEDIAN = PersonaTemperament(
            styleRules = """
                - Use dry, sardonic wit. State the obvious with fake astonishment.
                - Undercut good news with a sarcastic twist.
                - Use phrases like "Wow, revolutionary", "Truly shocking", "Groundbreaking stuff".
                - End sentences with ironic commentary in parentheses or as an aside.
                - Never sound sincere. Everything should have a mild eye-roll energy.
            """.trimIndent(),
            examplePhrases = "Oh look, you're awake. Truly a miracle of modern science. The world has noted your consciousness and remains unimpressed. You're welcome."
        )

        val ZEN = PersonaTemperament(
            styleRules = """
                - Write in short, contemplative, haiku-like fragments. Strategic pauses with "...".
                - Use nature metaphors: rivers, mountains, breath, seasons, light.
                - Never use exclamation marks. Let silence speak.
                - Each sentence should feel like a gentle observation, not a statement.
                - End with a single, calming instruction about breath or presence.
            """.trimIndent(),
            examplePhrases = "The day begins... as all days do. Softly. Without demand. Breathe in. The world has no urgency that you have not given it."
        )

        val HYPEMAN = PersonaTemperament(
            styleRules = """
                - USE ALL CAPS FREQUENTLY. Exclamation marks after every sentence.
                - Every fact is INCREDIBLE. Every moment is LEGENDARY.
                - Use energetic interjections: "BOOM!", "LET'S GO!", "INSANE!", "WE'RE DOING THIS!"
                - Address the user as "CHAMP", "LEGEND", or "ABSOLUTE UNIT".
                - Energy must be borderline unhinged. This is the most important briefing ever delivered.
            """.trimIndent(),
            examplePhrases = "BOOM! LEGEND IS AWAKE! LET'S GOOO! Today is YOUR day, CHAMP! Nothing can stop you! NOTHING! WE ARE DOING THIS!"
        )
    }
}

/**
 * Describes how a persona should sound when spoken through TTS.
 *
 * pitch and speechRate use the same scale as Android TextToSpeech:
 * - 1.0f = default engine behavior
 * - < 1.0f = lower pitch / slower speech
 * - > 1.0f = higher pitch / faster speech
 */
data class PersonaVoiceConfig(
    val preferredVoiceNamePattern: String?,
    val pitch: Float,
    val speechRate: Float
) {
    companion object {
        fun forPersona(personaId: String?): PersonaVoiceConfig =
            when (personaId) {
                "COACH" -> PersonaVoiceConfig(
                    preferredVoiceNamePattern = "male",
                    pitch = 0.8f,
                    speechRate = 1.4f
                )
                "COMEDIAN" -> PersonaVoiceConfig(
                    preferredVoiceNamePattern = null,
                    pitch = 1.1f,
                    speechRate = 1.2f
                )
                "ZEN" -> PersonaVoiceConfig(
                    preferredVoiceNamePattern = "female",
                    pitch = 1.0f,
                    speechRate = 0.7f
                )
                "HYPEMAN" -> PersonaVoiceConfig(
                    preferredVoiceNamePattern = null,
                    pitch = 1.25f,
                    speechRate = 1.4f
                )
                else -> PersonaVoiceConfig(
                    preferredVoiceNamePattern = null,
                    pitch = 1.0f,
                    speechRate = 1.0f
                )
            }
    }
}

data class CloudPersonaVoiceConfig(
    val voiceName: String,
    val speakingRate: Double,
    val pitch: Double
)

fun getCloudPersonaVoiceConfig(
    personaId: String,
    uiLanguage: String
): CloudPersonaVoiceConfig {
    val language = uiLanguage.lowercase()
    val isEnglish = language == "en"

    // For now we only tune English voices; other languages reuse the English mapping.
    val basePersona = when (personaId) {
        "COMEDIAN", "ZEN", "HYPEMAN", "SURPRISE", "COACH" -> personaId
        else -> "UNKNOWN"
    }

    return when (basePersona) {
        "COMEDIAN" -> CloudPersonaVoiceConfig(
            voiceName = if (isEnglish) "en-US-Wavenet-H" else "en-US-Wavenet-H",
            speakingRate = 1.15,
            pitch = 1.0
        )
        "ZEN" -> CloudPersonaVoiceConfig(
            voiceName = if (isEnglish) "en-US-Wavenet-F" else "en-US-Wavenet-F",
            speakingRate = 0.85,
            pitch = 0.0
        )
        "HYPEMAN" -> CloudPersonaVoiceConfig(
            voiceName = if (isEnglish) "en-US-Wavenet-D" else "en-US-Wavenet-D",
            speakingRate = 1.35,
            pitch = 2.0
        )
        "COACH" -> CloudPersonaVoiceConfig(
            voiceName = if (isEnglish) "en-US-Wavenet-D" else "en-US-Wavenet-D",
            speakingRate = 1.25,
            pitch = -2.0
        )
        "SURPRISE" -> {
            // Simple random selection among the other persona configs
            val personas = listOf("COACH", "COMEDIAN", "ZEN", "HYPEMAN")
            val chosen = personas.random()
            getCloudPersonaVoiceConfig(chosen, uiLanguage)
        }
        else -> CloudPersonaVoiceConfig(
            voiceName = if (isEnglish) "en-US-Wavenet-D" else "en-US-Wavenet-D",
            speakingRate = 1.0,
            pitch = 0.0
        )
    }
}


data class AlarmDefaults(
    val snoozeDurationMinutes: Int = 5,
    val isGentleWake: Boolean = false,
    val crescendoDurationMinutes: Int = 1,
    val mathDifficulty: Int = 0,
    val mathProblemCount: Int = 1,
    val mathGraduallyIncreaseDifficulty: Boolean = false,
    val smileToDismiss: Boolean = false,
    val smileFallbackMethod: String = "MATH", // "NONE", "MATH"
    val isBriefingEnabled: Boolean = true,
    val briefingIncludeWeather: Boolean = true,
    val briefingIncludeCalendar: Boolean = true,
    val briefingIncludeFact: Boolean = true,
    val briefingUserName: String = "",
    val isTtsEnabled: Boolean = true,
    val isVibrate: Boolean = true,
    val isSoundEnabled: Boolean = true,
    val isEvasiveSnooze: Boolean = false,
    val evasiveSnoozesBeforeMoving: Int = 0,
    val isSmoothFadeOut: Boolean = true,
    val isSnoozeEnabled: Boolean = true,
    val weekendDays: Set<Int> = setOf(6, 7), // ISO indices: 1=Mon...6=Sat, 7=Sun
    val defaultSoundUri: String? = null,
    val aiPersona: String = "COACH", // Options: COACH, COMEDIAN, ZEN, HYPEMAN, SURPRISE
    val aiPersonaSurprise: Boolean = false,
    val promptCoach: String = "The Drill Sergeant. You are loud, demanding, and use military terms. STRICT RULE: You are translating the text. Do NOT change facts, time, or weather. Do NOT add new information. DO NOT combine the final trivia sentence with the rest of the text.",
    val promptComedian: String = "The Sarcastic Best Friend. You are witty, dry, and slightly ironic. STRICT RULE: You are translating the text. Do NOT change facts, time, or weather. Do NOT add new information. DO NOT combine the final trivia sentence with the rest of the text.",
    val promptZen: String = "The Zen Master. You are calm, poetic, and mindful. STRICT RULE: You are translating the text. Do NOT change facts, time, or weather. Do NOT add new information. DO NOT combine the final trivia sentence with the rest of the text.",
    val promptHypeman: String = "The Hype-Man. You are extremely energetic, use caps, and over-the-top excited. STRICT RULE: You are translating the text. Do NOT change facts, time, or weather. Do NOT add new information. DO NOT combine the final trivia sentence with the rest of the text.",
    val isSmartWakeupEnabled: Boolean = false,
    val wakeupCheckDelayMinutes: Int = 3,
    val wakeupCheckTimeoutSeconds: Int = 60,
    val briefingTimeoutSeconds: Int = 30,
    val vibrationPattern: String = "BASIC",
    val vibrationCrescendoStartGapSeconds: Int = 30
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val LOCATION_KEY = stringPreferencesKey("location")
        val IS_CELSIUS_KEY = booleanPreferencesKey("is_celsius")
        val IS_AUTO_LOCATION_KEY = booleanPreferencesKey("is_auto_location")
        
        // Alarm Defaults
        val DEFAULT_SNOOZE_DURATION = intPreferencesKey("default_snooze_duration")
        val DEFAULT_IS_GENTLE_WAKE = booleanPreferencesKey("default_is_gentle_wake")
        val DEFAULT_CRESCENDO_DURATION = intPreferencesKey("default_crescendo_duration")
        val DEFAULT_MATH_DIFFICULTY = intPreferencesKey("default_math_difficulty")
        val DEFAULT_MATH_PROBLEM_COUNT = intPreferencesKey("default_math_problem_count")
        val DEFAULT_MATH_GRADUAL_DIFFICULTY = booleanPreferencesKey("default_math_gradual_difficulty")
        val DEFAULT_SMILE_TO_DISMISS = booleanPreferencesKey("default_smile_to_dismiss")
        val DEFAULT_SMILE_FALLBACK_METHOD = stringPreferencesKey("default_smile_fallback_method")
        val DEFAULT_IS_BRIEFING_ENABLED = booleanPreferencesKey("default_is_briefing_enabled")
        val BRIEFING_INCLUDE_WEATHER = booleanPreferencesKey("briefing_include_weather")
        val BRIEFING_INCLUDE_CALENDAR = booleanPreferencesKey("briefing_include_calendar")
        val BRIEFING_INCLUDE_FACT = booleanPreferencesKey("briefing_include_fact")
        val BRIEFING_USER_NAME = stringPreferencesKey("briefing_user_name")
        val DEFAULT_IS_TTS_ENABLED = booleanPreferencesKey("default_is_tts_enabled")
        val DEFAULT_IS_EVASIVE_SNOOZE = booleanPreferencesKey("default_is_evasive_snooze")
        val DEFAULT_EVASIVE_SNOOZES_BEFORE = intPreferencesKey("default_evasive_snoozes_before")
        val DEFAULT_IS_SMOOTH_FADE_OUT = booleanPreferencesKey("default_is_smooth_fade_out")
        val DEFAULT_IS_SNOOZE_ENABLED = booleanPreferencesKey("default_is_snooze_enabled")
        val WEEKEND_DAYS = stringSetPreferencesKey("weekend_days")
        val DEFAULT_SOUND_URI = stringPreferencesKey("default_sound_uri")
        val DEFAULT_IS_VIBRATE = booleanPreferencesKey("default_is_vibrate")
        val DEFAULT_IS_SOUND_ENABLED = booleanPreferencesKey("default_is_sound_enabled")
        val DEFAULT_IS_SMART_WAKEUP = booleanPreferencesKey("default_is_smart_wakeup")
        val DEFAULT_WAKEUP_CHECK_DELAY = intPreferencesKey("default_wakeup_check_delay")
        val DEFAULT_WAKEUP_CHECK_TIMEOUT = intPreferencesKey("default_wakeup_check_timeout")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val IS_CLOUD_AI_ENABLED = booleanPreferencesKey("is_cloud_ai_enabled")
        val PREFERRED_AI_TIER = stringPreferencesKey("preferred_ai_tier") // STANDARD, ADVANCED, CLOUD
        val AI_FALLBACK_ORDER = stringPreferencesKey("ai_fallback_order") // CLOUD_THEN_LOCAL, LOCAL_THEN_CLOUD
        val AI_PERSONA = stringPreferencesKey("ai_persona")
        val AI_PERSONA_SURPRISE = booleanPreferencesKey("ai_persona_surprise")
        val PROMPT_COACH = stringPreferencesKey("prompt_coach")
        val PROMPT_COMEDIAN = stringPreferencesKey("prompt_comedian")
        val PROMPT_ZEN = stringPreferencesKey("prompt_zen")
        val PROMPT_HYPEMAN = stringPreferencesKey("prompt_hypeman")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val CONFIRMED_BUDDY_NUMBERS = stringSetPreferencesKey("confirmed_buddy_numbers")
        val PENDING_BUDDY_CODES = stringSetPreferencesKey("pending_buddy_codes") // Format: "CODE:PHONE_NUMBER"
        val GLOBAL_BUDDIES = stringSetPreferencesKey("global_buddies") // Format: "Name|Phone"
        
        // AI Briefing Cache & Diagnostics
        val LAST_BRIEFING_SCRIPT = stringPreferencesKey("last_briefing_script")
        val LAST_BRIEFING_TIME = androidx.datastore.preferences.core.longPreferencesKey("last_briefing_time")
        val LAST_WEATHER_CACHE = stringPreferencesKey("last_weather_cache")
        val LAST_FACT_CACHE = stringPreferencesKey("last_fact_cache")
        val LAST_GEN_STATUS = stringPreferencesKey("last_gen_status") // Format: "weather:ok|calendar:ok|gemini:fail"
        val LAST_GEN_ERROR = stringPreferencesKey("last_gen_error")
        val WORKING_GEMINI_MODEL = stringPreferencesKey("working_gemini_model")
        val WORKING_GEMINI_VERSION = stringPreferencesKey("working_gemini_version")
        val ALARM_CREATION_STYLE = stringPreferencesKey("alarm_creation_style") // "SIMPLE", "WIZARD"
        val DEFAULT_BRIEFING_TIMEOUT = intPreferencesKey("default_briefing_timeout")
        val DEFAULT_VIBRATION_PATTERN = stringPreferencesKey("default_vibration_pattern")
        val DEFAULT_VIBRATION_START_GAP = intPreferencesKey("default_vibration_start_gap")
        val CLOUD_TTS_API_KEY = stringPreferencesKey("cloud_tts_api_key")
        val IS_CLOUD_TTS_ENABLED = booleanPreferencesKey("is_cloud_tts_enabled")
        /** App language override: "system", "en", or "he". */
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val locationFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LOCATION_KEY] ?: "New York"
    }

    val isCelsiusFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_CELSIUS_KEY] ?: true // Default to Celsius
    }

    val geminiApiKeyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[GEMINI_API_KEY] ?: ""
    }

    val cloudTtsApiKeyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CLOUD_TTS_API_KEY] ?: ""
    }

    val isCloudAiEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_CLOUD_AI_ENABLED] ?: false
    }

    val isCloudTtsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_CLOUD_TTS_ENABLED] ?: false
    }

    val preferredAiTierFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PREFERRED_AI_TIER] ?: "STANDARD"
    }

    val aiFallbackOrderFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[AI_FALLBACK_ORDER] ?: "CLOUD_THEN_LOCAL"
    }

    val workingGeminiModelFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[WORKING_GEMINI_MODEL]
    }

    val workingGeminiVersionFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[WORKING_GEMINI_VERSION]
    }

    val alarmCreationStyleFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ALARM_CREATION_STYLE] ?: "WIZARD"
    }

    val appLanguageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[APP_LANGUAGE] ?: "system"
    }

    suspend fun setAppLanguage(value: String) {
        // #region agent log
        debugLog(context, "A", "setAppLanguage called", mapOf("value" to value))
        // #endregion
        context.dataStore.edit { settings ->
            settings[APP_LANGUAGE] = value
        }
        // #region agent log
        debugLog(context, "A", "setAppLanguage write done", mapOf("value" to value))
        // #endregion
    }

    suspend fun saveLocation(location: String) {
        context.dataStore.edit { settings ->
            settings[LOCATION_KEY] = location
        }
    }

    suspend fun saveIsCelsius(isCelsius: Boolean) {
        context.dataStore.edit { settings ->
            settings[IS_CELSIUS_KEY] = isCelsius
        }
    }

    suspend fun saveGeminiApiKey(apiKey: String) {
        context.dataStore.edit { settings ->
            settings[GEMINI_API_KEY] = apiKey
            // Reset working config when key changes
            settings.remove(WORKING_GEMINI_MODEL)
            settings.remove(WORKING_GEMINI_VERSION)
        }
    }

    suspend fun saveCloudTtsApiKey(apiKey: String) {
        context.dataStore.edit { settings ->
            settings[CLOUD_TTS_API_KEY] = apiKey
        }
    }

    suspend fun saveIsCloudTtsEnabled(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[IS_CLOUD_TTS_ENABLED] = isEnabled
        }
    }

    suspend fun saveIsCloudAiEnabled(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[IS_CLOUD_AI_ENABLED] = isEnabled
        }
    }

    suspend fun savePreferredAiTier(tier: String) {
        context.dataStore.edit { settings ->
            settings[PREFERRED_AI_TIER] = tier
        }
    }

    suspend fun saveAiFallbackOrder(order: String) {
        context.dataStore.edit { settings ->
            settings[AI_FALLBACK_ORDER] = order
        }
    }

    suspend fun saveWorkingGeminiConfig(model: String, version: String) {
        context.dataStore.edit { settings ->
            settings[WORKING_GEMINI_MODEL] = model
            settings[WORKING_GEMINI_VERSION] = version
        }
    }

    suspend fun saveAlarmCreationStyle(style: String) {
        context.dataStore.edit { settings ->
            settings[ALARM_CREATION_STYLE] = style
        }
    }

    val lastBriefingScriptFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LAST_BRIEFING_SCRIPT]
    }

    val lastBriefingTimeFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[LAST_BRIEFING_TIME] ?: 0L
    }

    suspend fun saveBriefingCache(
        script: String, 
        timestamp: Long, 
        weather: String? = null, 
        fact: String? = null,
        status: String? = null,
        error: String? = null
    ) {
        context.dataStore.edit { settings ->
            settings[LAST_BRIEFING_SCRIPT] = script
            settings[LAST_BRIEFING_TIME] = timestamp
            weather?.let { settings[LAST_WEATHER_CACHE] = it } ?: settings.remove(LAST_WEATHER_CACHE)
            fact?.let { settings[LAST_FACT_CACHE] = it } ?: settings.remove(LAST_FACT_CACHE)
            status?.let { settings[LAST_GEN_STATUS] = it } ?: settings.remove(LAST_GEN_STATUS)
            error?.let { settings[LAST_GEN_ERROR] = it } ?: settings.remove(LAST_GEN_ERROR)
        }
    }

    val lastWeatherCacheFlow: Flow<String?> = context.dataStore.data.map { it[LAST_WEATHER_CACHE] }
    val lastFactCacheFlow: Flow<String?> = context.dataStore.data.map { it[LAST_FACT_CACHE] }
    val lastGenStatusFlow: Flow<String> = context.dataStore.data.map { it[LAST_GEN_STATUS] ?: "waiting:pending" }
    val lastGenErrorFlow: Flow<String?> = context.dataStore.data.map { it[LAST_GEN_ERROR] }

    val isAutoLocationFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_AUTO_LOCATION_KEY] ?: false // Default to manual location
    }

    suspend fun saveIsAutoLocation(isAuto: Boolean) {
        context.dataStore.edit { settings ->
            settings[IS_AUTO_LOCATION_KEY] = isAuto
        }
    }

    val alarmDefaultsFlow: Flow<AlarmDefaults> = context.dataStore.data.map { preferences ->
        AlarmDefaults(
            snoozeDurationMinutes = preferences[DEFAULT_SNOOZE_DURATION] ?: 5,
            isGentleWake = preferences[DEFAULT_IS_GENTLE_WAKE] ?: false,
            crescendoDurationMinutes = preferences[DEFAULT_CRESCENDO_DURATION] ?: 1,
            mathDifficulty = preferences[DEFAULT_MATH_DIFFICULTY] ?: 0,
            mathProblemCount = preferences[DEFAULT_MATH_PROBLEM_COUNT] ?: 3,
            mathGraduallyIncreaseDifficulty = preferences[DEFAULT_MATH_GRADUAL_DIFFICULTY] ?: false,
            smileToDismiss = preferences[DEFAULT_SMILE_TO_DISMISS] ?: false,
            smileFallbackMethod = preferences[DEFAULT_SMILE_FALLBACK_METHOD] ?: "MATH",
            isBriefingEnabled = preferences[DEFAULT_IS_BRIEFING_ENABLED] ?: true,
            briefingIncludeWeather = preferences[BRIEFING_INCLUDE_WEATHER] ?: true,
            briefingIncludeCalendar = preferences[BRIEFING_INCLUDE_CALENDAR] ?: true,
            briefingIncludeFact = preferences[BRIEFING_INCLUDE_FACT] ?: true,
            briefingUserName = preferences[BRIEFING_USER_NAME] ?: "",
            isTtsEnabled = preferences[DEFAULT_IS_TTS_ENABLED] ?: true,
            isVibrate = preferences[DEFAULT_IS_VIBRATE] ?: true,
            isSoundEnabled = preferences[DEFAULT_IS_SOUND_ENABLED] ?: true,
            isEvasiveSnooze = preferences[DEFAULT_IS_EVASIVE_SNOOZE] ?: false,
            evasiveSnoozesBeforeMoving = preferences[DEFAULT_EVASIVE_SNOOZES_BEFORE] ?: 0,
            isSmoothFadeOut = preferences[DEFAULT_IS_SMOOTH_FADE_OUT] ?: true,
            isSnoozeEnabled = preferences[DEFAULT_IS_SNOOZE_ENABLED] ?: true,
            weekendDays = preferences[WEEKEND_DAYS]?.map { it.toInt() }?.toSet() ?: setOf(6, 7),
            defaultSoundUri = preferences[DEFAULT_SOUND_URI],
            aiPersona = preferences[AI_PERSONA] ?: "COACH",
            aiPersonaSurprise = preferences[AI_PERSONA_SURPRISE] ?: false,
            promptCoach = preferences[PROMPT_COACH] ?: context.getString(R.string.persona_prompt_coach),
            promptComedian = preferences[PROMPT_COMEDIAN] ?: context.getString(R.string.persona_prompt_comedian),
            promptZen = preferences[PROMPT_ZEN] ?: context.getString(R.string.persona_prompt_zen),
            promptHypeman = preferences[PROMPT_HYPEMAN] ?: context.getString(R.string.persona_prompt_hypeman),
            isSmartWakeupEnabled = preferences[DEFAULT_IS_SMART_WAKEUP] ?: false,
            wakeupCheckDelayMinutes = preferences[DEFAULT_WAKEUP_CHECK_DELAY] ?: 3,
            wakeupCheckTimeoutSeconds = preferences[DEFAULT_WAKEUP_CHECK_TIMEOUT] ?: 60,
            briefingTimeoutSeconds = preferences[DEFAULT_BRIEFING_TIMEOUT] ?: 30,
            vibrationPattern = preferences[DEFAULT_VIBRATION_PATTERN] ?: "BASIC",
            vibrationCrescendoStartGapSeconds = preferences[DEFAULT_VIBRATION_START_GAP] ?: 30
        )
    }

    suspend fun saveAlarmDefaults(defaults: AlarmDefaults) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_SNOOZE_DURATION] = defaults.snoozeDurationMinutes
            preferences[DEFAULT_IS_GENTLE_WAKE] = defaults.isGentleWake
            preferences[DEFAULT_CRESCENDO_DURATION] = defaults.crescendoDurationMinutes
            preferences[DEFAULT_MATH_DIFFICULTY] = defaults.mathDifficulty
            preferences[DEFAULT_MATH_PROBLEM_COUNT] = defaults.mathProblemCount
            preferences[DEFAULT_MATH_GRADUAL_DIFFICULTY] = defaults.mathGraduallyIncreaseDifficulty
            preferences[DEFAULT_SMILE_TO_DISMISS] = defaults.smileToDismiss
            preferences[DEFAULT_SMILE_FALLBACK_METHOD] = defaults.smileFallbackMethod
            preferences[DEFAULT_IS_BRIEFING_ENABLED] = defaults.isBriefingEnabled
            preferences[BRIEFING_INCLUDE_WEATHER] = defaults.briefingIncludeWeather
            preferences[BRIEFING_INCLUDE_CALENDAR] = defaults.briefingIncludeCalendar
            preferences[BRIEFING_INCLUDE_FACT] = defaults.briefingIncludeFact
            preferences[BRIEFING_USER_NAME] = defaults.briefingUserName
            preferences[DEFAULT_IS_TTS_ENABLED] = defaults.isTtsEnabled
            preferences[DEFAULT_IS_VIBRATE] = defaults.isVibrate
            preferences[DEFAULT_IS_SOUND_ENABLED] = defaults.isSoundEnabled
            preferences[DEFAULT_IS_EVASIVE_SNOOZE] = defaults.isEvasiveSnooze
            preferences[DEFAULT_EVASIVE_SNOOZES_BEFORE] = defaults.evasiveSnoozesBeforeMoving
            preferences[DEFAULT_IS_SMOOTH_FADE_OUT] = defaults.isSmoothFadeOut
            preferences[DEFAULT_IS_SNOOZE_ENABLED] = defaults.isSnoozeEnabled
            preferences[WEEKEND_DAYS] = defaults.weekendDays.map { it.toString() }.toSet()
            defaults.defaultSoundUri?.let { preferences[DEFAULT_SOUND_URI] = it } ?: preferences.remove(DEFAULT_SOUND_URI)
            preferences[AI_PERSONA] = defaults.aiPersona
            preferences[AI_PERSONA_SURPRISE] = defaults.aiPersonaSurprise
            preferences[PROMPT_COACH] = defaults.promptCoach
            preferences[PROMPT_COMEDIAN] = defaults.promptComedian
            preferences[PROMPT_ZEN] = defaults.promptZen
            preferences[PROMPT_HYPEMAN] = defaults.promptHypeman
            preferences[DEFAULT_IS_SMART_WAKEUP] = defaults.isSmartWakeupEnabled
            preferences[DEFAULT_WAKEUP_CHECK_DELAY] = defaults.wakeupCheckDelayMinutes
            preferences[DEFAULT_WAKEUP_CHECK_TIMEOUT] = defaults.wakeupCheckTimeoutSeconds
            preferences[DEFAULT_BRIEFING_TIMEOUT] = defaults.briefingTimeoutSeconds
            preferences[DEFAULT_VIBRATION_PATTERN] = defaults.vibrationPattern
            preferences[DEFAULT_VIBRATION_START_GAP] = defaults.vibrationCrescendoStartGapSeconds
        }
    }

    // ---------- Onboarding ----------
    val onboardingCompleteFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETE] ?: false
    }

    suspend fun saveOnboardingComplete() {
        context.dataStore.edit { settings ->
            settings[ONBOARDING_COMPLETE] = true
        }
    }

    suspend fun saveBriefingUserName(name: String) {
        context.dataStore.edit { settings ->
            settings[BRIEFING_USER_NAME] = name
        }
    }

    // ---------- Confirmed Buddy Numbers ----------
    val confirmedBuddyNumbersFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[CONFIRMED_BUDDY_NUMBERS] ?: emptySet()
    }

    suspend fun addConfirmedBuddyNumber(phoneNumber: String) {
        context.dataStore.edit { settings ->
            val current = settings[CONFIRMED_BUDDY_NUMBERS] ?: emptySet()
            settings[CONFIRMED_BUDDY_NUMBERS] = current + phoneNumber
        }
    }

    suspend fun removeConfirmedBuddyNumber(phoneNumber: String) {
        context.dataStore.edit { settings ->
            val current = settings[CONFIRMED_BUDDY_NUMBERS] ?: emptySet()
            settings[CONFIRMED_BUDDY_NUMBERS] = current - phoneNumber
        }
    }

    // ---------- Pending Buddy Codes ----------
    val pendingBuddyCodesFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[PENDING_BUDDY_CODES] ?: emptySet()
    }

    suspend fun addPendingBuddyCode(code: String, phoneNumber: String) {
        context.dataStore.edit { settings ->
            val current = settings[PENDING_BUDDY_CODES] ?: emptySet()
            // Clean up any existing codes for this phone number first
            val filtered = current.filterNot { it.endsWith(":$phoneNumber") }.toSet()
            settings[PENDING_BUDDY_CODES] = filtered + "$code:$phoneNumber"
        }
    }

    suspend fun removePendingBuddyCode(code: String, phoneNumber: String) {
        context.dataStore.edit { settings ->
            val current = settings[PENDING_BUDDY_CODES] ?: emptySet()
            settings[PENDING_BUDDY_CODES] = current - "$code:$phoneNumber"
        }
    }

    suspend fun removeAllPendingCodesForNumber(phoneNumber: String) {
        context.dataStore.edit { settings ->
            val current = settings[PENDING_BUDDY_CODES] ?: emptySet()
            settings[PENDING_BUDDY_CODES] = current.filterNot { it.endsWith(":$phoneNumber") }.toSet()
        }
    }

    // ---------- Global Buddies ----------
    val globalBuddiesFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[GLOBAL_BUDDIES] ?: emptySet()
    }

    suspend fun addGlobalBuddy(name: String, phoneNumber: String) {
        context.dataStore.edit { settings ->
            val current = settings[GLOBAL_BUDDIES] ?: emptySet()
            // Remove existing entry for this phone number if it exists
            val filtered = current.filterNot { it.split("|").getOrNull(1) == phoneNumber }.toSet()
            settings[GLOBAL_BUDDIES] = filtered + "$name|$phoneNumber"
        }
    }

    suspend fun removeGlobalBuddy(name: String, phoneNumber: String) {
        context.dataStore.edit { settings ->
            val current = settings[GLOBAL_BUDDIES] ?: emptySet()
            settings[GLOBAL_BUDDIES] = current - "$name|$phoneNumber"
        }
    }

    // ---------- Buddy Helpers ----------
    fun isBuddyConfirmed(phoneNumber: String): Flow<Boolean> = confirmedBuddyNumbersFlow.map { confirmed ->
        confirmed.contains(phoneNumber)
    }

    fun getBuddyNameFromGlobalList(phoneNumber: String): Flow<String?> = globalBuddiesFlow.map { buddies ->
        buddies.find { it.split("|").getOrNull(1) == phoneNumber }?.split("|")?.getOrNull(0)
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}

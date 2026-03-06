package com.elroi.alarmpal.domain.generator

import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import com.elroi.alarmpal.domain.manager.SettingsManager
import com.elroi.alarmpal.domain.manager.BriefingStateManager
import com.elroi.alarmpal.domain.manager.CalendarManager
import com.elroi.alarmpal.domain.manager.GeminiManager
import com.elroi.alarmpal.domain.manager.LocalLLMManager
import com.elroi.alarmpal.domain.manager.BriefingLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BriefingGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val calendarManager: CalendarManager,
    private val settingsManager: SettingsManager,
    private val geminiManager: GeminiManager,
    private val scriptBuilder: BriefingScriptBuilder,
    private val localLLMManager: LocalLLMManager,
    private val alarmRepository: com.elroi.alarmpal.domain.repository.AlarmRepository,
    private val briefingLogger: BriefingLogger
) {
    private val generationLock = Mutex()

    suspend fun generateBriefing(alarmId: String? = null): String = withContext(Dispatchers.IO) {
        val cached = getCachedBriefing()
        if (cached != null) {
            android.util.Log.d("BriefingGenerator", "Using cached briefing script.")
            return@withContext cached
        }
        
        val script = generationLock.withLock {
            val postLockCached = getCachedBriefing()
            if (postLockCached != null) return@withLock postLockCached
            
            BriefingStateManager.updateStatus("Activating LemurLoop brain cells...")
            val generated = generateFullBriefing(alarmId)
            
            generated
        }
        
        if (script != null && script.isNotBlank() && !script.startsWith("ERROR:")) {
            return@withContext script.trim()
        }
        
        BriefingStateManager.updateStatus("AI is sleeping... using standard backup script.")
        "Rise and shine! Your day is waiting for you."
    }

    suspend fun refreshBriefing(alarmId: String? = null): String? = withContext(Dispatchers.IO) {
        generationLock.withLock {
            generateFullBriefing(alarmId)
        }
    }

    private suspend fun getCachedBriefing(): String? {
        val script = settingsManager.lastBriefingScriptFlow.first()
        val timestamp = settingsManager.lastBriefingTimeFlow.first()
        
        if (script == null || script.isBlank() || script.startsWith("ERROR:")) return null
        
        val isFresh = (System.currentTimeMillis() - timestamp) < (2 * 3600 * 1000)
        return if (isFresh) script else null
    }

    private suspend fun generateFullBriefing(alarmId: String?): String? {
        val isAutoLocation = settingsManager.isAutoLocationFlow.first()
        var location = settingsManager.locationFlow.first().ifBlank { "New York" }
        val isCelsius = settingsManager.isCelsiusFlow.first()
        
        val settings = settingsManager.alarmDefaultsFlow.first()
        val alarmOverride = alarmId?.let { alarmRepository.getAlarmById(it) }?.aiPersona

        if (isAutoLocation) {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val hasPermission = context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                if (hasPermission) {
                    val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) 
                        ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        
                    lastKnownLocation?.let {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            location = addr.locality ?: addr.subAdminArea ?: location
                        }
                    }
                }
            } catch (e: Exception) {}
        }

        var weatherStatus = "skipped"
        var weatherText = ""
        if (settings.briefingIncludeWeather) {
            BriefingStateManager.updateStatus("Querying Open-Meteo for stable weather...")
            weatherStatus = "fail"
            weatherText = try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocationName(location, 1)
                if (!addresses.isNullOrEmpty()) {
                    val lat = addresses[0].latitude
                    val lon = addresses[0].longitude
                    val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
                    val connection = URL(weatherUrl).openConnection() as HttpURLConnection
                    connection.connectTimeout = 10000
                    if (connection.responseCode == 200) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(response)
                        val current = json.getJSONObject("current_weather")
                        val temp = current.getDouble("temperature")
                        val code = current.getInt("weathercode")
                        val weatherDesc = getWeatherDescription(code)
                        val displayTemp = if (isCelsius) "${temp}°C" else "${(temp * 9/5) + 32}°F"
                        weatherStatus = "ok"
                        "$weatherDesc, $displayTemp"
                    } else "Weather unavailable."
                } else "Location not found."
            } catch (e: Exception) { "Weather service timeout." }

            if (weatherStatus == "fail") {
                settingsManager.lastWeatherCacheFlow.first()?.let {
                    weatherText = it
                    weatherStatus = "cached"
                }
            }
            BriefingStateManager.updateComponentStatus("weather", weatherStatus)
        }

        var calendarText = ""
        if (settings.briefingIncludeCalendar) {
            BriefingStateManager.updateStatus("Sifting through your digital obligations...")
            val events = calendarManager.getUpcomingEvents()
            calendarText = if (events.isNotEmpty()) {
                val eventList = events.take(3).joinToString("; ") { event ->
                    val time = java.time.Instant.ofEpochMilli(event.startTime)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalTime()
                        .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
                    "${event.title} at $time"
                }
                "You have ${events.size} events today. Highlights: $eventList."
            } else "No events scheduled for today."
            BriefingStateManager.updateComponentStatus("calendar", "ok")
        }

        var factStatus = "skipped"
        var funFactText = ""
        if (settings.briefingIncludeFact) {
            BriefingStateManager.updateStatus("Raiding the forbidden library of trivia...")
            factStatus = "fail"
            funFactText = try {
                val factUrl = "https://uselessfacts.jsph.pl/api/v2/facts/today"
                val connection = URL(factUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    factStatus = "ok"
                    json.getString("text")
                } else "No fun fact today."
            } catch (e: Exception) { "Fact service offline." }

            if (factStatus == "fail") {
                settingsManager.lastFactCacheFlow.first()?.let {
                    funFactText = it
                    factStatus = "cached"
                }
            }
        }

        val isCloudEnabled = settingsManager.isCloudAiEnabledFlow.first()
        val preferredTier = settingsManager.preferredAiTierFlow.first()
        val fallbackOrder = settingsManager.aiFallbackOrderFlow.first()
        val persona = alarmOverride ?: settings.aiPersona
        
        var aiScript: String? = null
        var aiSuccess = false

        val localDraft = scriptBuilder.buildLocalBriefing(persona, location, weatherText, calendarText, funFactText)
        val personaInstruction = getPersonaInstruction(persona, settings)
        val temperament = getPersonaTemperament(persona)
        // Full prompt for Cloud (Gemini Flash handles large context fine)
        val cloudPrompt = buildEnhancementPrompt(personaInstruction, temperament, localDraft, settings.briefingUserName)
        // Short prompt for Local LLM (Gemma 2B has ~1024 token limit; temperament rules overflow it)
        val localPrompt = buildLocalLLMPrompt(personaInstruction, localDraft)
        
        android.util.Log.d("BriefingGenerator", "Generated Draft:\n$localDraft")
        android.util.Log.d("BriefingGenerator", "Generated Enhancement Prompt (cloud):\n$cloudPrompt")
        
        val attempts = mutableListOf<String>()
        if (fallbackOrder == "LOCAL_THEN_CLOUD") {
            if (preferredTier == "ADVANCED") attempts.add("ADVANCED")
            if (isCloudEnabled) attempts.add("CLOUD")
        } else {
            if (isCloudEnabled) attempts.add("CLOUD")
            if (preferredTier == "ADVANCED") attempts.add("ADVANCED")
        }

        var actualEngineUsed = "DRAFT_ONLY"
        for (tier in attempts) {
            if (aiSuccess) break
            
            BriefingStateManager.updateStatus(if (tier == "CLOUD") "Adding persona with Cloud AI..." else "Adding persona locally with Gemma...")
            val finalResult: String? = when (tier) {
                "CLOUD" -> if (isCloudEnabled) {
                    try {
                        var lastResult: String? = null
                        geminiManager.generateContentStreaming(cloudPrompt).collect { partialText ->
                            lastResult = partialText
                            val wordCount = partialText.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                            BriefingStateManager.updateStatus("AI is writing... ($wordCount words)")
                        }
                        lastResult
                    } catch (e: Exception) {
                        android.util.Log.e("BriefingGenerator", "Cloud AI error", e); null
                    }
                } else null
                "ADVANCED" -> if (preferredTier == "ADVANCED") {
                    try {
                        val status = localLLMManager.checkStatus()
                        if (status == com.elroi.alarmpal.domain.manager.GeminiNanoStatus.SUPPORTED) {
                            localLLMManager.generateContent(localPrompt)  // use compact prompt!
                        } else null
                    } catch (e: Exception) {
                        android.util.Log.e("BriefingGenerator", "Local LLM error", e); null
                    }
                } else null
                else -> null
            }
            
            if (finalResult != null && !finalResult.startsWith("ERROR") && finalResult.isNotBlank()) {
                // Strip "Sure, here is..." / "Here's the rewritten..." / "Certainly..." style preamble
                // from Local LLM which often adds an unwanted introductory sentence before the actual content.
                val cleanedResult = if (tier == "ADVANCED") stripLocalAIPreamble(finalResult) else finalResult
                val draftParagraphs = localDraft.trim().split("\n\n").filter { it.isNotBlank() }
                val aiParagraphs = cleanedResult.trim().split("\n\n").filter { it.isNotBlank() }
                
                // If the AI dropped paragraphs, accept what it wrote for existing ones but
                // fall back to the draft for the entire script so data is never lost.
                if (aiParagraphs.size >= draftParagraphs.size) {
                    aiScript = cleanedResult
                    aiSuccess = true
                    actualEngineUsed = tier
                    BriefingStateManager.updateComponentStatus("ai", "ok")
                    android.util.Log.d("BriefingGenerator", "AI kept all ${aiParagraphs.size} paragraphs ✓")
                } else {
                    android.util.Log.w("BriefingGenerator", "AI dropped paragraphs (draft=${draftParagraphs.size}, ai=${aiParagraphs.size}). Using local draft.")
                    aiScript = localDraft
                    aiSuccess = false // keep as false so we know it's the draft
                }
            }
        }

        if (!aiSuccess && aiScript.isNullOrBlank()) {
            BriefingStateManager.updateStatus("AI is offline... building a custom local briefing for you.")
        } else if (!aiSuccess) {
            BriefingStateManager.updateStatus("AI stumbled ($aiScript)... using local fallback.")
        }
        
        val healthStatus = "weather:$weatherStatus|calendar:ok|fact:$factStatus|ai:${if(aiSuccess) "ok" else "draft"}"
        val title = getPersonaTitleAndEmoji(persona)
        val sourceTag = if (aiSuccess) "" else " [draft]"
        val finalScript = if (aiSuccess && aiScript != null) "$title$sourceTag\n\n${aiScript.trim()}" else {
            "$title [draft]\n\n${localDraft.trim()}"
        }
        
        settingsManager.saveBriefingCache(
            script = finalScript,
            timestamp = System.currentTimeMillis(),
            weather = if (weatherStatus == "ok") weatherText else null,
            fact = if (factStatus == "ok") funFactText else null,
            status = healthStatus,
            error = when {
                aiSuccess -> null
                !isCloudEnabled -> "AI skipped: Cloud Enhancement is disabled."
                aiScript != null -> if (aiScript!!.length > 100) aiScript!!.take(100) else aiScript
                else -> "Cloud AI failure: returned blank result."
            }
        )
        
        briefingLogger.logBriefing(
            engineUsed = actualEngineUsed,
            isFallbackTriggered = attempts.indexOf(actualEngineUsed).let { it > 0 },
            result = if (aiSuccess) "SUCCESS" else "FALLBACK_DRAFT",
            details = "Alarm: ${alarmId ?: "Test/Preview"} | Persona: $persona | Fallback Pref: $fallbackOrder",
            briefingScript = finalScript
        )
        
        return finalScript.trim()
    }

    private fun getPersonaInstruction(persona: String, settings: com.elroi.alarmpal.domain.manager.AlarmDefaults): String {
        return when(persona) {
            "COMEDIAN" -> settings.promptComedian
            "ZEN" -> settings.promptZen
            "HYPEMAN" -> settings.promptHypeman
            else -> settings.promptCoach
        }
    }

    /**
     * Strips common LLM preamble lines from local model output.
     * Gemma 2B often starts with "Sure, here is...", "Certainly!", "Here's the...", etc.
     * We detect leading lines that look like meta-commentary and drop them.
     */
    private fun stripLocalAIPreamble(text: String): String {
        val preamblePatterns = listOf(
            Regex("^sure[,!.].*", RegexOption.IGNORE_CASE),
            Regex("^certainly[,!.].*", RegexOption.IGNORE_CASE),
            Regex("^here(?:'s| is| are).*", RegexOption.IGNORE_CASE),
            Regex("^of course[,!.].*", RegexOption.IGNORE_CASE),
            Regex("^absolutely[,!.].*", RegexOption.IGNORE_CASE),
            Regex("^i'?(?:ve)? rewritten.*", RegexOption.IGNORE_CASE),
            Regex("^below is.*", RegexOption.IGNORE_CASE),
        )
        
        val lines = text.trimStart().lines()
        var startIndex = 0
        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.isEmpty()) {
                // Skip leading blank lines
                startIndex = i + 1
                continue
            }
            if (preamblePatterns.any { it.matches(line) }) {
                startIndex = i + 1
            } else {
                break
            }
        }
        return lines.drop(startIndex).joinToString("\n").trimStart()
    }

    private fun getPersonaTemperament(persona: String): com.elroi.alarmpal.domain.manager.PersonaTemperament {
        return when(persona) {
            "COMEDIAN" -> com.elroi.alarmpal.domain.manager.PersonaTemperament.COMEDIAN
            "ZEN" -> com.elroi.alarmpal.domain.manager.PersonaTemperament.ZEN
            "HYPEMAN" -> com.elroi.alarmpal.domain.manager.PersonaTemperament.HYPEMAN
            else -> com.elroi.alarmpal.domain.manager.PersonaTemperament.COACH
        }
    }

    private fun getPersonaTitleAndEmoji(persona: String): String {
        return when(persona) {
            "COMEDIAN" -> "🤡 The Sarcastic Friend"
            "ZEN" -> "🧘 The Zen Master"
            "HYPEMAN" -> "🚀 The Hype-Man"
            "SURPRISE" -> "🎲 Surprise Me"
            else -> "🪖 The Drill Sergeant"
        }
    }

    private fun buildEnhancementPrompt(
        personaInstruction: String,
        temperament: com.elroi.alarmpal.domain.manager.PersonaTemperament,
        draftBriefing: String,
        userName: String
    ): String {
        val nameInstruction = if (userName.isNotBlank()) "2. Greet the user by their name: $userName." else ""
        val paragraphCount = draftBriefing.trim().split("\n\n").filter { it.isNotBlank() }.size
        return """
            You are a voice actor polishing a draft script. Your job is to rewrite the DRAFT SCRIPT STRICTLY using the PERSONA voice.
            
            PERSONA: $personaInstruction

            TEMPERAMENT STYLE RULES (apply these strictly):
            ${temperament.styleRules}

            EXAMPLE PHRASES (for tone reference only — do NOT copy verbatim):
            "${temperament.examplePhrases}"
            
            TRANSLATION RULES (CRITICAL):
            1. DO NOT add, invent, or remove ANY facts. You MUST include the exact Time, Temperature, Calendar events, and the Daily Fact from the DRAFT SCRIPT.
            $nameInstruction
            3. The DRAFT SCRIPT has exactly $paragraphCount paragraphs. Your FINAL SCRIPT MUST ALSO have exactly $paragraphCount paragraphs separated by blank lines. DO NOT merge paragraphs.
            4. The Daily Fact MUST remain perfectly isolated in the final paragraph.
            5. ONLY alter the tone and vocabulary to fit the PERSONA. You MUST finish every sentence completely. DO NOT cut off mid-sentence.
            6. Do NOT ask questions or offer assistance.
            
            DRAFT SCRIPT:
            $draftBriefing
            
            FINAL SCRIPT:
        """.trimIndent()
    }
    /**
     * Compact prompt for on-device Gemma 2B (Advanced tier).
     * Must stay well under ~800 tokens to avoid native SIGABRT crash.
     * No temperament rules, no examples — just the core persona + draft.
     */
    private fun buildLocalLLMPrompt(personaInstruction: String, draftBriefing: String): String {
        return """Rewrite the SCRIPT below in the voice of: $personaInstruction
Rules: keep all facts exactly as written. Same number of paragraphs. No new info.
SCRIPT:
$draftBriefing
REWRITTEN:""".trimIndent()
    }

    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1, 2, 3 -> "Mainly clear"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            71, 73, 75 -> "Snow"
            80, 81, 82 -> "Rain showers"
            95 -> "Thunderstorm"
            else -> "Varied conditions"
        }
    }
}

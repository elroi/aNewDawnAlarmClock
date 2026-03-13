package com.elroi.lemurloop.domain.manager

/**
 * Short sample phrases for each persona, used for both on-device and Cloud TTS previews.
 * These are intentionally brief so users can quickly compare voices without hearing
 * a full briefing.
 */
object PersonaPreviewSamples {

    fun getPreviewText(personaId: String): String {
        return when (personaId) {
            "COMEDIAN" -> "Oh, you're awake. Impressive."
            "ZEN" -> "Breathe in, breathe out. The day awaits."
            "HYPEMAN" -> "Let's go! You absolutely got this."
            // SURPRISE is a mode, not a fixed persona. Reuse the coach-style tone.
            "SURPRISE" -> "Good morning. Today has plenty of surprises."
            else -> "Rise and shine. No excuses, just results."
        }
    }
}


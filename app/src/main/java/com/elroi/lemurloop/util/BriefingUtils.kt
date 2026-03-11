package com.elroi.lemurloop.util

/**
 * Utility for processing briefing text for display and speech.
 */
object BriefingUtils {

    /**
     * Filters the briefing text for Text-to-Speech by removing visual-only headers.
     *
     * Specifically, it removes:
     * 1. Lines starting with # or * (Markdown headers/lists)
     * 2. The persona title line (e.g., "🤡 The Sarcastic Friend" plus any source emoji ✅/✈/☁️)
     * 3. The "[draft]" tag (kept for backward compatibility with older cached scripts)
     */
    fun filterBriefingForTts(text: String?): String {
        if (text.isNullOrBlank()) return ""

        val personaTitles = listOf(
            "The Sarcastic Friend",
            "The Zen Master",
            "The Hype-Man",
            "Surprise Me",
            "The Drill Sergeant"
        )

        return text.lines()
            .filterNot { line ->
                val trimmedLine = line.trim()
                
                // 1. Skip Markdown headers/bullets
                if (trimmedLine.startsWith("#") || trimmedLine.startsWith("*")) return@filterNot true
                
                // 2. Skip persona titles (with or without emoji)
                val isTitleLine = personaTitles.any { title -> 
                    trimmedLine.contains(title, ignoreCase = true)
                }
                
                // 3. Skip lines that are just "[draft]"
                val isDraftOnly = trimmedLine.equals("[draft]", ignoreCase = true)
                
                isTitleLine || isDraftOnly
            }
            .joinToString("\n")
            .replace("[draft]", "", ignoreCase = true) // Remove tag if mid-line (unlikely but safe)
            .trim()
    }
}

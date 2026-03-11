package com.elroi.lemurloop.domain.manager

/**
 * Lightweight model representing a single searchable settings item.
 *
 * The static list of [SettingSearchItem] entries acts as the canonical registry
 * of settings that should appear in search. When adding a new setting that
 * should be searchable, also add a corresponding entry here.
 */
data class SettingSearchItem(
    val id: String,
    val title: String,
    val description: String = "",
    val section: String = "",
    val target: SettingSearchTarget
)

/**
 * Describes what should happen when a search result is selected.
 * UI/ViewModel map this to concrete navigation or in-place actions.
 */
sealed class SettingSearchTarget {
    /**
     * Navigate to a logical section within the Settings screen itself.
     * The [sectionId] should match existing section identifiers such as
     * \"MORNING\", \"WAKEUP\", \"ACCOUNTABILITY\", \"INTELLIGENCE\", or \"HELP\".
     */
    data class Section(val sectionId: String) : SettingSearchTarget()

    /**
     * Navigate to a dedicated sub-screen reachable from Settings
     * (e.g., About, Onboarding/tutorial, Diagnostic logs).
     */
    data class SubScreen(val route: String) : SettingSearchTarget()

    /**
     * Represents a setting that is toggled or otherwise acted on directly
     * from the current Settings surface.
     */
    data class InlineAction(val actionId: String) : SettingSearchTarget()
}

/**
 * Central registry of settings that should appear in the Settings search
 * experience. This is a static list by design so it is easy to reason about.
 *
 * When adding a new setting that should be searchable, add a corresponding
 * [SettingSearchItem] entry to [allItems].
 */
object SettingsSearchRegistry {
    val allItems: List<SettingSearchItem> = listOf(
        // Morning Experience
        SettingSearchItem(
            id = "morning_personality",
            title = "Companion personality",
            description = "Choose how LemurLoop talks to you in the morning",
            section = "Morning Experience",
            target = SettingSearchTarget.InlineAction("scroll_morning_personality")
        ),
        SettingSearchItem(
            id = "briefing_tts",
            title = "Read briefing aloud",
            description = "Enable text-to-speech for your morning briefing",
            section = "Morning Experience",
            target = SettingSearchTarget.InlineAction("scroll_briefing_content")
        ),
        SettingSearchItem(
            id = "briefing_content",
            title = "Aura report content",
            description = "Weather, calendar events, and fun fact in your briefing",
            section = "Morning Experience",
            target = SettingSearchTarget.InlineAction("scroll_briefing_content")
        ),

        // Wake-Up Engine
        SettingSearchItem(
            id = "alarm_creation_style",
            title = "Alarm creation style",
            description = "Choose between guided wizard or simple setup",
            section = "Wake-Up Engine",
            target = SettingSearchTarget.Section("WAKEUP")
        ),
        SettingSearchItem(
            id = "alarm_sound",
            title = "Alarm sound",
            description = "Pick the default alarm sound",
            section = "Wake-Up Engine",
            target = SettingSearchTarget.Section("WAKEUP")
        ),
        SettingSearchItem(
            id = "vibration",
            title = "Vibration",
            description = "Enable vibration and choose a pattern",
            section = "Wake-Up Engine",
            target = SettingSearchTarget.Section("WAKEUP")
        ),
        SettingSearchItem(
            id = "gentle_wake",
            title = "Gentle wake",
            description = "Start softly and gradually increase intensity",
            section = "Wake-Up Engine",
            target = SettingSearchTarget.Section("WAKEUP")
        ),
        SettingSearchItem(
            id = "snooze_duration",
            title = "Snooze duration",
            description = "How long each snooze lasts",
            section = "Wake-Up Engine",
            target = SettingSearchTarget.InlineAction("scroll_snooze")
        ),
        SettingSearchItem(
            id = "math_challenge",
            title = "Math challenge",
            description = "Solve math problems to dismiss your alarm",
            section = "Wake-Up Engine",
            target = SettingSearchTarget.InlineAction("scroll_math")
        ),
        SettingSearchItem(
            id = "face_game",
            title = "Face game",
            description = "Smile to dismiss your alarm",
            section = "Wake-Up Engine",
            target = SettingSearchTarget.InlineAction("scroll_face_game")
        ),
        SettingSearchItem(
            id = "weekend_days",
            title = "Weekend days",
            description = "Choose which days count as your weekend",
            section = "Wake-Up Engine",
            target = SettingSearchTarget.Section("WAKEUP")
        ),

        // Accountability
        SettingSearchItem(
            id = "accountability_buddies",
            title = "Accountability buddies",
            description = "Manage who gets notified about your wake-ups",
            section = "Accountability",
            target = SettingSearchTarget.Section("ACCOUNTABILITY")
        ),

        // Intelligence
        SettingSearchItem(
            id = "cloud_ai",
            title = "Cloud AI enhancement",
            description = "Use Gemini for personalized briefings",
            section = "Intelligence",
            target = SettingSearchTarget.Section("INTELLIGENCE")
        ),
        SettingSearchItem(
            id = "gemini_api_key",
            title = "Gemini API key",
            description = "Manage your Gemini API credentials",
            section = "Intelligence",
            target = SettingSearchTarget.Section("INTELLIGENCE")
        ),
        SettingSearchItem(
            id = "intelligence_health",
            title = "Intelligence health",
            description = "Check the health of your AI, weather, and calendar",
            section = "Intelligence",
            target = SettingSearchTarget.Section("INTELLIGENCE")
        ),

        // Help & System
        SettingSearchItem(
            id = "about",
            title = "About LemurLoop",
            description = "Version, credits, and legal",
            section = "Help & System",
            target = SettingSearchTarget.SubScreen("about")
        ),
        SettingSearchItem(
            id = "replay_tutorial",
            title = "Replay tutorial",
            description = "See the setup wizard again",
            section = "Help & System",
            target = SettingSearchTarget.SubScreen("onboarding")
        ),
        SettingSearchItem(
            id = "demo_alarms",
            title = "Create demo alarms",
            description = "Add example alarms to explore LemurLoop",
            section = "Help & System",
            target = SettingSearchTarget.Section("HELP")
        ),
        SettingSearchItem(
            id = "privacy_policy",
            title = "Privacy policy",
            description = "Learn how your data is handled",
            section = "Help & System",
            target = SettingSearchTarget.Section("HELP")
        ),
        SettingSearchItem(
            id = "diagnostic_logs",
            title = "Diagnostic logs",
            description = "View system events and debug info",
            section = "Help & System",
            target = SettingSearchTarget.SubScreen("logs")
        ),
        SettingSearchItem(
            id = "danger_zone",
            title = "Danger zone",
            description = "Reset or wipe app data",
            section = "Help & System",
            target = SettingSearchTarget.Section("HELP")
        )
    )
}

/**
 * Case-insensitive, in-memory search over a list of [SettingSearchItem]s.
 *
 * Matches against title, description, and section label. The list is small,
 * so a simple O(n) scan per query is sufficient.
 */
fun searchSettings(
    query: String,
    items: List<SettingSearchItem>
): List<SettingSearchItem> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return emptyList()

    val q = trimmed.lowercase()

    return items.filter { item ->
        val title = item.title.lowercase()
        val desc = item.description.lowercase()
        val section = item.section.lowercase()

        title.contains(q) || desc.contains(q) || section.contains(q)
    }
}



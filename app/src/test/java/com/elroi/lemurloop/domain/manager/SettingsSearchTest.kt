package com.elroi.lemurloop.domain.manager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchTest {

    @Test
    fun searchSettings_matchesTitleDescriptionAndSection_caseInsensitive() {
        val items = listOf(
            SettingSearchItem(
                id = "1",
                title = "Alarm Sound",
                description = "Pick the default alarm sound",
                section = "Wake-Up Engine",
                target = SettingSearchTarget.Section("WAKEUP")
            ),
            SettingSearchItem(
                id = "2",
                title = "Privacy Policy",
                description = "Your data stays on device",
                section = "Help & System",
                target = SettingSearchTarget.Section("HELP")
            )
        )

        // Title match (case-insensitive)
        val byTitle = searchSettings("alarm", items)
        assertEquals(1, byTitle.size)
        assertEquals("1", byTitle.first().id)

        // Description match
        val byDescription = searchSettings("stays on", items)
        assertEquals(1, byDescription.size)
        assertEquals("2", byDescription.first().id)

        // Section match
        val bySection = searchSettings("help & system", items)
        assertEquals(1, bySection.size)
        assertEquals("2", bySection.first().id)
    }

    @Test
    fun searchSettings_trimsAndHandlesEmptyQuery() {
        val items = listOf(
            SettingSearchItem(
                id = "1",
                title = "Alarm Sound",
                description = "",
                section = "",
                target = SettingSearchTarget.Section("WAKEUP")
            )
        )

        // Empty -> no results
        assertTrue(searchSettings("", items).isEmpty())

        // Whitespace-only -> no results
        assertTrue(searchSettings("   ", items).isEmpty())

        // Trimmed query still matches
        val results = searchSettings("  alarm  ", items)
        assertEquals(1, results.size)
        assertEquals("1", results.first().id)
    }
}


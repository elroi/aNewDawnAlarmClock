package com.elroi.lemurloop.domain.manager

import org.junit.Assert.assertEquals
import org.junit.Test

class CloudPersonaVoiceConfigTest {

    @Test
    fun `english coach uses authoritative male voice`() {
        val config = getCloudPersonaVoiceConfig(personaId = "COACH", uiLanguage = "en")

        assertEquals("en-US-Wavenet-D", config.voiceName)
        assertEquals(1.25, config.speakingRate, 0.01)
        assertEquals(-2.0, config.pitch, 0.01)
    }

    @Test
    fun `english zen uses calm slower voice`() {
        val config = getCloudPersonaVoiceConfig(personaId = "ZEN", uiLanguage = "en")

        assertEquals("en-US-Wavenet-F", config.voiceName)
        assertEquals(0.85, config.speakingRate, 0.01)
        assertEquals(0.0, config.pitch, 0.01)
    }

    @Test
    fun `unknown persona falls back to neutral english voice`() {
        val config = getCloudPersonaVoiceConfig(personaId = "UNKNOWN", uiLanguage = "en")

        assertEquals("en-US-Wavenet-D", config.voiceName)
        assertEquals(1.0, config.speakingRate, 0.01)
        assertEquals(0.0, config.pitch, 0.01)
    }
}


package com.elroi.lemurloop.domain.manager

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonaVoiceConfigTest {

    @Test
    fun `COACH persona uses deep fast voice config`() {
        val config = PersonaVoiceConfig.forPersona("COACH")

        assertEquals("COACH should use lower pitch for a deeper sound", 0.8f, config.pitch)
        assertEquals("COACH should speak much faster to sound commanding", 1.4f, config.speechRate)
    }

    @Test
    fun `ZEN persona uses calm slow voice config`() {
        val config = PersonaVoiceConfig.forPersona("ZEN")

        assertEquals("ZEN should use neutral pitch", 1.0f, config.pitch)
        assertEquals("ZEN should speak noticeably slower to sound calm", 0.7f, config.speechRate)
    }

    @Test
    fun `HYPEMAN persona uses excited fast high voice config`() {
        val config = PersonaVoiceConfig.forPersona("HYPEMAN")

        assertEquals("HYPEMAN should use clearly higher pitch", 1.25f, config.pitch)
        assertEquals("HYPEMAN should speak extremely fast", 1.4f, config.speechRate)
    }

    @Test
    fun `COMEDIAN persona uses slightly fast neutral voice config`() {
        val config = PersonaVoiceConfig.forPersona("COMEDIAN")

        assertEquals("COMEDIAN should use slightly higher than neutral pitch", 1.1f, config.pitch)
        assertEquals("COMEDIAN should speak a bit faster for snappy delivery", 1.2f, config.speechRate)
    }

    @Test
    fun `unknown persona falls back to neutral config`() {
        val config = PersonaVoiceConfig.forPersona("UNKNOWN")

        assertEquals(1.0f, config.pitch)
        assertEquals(1.0f, config.speechRate)
    }
}


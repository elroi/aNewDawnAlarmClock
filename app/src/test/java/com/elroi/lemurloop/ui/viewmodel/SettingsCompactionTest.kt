package com.elroi.lemurloop.ui.viewmodel

import com.elroi.lemurloop.domain.manager.AlarmDefaults
import com.elroi.lemurloop.domain.manager.SettingsManager
import com.elroi.lemurloop.domain.manager.GeminiManager
import com.elroi.lemurloop.domain.generator.BriefingGenerator
import com.elroi.lemurloop.domain.manager.LocalLLMManager
import com.elroi.lemurloop.data.local.AppDatabase
import com.elroi.lemurloop.domain.manager.TtsEngine
import com.elroi.lemurloop.data.local.GoogleCloudTtsEngine
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsCompactionTest {

    private val settingsManager = mockk<SettingsManager>(relaxed = true)
    private val geminiManager = mockk<GeminiManager>(relaxed = true)
    private val briefingGenerator = mockk<BriefingGenerator>(relaxed = true)
    private val localLLMManager = mockk<LocalLLMManager>(relaxed = true)
    private val database = mockk<AppDatabase>(relaxed = true)
    private val ttsManager = mockk<TtsEngine>(relaxed = true)
    private val cloudTtsEngine = mockk<GoogleCloudTtsEngine>(relaxed = true)

    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock required flows
        every { settingsManager.alarmDefaultsFlow } returns MutableStateFlow(AlarmDefaults())
        every { settingsManager.locationFlow } returns MutableStateFlow("")
        every { settingsManager.isCelsiusFlow } returns MutableStateFlow(true)
        every { settingsManager.isAutoLocationFlow } returns MutableStateFlow(false)
        every { settingsManager.geminiApiKeyFlow } returns MutableStateFlow("")
        every { settingsManager.preferredAiTierFlow } returns MutableStateFlow("CLOUD")
        every { settingsManager.isCloudAiEnabledFlow } returns MutableStateFlow(true)
        every { settingsManager.aiFallbackOrderFlow } returns MutableStateFlow("CLOUD_THEN_LOCAL")
        every { settingsManager.globalBuddiesFlow } returns MutableStateFlow(emptySet())
        every { settingsManager.confirmedBuddyNumbersFlow } returns MutableStateFlow(emptySet())
        every { settingsManager.lastGenStatusFlow } returns MutableStateFlow("")
        every { settingsManager.lastGenErrorFlow } returns MutableStateFlow(null)
        every { settingsManager.lastBriefingScriptFlow } returns MutableStateFlow(null)
        every { settingsManager.alarmCreationStyleFlow } returns MutableStateFlow("WIZARD")

        val alarmDao = mockk<com.elroi.lemurloop.data.local.dao.AlarmDao>(relaxed = true)
        val sleepDao = mockk<com.elroi.lemurloop.data.local.dao.SleepRecordDao>(relaxed = true)
        every { database.alarmDao() } returns alarmDao
        every { database.sleepRecordDao() } returns sleepDao

        viewModel = SettingsViewModel(
            settingsManager,
            geminiManager,
            briefingGenerator,
            localLLMManager,
            database,
            ttsManager,
            cloudTtsEngine,
            demoAlarmSeeder = mockk(relaxed = true)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test sections are collapsed by default`() = runTest {
        // Sections should be empty (all collapsed) or contain nothing
        assertTrue("Expected all sections to be collapsed initially", viewModel.expandedSections.value.isEmpty())
    }

    @Test
    fun `test toggling section expands it`() = runTest {
        viewModel.toggleSection("MORNING")
        assertTrue("Expected MORNING section to be expanded", viewModel.expandedSections.value.contains("MORNING"))
        
        viewModel.toggleSection("MORNING")
        assertFalse("Expected MORNING section to be collapsed after second toggle", viewModel.expandedSections.value.contains("MORNING"))
    }

    @Test
    fun `test multiple sections can be expanded`() = runTest {
        viewModel.toggleSection("MORNING")
        viewModel.toggleSection("WAKEUP")
        
        assertTrue(viewModel.expandedSections.value.contains("MORNING"))
        assertTrue(viewModel.expandedSections.value.contains("WAKEUP"))
    }
}

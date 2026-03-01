package com.elroi.alarmpal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elroi.alarmpal.domain.manager.SettingsManager
import com.elroi.alarmpal.domain.manager.AlarmDefaults
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import java.util.Locale
import javax.inject.Inject
import com.elroi.alarmpal.domain.manager.GeminiManager
import com.elroi.alarmpal.domain.manager.GeminiNanoStatus
import android.content.ClipboardManager
import android.content.Context

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val geminiManager: GeminiManager,
    private val briefingGenerator: com.elroi.alarmpal.domain.generator.BriefingGenerator,
    private val localLLMManager: com.elroi.alarmpal.domain.manager.LocalLLMManager
) : ViewModel() {

    private val _draftLocation = MutableStateFlow("")
    val location = _draftLocation.asStateFlow()

    private val _draftIsCelsius = MutableStateFlow(true)
    val isCelsius = _draftIsCelsius.asStateFlow()

    private val _draftIsAutoLocation = MutableStateFlow(false)
    val isAutoLocation = _draftIsAutoLocation.asStateFlow()

    private val _draftAlarmDefaults = MutableStateFlow(AlarmDefaults())
    val alarmDefaults = _draftAlarmDefaults.asStateFlow()

    private val _draftGeminiApiKey = MutableStateFlow("")
    val geminiApiKey = _draftGeminiApiKey.asStateFlow()

    private val _draftIsCloudAiEnabled = MutableStateFlow(false)
    val isCloudAiEnabled = _draftIsCloudAiEnabled.asStateFlow()

    private val _draftPreferredAiTier = MutableStateFlow("STANDARD")
    val preferredAiTier = _draftPreferredAiTier.asStateFlow()

    private val _isAdvancedAiSupported = MutableStateFlow(GeminiNanoStatus.CHECKING)
    val isAdvancedAiSupported = _isAdvancedAiSupported.asStateFlow()

    private val _draftAiFallbackOrder = MutableStateFlow("CLOUD_THEN_LOCAL")
    val aiFallbackOrder = _draftAiFallbackOrder.asStateFlow()

    private val _isKeyValidating = MutableStateFlow(false)
    val isKeyValidating = _isKeyValidating.asStateFlow()

    private val _keyValidationResult = MutableStateFlow<Boolean?>(null)
    val keyValidationResult = _keyValidationResult.asStateFlow()

    private val _keyValidationError = MutableStateFlow<String?>(null)
    val keyValidationError = _keyValidationError.asStateFlow()

    private val _detectedClipboardKey = MutableStateFlow<String?>(null)
    val detectedClipboardKey = _detectedClipboardKey.asStateFlow()

    private val _isBriefingGenerating = MutableStateFlow(false)
    val isBriefingGenerating = _isBriefingGenerating.asStateFlow()

    val briefingStatus = settingsManager.lastGenStatusFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "waiting:pending")
    val briefingError = settingsManager.lastGenErrorFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val lastBriefingScript = settingsManager.lastBriefingScriptFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Original values to detect changes
    private val _originalLocation = MutableStateFlow("")
    private val _originalIsCelsius = MutableStateFlow(true)
    private val _originalIsAutoLocation = MutableStateFlow(false)
    private val _originalAlarmDefaults = MutableStateFlow(AlarmDefaults())
    private val _originalGeminiApiKey = MutableStateFlow("")
    private val _originalIsCloudAiEnabled = MutableStateFlow(false)
    private val _originalPreferredAiTier = MutableStateFlow("STANDARD")
    private val _originalAiFallbackOrder = MutableStateFlow("CLOUD_THEN_LOCAL")

    val hasChanges: StateFlow<Boolean> = combine(
        listOf(
            _draftLocation, _originalLocation,
            _draftIsCelsius, _originalIsCelsius,
            _draftIsAutoLocation, _originalIsAutoLocation,
            _draftAlarmDefaults, _originalAlarmDefaults,
            _draftGeminiApiKey, _originalGeminiApiKey,
            _draftIsCloudAiEnabled, _originalIsCloudAiEnabled,
            _draftPreferredAiTier, _originalPreferredAiTier,
            _draftAiFallbackOrder, _originalAiFallbackOrder
        )
    ) { args ->
        val loc = args[0] as String
        val oLoc = args[1] as String
        val cel = args[2] as Boolean
        val oCel = args[3] as Boolean
        val auto = args[4] as Boolean
        val oAuto = args[5] as Boolean
        val def = args[6] as AlarmDefaults
        val oDef = args[7] as AlarmDefaults
        val gemini = args[8] as String
        val oGemini = args[9] as String
        val cloud = args[10] as Boolean
        val oCloud = args[11] as Boolean
        val tier = args[12] as String
        val oTier = args[13] as String
        val order = args[14] as String
        val oOrder = args[15] as String
        loc != oLoc || cel != oCel || auto != oAuto || def != oDef || gemini != oGemini || cloud != oCloud || tier != oTier || order != oOrder
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        // Initialize original values from flow, but only update drafts once to avoid race conditions
        // where a DataStore write triggers an emission that overwrites the user's manual changes
        // while they are interacting with the UI.
        viewModelScope.launch {
            settingsManager.locationFlow.collectLatest { valValue ->
                _originalLocation.value = valValue
                if (_draftLocation.value == "") _draftLocation.value = valValue
            }
        }
        viewModelScope.launch {
            settingsManager.isCelsiusFlow.collectLatest { valValue ->
                val prevOriginal = _originalIsCelsius.value
                _originalIsCelsius.value = valValue
                // Only update draft if it hasn't been touched by user (draft matches old original)
                if (_draftIsCelsius.value == prevOriginal) {
                    _draftIsCelsius.value = valValue
                }
            }
        }
        viewModelScope.launch {
            settingsManager.isAutoLocationFlow.collectLatest { valValue ->
                val prevOriginal = _originalIsAutoLocation.value
                _originalIsAutoLocation.value = valValue
                if (_draftIsAutoLocation.value == prevOriginal) {
                    _draftIsAutoLocation.value = valValue
                }
            }
        }
        viewModelScope.launch {
            settingsManager.alarmDefaultsFlow.collectLatest { valValue ->
                val prevOriginal = _originalAlarmDefaults.value
                _originalAlarmDefaults.value = valValue
                if (_draftAlarmDefaults.value == prevOriginal) {
                    _draftAlarmDefaults.value = valValue
                }
            }
        }
        viewModelScope.launch {
            settingsManager.geminiApiKeyFlow.collectLatest { valValue ->
                val prevOriginal = _originalGeminiApiKey.value
                _originalGeminiApiKey.value = valValue
                if (_draftGeminiApiKey.value == prevOriginal) {
                    _draftGeminiApiKey.value = valValue
                }
            }
        }
        viewModelScope.launch {
            settingsManager.isCloudAiEnabledFlow.collectLatest { valValue ->
                val prevOriginal = _originalIsCloudAiEnabled.value
                _originalIsCloudAiEnabled.value = valValue
                if (_draftIsCloudAiEnabled.value == prevOriginal) {
                    _draftIsCloudAiEnabled.value = valValue
                }
            }
        }
        viewModelScope.launch {
            settingsManager.preferredAiTierFlow.collectLatest { valValue ->
                val prevOriginal = _originalPreferredAiTier.value
                _originalPreferredAiTier.value = valValue
                if (_draftPreferredAiTier.value == prevOriginal) {
                    _draftPreferredAiTier.value = valValue
                }
            }
        }
        viewModelScope.launch {
            settingsManager.aiFallbackOrderFlow.collectLatest { valValue ->
                val prevOriginal = _originalAiFallbackOrder.value
                _originalAiFallbackOrder.value = valValue
                if (_draftAiFallbackOrder.value == prevOriginal) {
                    _draftAiFallbackOrder.value = valValue
                }
            }
        }
        
        // Hardware support check for Gemini Nano — use lightweight file check only.
        // DO NOT call localLLMManager.checkStatus() here: it loads the native Gemma model
        // into memory which crashes (SIGABRT) when concurrent API calls touch it.
        viewModelScope.launch {
            val modelExists = localLLMManager.isModelDownloaded()
            _isAdvancedAiSupported.value = if (modelExists) {
                GeminiNanoStatus.SUPPORTED
            } else {
                GeminiNanoStatus.DOWNLOAD_REQUIRED
            }
        }
    }

    fun updateLocation(newLocation: String) {
        _draftLocation.value = newLocation
    }

    fun updateIsCelsius(celsius: Boolean) {
        _draftIsCelsius.value = celsius
    }

    fun updateIsAutoLocation(isAuto: Boolean, context: android.content.Context? = null) {
        _draftIsAutoLocation.value = isAuto
        if (isAuto && context != null) {
            viewModelScope.launch {
                fetchAndSaveCurrentLocation(context)
            }
        }
    }

    fun updateAlarmDefaults(defaults: AlarmDefaults) {
        _draftAlarmDefaults.value = defaults
    }

    fun updateGeminiApiKey(apiKey: String) {
        _draftGeminiApiKey.value = apiKey
        _keyValidationResult.value = null
        _keyValidationError.value = null
        // Persist immediately so the briefing generator always reads the latest key,
        // even if the user navigates away without tapping Save.
        viewModelScope.launch {
            settingsManager.saveGeminiApiKey(apiKey.trim())
            _originalGeminiApiKey.value = apiKey
        }
    }

    fun updateIsCloudAiEnabled(isEnabled: Boolean) {
        _draftIsCloudAiEnabled.value = isEnabled
        // Persist immediately — this is a live toggle, not a form field.
        // BriefingGenerator reads from DataStore directly; it must see this immediately.
        viewModelScope.launch {
            settingsManager.saveIsCloudAiEnabled(isEnabled)
            _originalIsCloudAiEnabled.value = isEnabled
        }
    }

    fun updatePreferredAiTier(tier: String) {
        _draftPreferredAiTier.value = tier
    }

    fun updateAiFallbackOrder(order: String) {
        _draftAiFallbackOrder.value = order
    }

    fun validateApiKey() {
        val key = _draftGeminiApiKey.value.trim()
        if (key.isBlank()) return

        viewModelScope.launch {
            _isKeyValidating.value = true
            val error = geminiManager.testApiKey(key)
            _keyValidationResult.value = error == null
            _keyValidationError.value = error
            _isKeyValidating.value = false
        }
    }

    fun checkClipboardForKey(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val text = clipData.getItemAt(0).text?.toString()?.trim() ?: ""
            // Gemini API keys are usually 39 characters and start with AIza
            // Example: AIzaSyD-m... (39 chars)
            val keyRegex = Regex("^AIza[0-9A-Za-z_-]{35,}$")
            if (keyRegex.matches(text) && text != _draftGeminiApiKey.value) {
                _detectedClipboardKey.value = text
            }
        }
    }

    fun useDetectedKey() {
        _detectedClipboardKey.value?.let {
            updateGeminiApiKey(it)
            _detectedClipboardKey.value = null
            validateApiKey()
        }
    }

    fun dismissDetectedKey() {
        _detectedClipboardKey.value = null
    }

    fun launchTestBriefing() {
        viewModelScope.launch {
            _isBriefingGenerating.value = true
            // Save draft settings so the test briefing reflects the current UI state immediately
            performSaveSettings() 
            briefingGenerator.refreshBriefing()
            _isBriefingGenerating.value = false
        }
    }

    fun triggerLocalModelDownload() {
        viewModelScope.launch {
            _isAdvancedAiSupported.value = GeminiNanoStatus.DOWNLOADING
            val success = localLLMManager.downloadModel()
            if (success) {
                // Double check status after download
                _isAdvancedAiSupported.value = localLLMManager.checkStatus()
            } else {
                _isAdvancedAiSupported.value = GeminiNanoStatus.DOWNLOAD_REQUIRED
            }
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            performSaveSettings()
        }
    }

    private suspend fun performSaveSettings() {
        val loc = _draftLocation.value
        val cel = _draftIsCelsius.value
        val auto = _draftIsAutoLocation.value
        val def = _draftAlarmDefaults.value

        settingsManager.saveLocation(loc)
        settingsManager.saveIsCelsius(cel)
        settingsManager.saveIsAutoLocation(auto)
        settingsManager.saveAlarmDefaults(def)
        settingsManager.saveGeminiApiKey(_draftGeminiApiKey.value.trim())
        settingsManager.saveIsCloudAiEnabled(_draftIsCloudAiEnabled.value)
        settingsManager.savePreferredAiTier(_draftPreferredAiTier.value)
        settingsManager.saveAiFallbackOrder(_draftAiFallbackOrder.value)

        // Update original values to reset dirty state
        _originalLocation.value = loc
        _originalIsCelsius.value = cel
        _originalIsAutoLocation.value = auto
        _originalAlarmDefaults.value = def
        _originalGeminiApiKey.value = _draftGeminiApiKey.value
        _originalIsCloudAiEnabled.value = _draftIsCloudAiEnabled.value
        _originalPreferredAiTier.value = _draftPreferredAiTier.value
        _originalAiFallbackOrder.value = _draftAiFallbackOrder.value
    }

    private suspend fun fetchAndSaveCurrentLocation(context: android.content.Context) = withContext(Dispatchers.IO) {
        try {
            val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
            val hasPermission = context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return@withContext

            val lastKnownLocation = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                ?: locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)

            if (lastKnownLocation != null) {
                val geocoder = android.location.Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lastKnownLocation.latitude, lastKnownLocation.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val city = addr.locality ?: addr.subAdminArea
                    if (city != null) {
                        _draftLocation.value = city
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore failure, falls back to whatever was manually typed or Default.
        }
    }

    fun wipeBrainMemory() {
        viewModelScope.launch {
            settingsManager.saveBriefingCache(
                script = "",
                timestamp = 0L,
                weather = null,
                fact = null,
                status = "waiting:pending",
                error = null
            )
        }
    }
}

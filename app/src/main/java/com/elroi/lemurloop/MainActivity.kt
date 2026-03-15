package com.elroi.lemurloop

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.LocaleListCompat
import com.elroi.lemurloop.domain.manager.SettingsManager
import com.elroi.lemurloop.ui.theme.LemurLoopTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import com.elroi.lemurloop.util.debugLog
import java.util.Locale
import javax.inject.Inject

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MainActivityEntryPoint {
    fun settingsManager(): SettingsManager
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsManager: SettingsManager

    /**
     * When set in attachBaseContext, getResources() returns resources with this configuration
     * so that stringResource() and layout direction use the selected app language.
     */
    private var cachedLocaleConfig: Configuration? = null

    /** Log getResources() override only first few times to confirm it's used without spamming. */
    private var getResourcesCallCount = 0

    override fun attachBaseContext(newBase: Context) {
        val appCompatLocales = AppCompatDelegate.getApplicationLocales()
        val appCompatTags = if (appCompatLocales.isEmpty) "<empty>" else appCompatLocales.toLanguageTags()
        val langFromAppCompat: String? = if (appCompatLocales.isEmpty) null else appCompatLocales[0]?.language

        debugLog(
            newBase.applicationContext,
            "MainActivity",
            "attachBaseContext_start",
            mapOf(
                "appCompatLocalesToTags" to appCompatTags,
                "langFromAppCompat" to (langFromAppCompat ?: "<null>")
            )
        )

        val (base, config) = runCatching {
            val langRaw = langFromAppCompat ?: runBlocking(Dispatchers.IO) {
                EntryPointAccessors.fromApplication(
                    newBase.applicationContext,
                    MainActivityEntryPoint::class.java
                ).settingsManager().appLanguageFlow.first()
            }
            // AppCompat/Android can return "iw" (legacy) for Hebrew; we store and use "he". Normalize so we apply locale.
            val lang = if (langRaw == "iw") "he" else langRaw
            debugLog(
                newBase.applicationContext,
                "MainActivity",
                "attachBaseContext",
                mapOf(
                    "langFromAppCompat" to (langFromAppCompat ?: "<null>"),
                    "langUsed" to lang,
                    "willApplyLocale" to (lang in listOf("he", "en")).toString()
                )
            )
            when (lang) {
                "he", "en" -> {
                    val locale = Locale.forLanguageTag(lang)
                    Locale.setDefault(locale)
                    val appResources = newBase.applicationContext.resources
                    val config = Configuration(appResources.configuration).apply {
                        setLocale(locale)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            setLocales(android.os.LocaleList(locale))
                        }
                    }
                    @Suppress("DEPRECATION")
                    appResources.updateConfiguration(config, appResources.displayMetrics)
                    Pair(newBase.createConfigurationContext(config), config)
                }
                else -> Pair(newBase, null)
            }
        }.getOrElse { e ->
            debugLog(
                newBase.applicationContext,
                "MainActivity",
                "attachBaseContext_getOrElse",
                mapOf("exception" to (e.message ?: e.toString()), "cause" to (e.cause?.message ?: ""))
            )
            Pair(newBase, null)
        }
        cachedLocaleConfig = config
        debugLog(
            newBase.applicationContext,
            "MainActivity",
            "attachBaseContext_done",
            mapOf("cachedLocaleConfigSet" to (config != null).toString())
        )
        super.attachBaseContext(base)
    }

    override fun getResources(): Resources {
        return if (cachedLocaleConfig != null) {
            val res = applicationContext.createConfigurationContext(cachedLocaleConfig!!).resources
            if (getResourcesCallCount < 5) {
                getResourcesCallCount++
                debugLog(
                    applicationContext,
                    "MainActivity",
                    "getResources_override",
                    mapOf(
                        "callCount" to getResourcesCallCount.toString(),
                        "sampleString" to res.getString(R.string.settings_language),
                        "configurationLocales" to res.configuration.locales.toString()
                    )
                )
            }
            res
        } else {
            super.getResources()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }

        super.onCreate(savedInstanceState)

        debugLog(
            this,
            "MainActivity",
            "onCreate",
            mapOf(
                "pid" to android.os.Process.myPid().toString(),
                "configurationLocales" to resources.configuration.locales.toString(),
                "getString(settings_language)" to getString(R.string.settings_language),
                "cachedLocaleConfigSet" to (cachedLocaleConfig != null).toString()
            )
        )

        // Artificial delay for branding
        lifecycleScope.launch {
            delay(1500)
            keepSplash = false
        }

        // Do not provide LocalContext with a wrapped context: Hilt's hiltViewModel() requires
        // an Activity context. Provide LocalConfiguration so layout/RTL use the correct config.
        // Activity.getResources() is overridden to return localized resources for stringResource().
        setContent {
            CompositionLocalProvider(
                LocalConfiguration provides resources.configuration
            ) {
                LemurLoopTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        com.elroi.lemurloop.ui.MainScreen()
                    }
                }
            }
        }
    }

    private fun applySavedAppLanguage() {
        runBlocking {
            try {
                val lang = withContext(Dispatchers.IO) {
                    settingsManager.appLanguageFlow.first()
                }
                val tag = when (lang) {
                    "he" -> "he"
                    "en" -> "en"
                    else -> ""
                }
                // #region agent log
                debugLog(this@MainActivity, "C", "applySavedAppLanguage", mapOf("lang" to lang, "tag" to tag))
                // #endregion
                AppCompatDelegate.setApplicationLocales(
                    if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
                )
            } catch (_: Exception) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            }
        }
    }
}

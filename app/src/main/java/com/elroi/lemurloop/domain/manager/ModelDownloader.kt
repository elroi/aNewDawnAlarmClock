package com.elroi.lemurloop.domain.manager

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Repository URL for Gemma 2B optimized for CPU
    private val MODEL_URL = "https://huggingface.co/google/gemma-2b-it-tflite/resolve/main/gemma-2b-it-cpu-int4.bin" 
    private val MODEL_FILE_NAME = "gemma-2b-it-cpu-int4.bin"

    init {
        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0 // Force fetch every time for debugging
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
    }

    val modelFile: File
        get() = File(context.getExternalFilesDir(null), MODEL_FILE_NAME)

    suspend fun downloadModel(onProgress: (Int) -> Unit): Boolean = withContext(Dispatchers.IO) {
        if (modelFile.exists()) {
            return@withContext true
        }

        // Fetch the secure token from Firebase
        val remoteConfig = Firebase.remoteConfig
        var hfToken = ""
        try {
            val activated = remoteConfig.fetchAndActivate().await()
            hfToken = remoteConfig.getString("hf_model_download_token")
            android.util.Log.d("ModelDownloader", "Firebase fetch activated=$activated, token length=${hfToken.length}")
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("ModelDownloader", "Firebase fetch error: ${e.message}")
        }

        val finalUrl = resolveRedirectsSafely(MODEL_URL, hfToken) ?: MODEL_URL

        val request = DownloadManager.Request(Uri.parse(finalUrl))
            .setTitle("Downloading On-Device AI Model")
            .setDescription("Fetching Gemma 2B for local processing")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            // Save to internal app-specific external storage.
            .setDestinationInExternalFilesDir(context, null, MODEL_FILE_NAME)
            .setAllowedOverMetered(false) // Wait for Wi-Fi

        // We still inject the token just in case it's a direct download that doesn't redirect
        if (hfToken.isNotEmpty()) {
            request.addRequestHeader("Authorization", "Bearer $hfToken")
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        var isFinished = false
        var success = false

        while (!isFinished) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)

            if (cursor != null && cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if (statusIndex >= 0) {
                    when (cursor.getInt(statusIndex)) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            isFinished = true
                            success = true
                            onProgress(100)
                        }
                        DownloadManager.STATUS_FAILED -> {
                            isFinished = true
                            success = false
                            
                            val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                            if (reasonIndex >= 0) {
                                val reason = cursor.getInt(reasonIndex)
                                android.util.Log.e("ModelDownloader", "Download failed with reason: $reason")
                            } else {
                                android.util.Log.e("ModelDownloader", "Download failed with unknown reason")
                            }
                        }
                        DownloadManager.STATUS_RUNNING -> {
                            val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                            
                            if (bytesDownloadedIndex >= 0 && bytesTotalIndex >= 0) {
                                val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
                                val bytesTotal = cursor.getLong(bytesTotalIndex)
                                if (bytesTotal > 0) {
                                    val progress = ((bytesDownloaded * 100) / bytesTotal).toInt()
                                    onProgress(progress)
                                }
                            }
                        }
                    }
                }
                cursor.close()
            }
            // Check progress periodically
            delay(500)
        }

        return@withContext success
    }

    /**
     * Android's DownloadManager strips custom HTTP headers (like Authorization) when following
     * a cross-domain redirect (e.g., from huggingface.co to their CloudFront CDN). 
     * This function manually follows the redirect to get the final, pre-signed CDN URL 
     * which DownloadManager can then fetch without needing the Authorization header.
     */
    private fun resolveRedirectsSafely(initialUrl: String, token: String): String? {
        if (token.isEmpty()) return null
        
        var connection: HttpURLConnection? = null
        try {
            val url = URL(initialUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false // We want to capture the 302
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || 
                responseCode == HttpURLConnection.HTTP_MOVED_PERM || 
                responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                responseCode == 307) {
                
                val redirectedUrl = connection.getHeaderField("Location")
                if (redirectedUrl != null) {
                    android.util.Log.d("ModelDownloader", "Resolved final CDN URL")
                    return redirectedUrl
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("ModelDownloader", "Failed to resolve redirect: ${e.message}")
        } finally {
            connection?.disconnect()
        }
        return null
    }
}

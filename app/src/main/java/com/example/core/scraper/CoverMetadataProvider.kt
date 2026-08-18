package com.example.core.scraper

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class CoverMetadataProvider(private val context: Context) {
    private val TAG = "CoverMetadataProvider"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val coversDir = File(context.filesDir, "covers").apply {
        if (!exists()) mkdirs()
    }

    suspend fun getOrFetchCover(titleId: String, titleName: String): String? = withContext(Dispatchers.IO) {
        if (titleId.isBlank()) return@withContext null
        val cleanId = titleId.trim().uppercase()
        val localFile = File(coversDir, "$cleanId.jpg")

        if (localFile.exists() && localFile.length() > 0) {
            return@withContext localFile.absolutePath
        }

        // List of reliable public Xbox 360 Title Art endpoints
        val lowerId = cleanId.lowercase()
        val candidateUrls = listOf(
            "http://download.xbox.com/content/images/66acd000-77fe-1000-9115-d802$lowerId/1033/boxartlg.jpg",
            "http://download.xbox.com/content/images/66acd000-77fe-1000-9115-d802$lowerId/1033/tile.png",
            "https://cdn.xboxunity.net/cover.php?titleid=$cleanId",
            "https://raw.githubusercontent.com/xenia-canary/game-compatibility/master/covers/$cleanId.jpg"
        )

        for (url in candidateUrls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Xenia-Android/1.0 (Android ARM64; MediaTek Optimized)")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body
                        if (body != null) {
                            val bytes = body.bytes()
                            if (bytes.size > 1024) { // Valid image payload
                                FileOutputStream(localFile).use { out ->
                                    out.write(bytes)
                                }
                                Log.i(TAG, "Successfully cached cover for $titleName ($cleanId) from $url")
                                return@withContext localFile.absolutePath
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Try next endpoint gracefully without crashing
                Log.d(TAG, "Endpoint $url skipped: ${e.message}")
            }
        }

        return@withContext null
    }

    fun saveCustomCover(titleId: String, inputStream: java.io.InputStream): String? {
        return try {
            val cleanId = titleId.trim().uppercase()
            val customFile = File(coversDir, "${cleanId}_custom.jpg")
            FileOutputStream(customFile).use { out ->
                inputStream.copyTo(out)
            }
            customFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save custom cover: ${e.message}")
            null
        }
    }
}

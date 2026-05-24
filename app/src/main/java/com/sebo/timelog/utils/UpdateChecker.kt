package com.sebo.timelog.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ReleaseInfo(
    val tagName: String,       // z. B. "v1.34.0"
    val version: String,       // z. B. "1.34.0"
    val releaseNotes: String,
    val apkDownloadUrl: String?,
    val releasePage: String
)

object UpdateChecker {

    private const val GITHUB_RELEASES_API =
        "https://api.github.com/repos/SEBO-1337/TimeLog/releases/latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Holt die neueste Release-Information von GitHub.
     * Gibt null zurück, wenn kein Release gefunden wurde oder ein Fehler aufgetreten ist.
     */
    suspend fun fetchLatestRelease(): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_RELEASES_API)
                .header("Accept", "application/vnd.github+json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)

            val tagName = json.optString("tag_name", "")
            val version = tagName.trimStart('v')
            val releaseNotes = json.optString("body", "").ifBlank { "Keine Release-Notes verfügbar." }
            val releasePage = json.optString("html_url", "https://github.com/SEBO-1337/TimeLog/releases")

            // APK-Asset suchen
            val assets = json.optJSONArray("assets")
            var apkUrl: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url").ifBlank { null }
                        break
                    }
                }
            }

            ReleaseInfo(
                tagName = tagName,
                version = version,
                releaseNotes = releaseNotes,
                apkDownloadUrl = apkUrl,
                releasePage = releasePage
            )
        } catch (ex: Exception) {
            android.util.Log.w("UpdateChecker", "Fehler beim Abrufen der Release-Info: ${ex.message}")
            null
        }
    }

    /**
     * Vergleicht zwei Versions-Strings (semantische Versionierung: major.minor.patch).
     * Gibt true zurück, wenn [latest] neuer als [current] ist.
     */
    fun isNewer(current: String, latest: String): Boolean {
        return try {
            val cur = parseVersion(current)
            val lat = parseVersion(latest)
            lat > cur
        } catch (e: Exception) {
            false
        }
    }

    private fun parseVersion(version: String): Triple<Int, Int, Int> {
        val parts = version.trimStart('v').split(".").map { it.toIntOrNull() ?: 0 }
        return Triple(
            parts.getOrElse(0) { 0 },
            parts.getOrElse(1) { 0 },
            parts.getOrElse(2) { 0 }
        )
    }

    private operator fun Triple<Int, Int, Int>.compareTo(other: Triple<Int, Int, Int>): Int {
        if (first != other.first) return first.compareTo(other.first)
        if (second != other.second) return second.compareTo(other.second)
        return third.compareTo(other.third)
    }
}



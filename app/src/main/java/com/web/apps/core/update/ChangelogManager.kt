package com.web.apps.core.update

import com.web.apps.BuildConfig
import com.web.apps.core.preferences.ChangelogPreferenceManager
import com.web.apps.data.remote.model.GitHubReleaseModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

sealed class ChangelogResult {
    data class Available(val versionName: String, val notes: String) : ChangelogResult()
    object NotAvailable : ChangelogResult()
}

@Singleton
class ChangelogManager @Inject constructor(
    private val changelogPreferenceManager: ChangelogPreferenceManager
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val GITHUB_API_URL = "https://api.github.com/repos/hastagaming/webapps/releases/latest"
    }

    suspend fun checkForChangelog(): ChangelogResult = withContext(Dispatchers.IO) {
        try {
            val currentVersion = BuildConfig.VERSION_NAME
            val lastSeen = changelogPreferenceManager.getLastSeenVersionBlocking()

            if (lastSeen == currentVersion || lastSeen.isBlank()) {
                changelogPreferenceManager.setLastSeenVersion(currentVersion)
                return@withContext ChangelogResult.NotAvailable
            }

            val connection = URL(GITHUB_API_URL).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext ChangelogResult.NotAvailable
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val release = json.decodeFromString<GitHubReleaseModel>(body)

            ChangelogResult.Available(currentVersion, release.releaseNotes)
        } catch (e: Exception) {
            ChangelogResult.NotAvailable
        }
    }

    suspend fun markAsSeen() {
        changelogPreferenceManager.setLastSeenVersion(BuildConfig.VERSION_NAME)
    }
}
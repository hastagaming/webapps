package com.web.apps.core.update

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import com.web.apps.BuildConfig
import com.web.apps.data.remote.model.GitHubReleaseModel
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

sealed class UpdateCheckResult {
    data class UpdateAvailable(
        val versionName: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val fileSize: Long
    ) : UpdateCheckResult()
    object UpToDate : UpdateCheckResult()
    data class Failure(val message: String) : UpdateCheckResult()
}

sealed class DownloadProgress {
    data class InProgress(val percent: Int) : DownloadProgress()
    data class Completed(val file: File) : DownloadProgress()
    data class Failed(val message: String) : DownloadProgress()
}

@Singleton
class UpdateManager @Inject constructor() {

    companion object {
        private const val GITHUB_API_URL = "https://api.github.com/repos/hastagaming/webapps/releases/latest"
        private const val APK_ASSET_SUFFIX = ".apk"
        private const val CONNECT_TIMEOUT_MS = 15000
        private const val READ_TIMEOUT_MS = 15000
        private const val BUFFER_SIZE = 8192
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdate(): UpdateCheckResult {
        return try {
            val connection = URL(GITHUB_API_URL).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return UpdateCheckResult.Failure("Could not reach the update server (code ${connection.responseCode}).")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val release = json.decodeFromString<GitHubReleaseModel>(body)

            val remoteVersionName = release.tagName.removePrefix("v")
            val localVersionName = BuildConfig.VERSION_NAME

            if (isRemoteNewer(remoteVersionName, localVersionName)) {
                val apkAsset = release.assets.firstOrNull { it.name.endsWith(APK_ASSET_SUFFIX) }
                    ?: return UpdateCheckResult.Failure("No APK file found in the latest release.")

                UpdateCheckResult.UpdateAvailable(
                    versionName = remoteVersionName,
                    releaseNotes = release.releaseNotes,
                    downloadUrl = apkAsset.downloadUrl,
                    fileSize = apkAsset.size
                )
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (e: Exception) {
            UpdateCheckResult.Failure(e.message ?: "Failed to check for updates.")
        }
    }

    private fun isRemoteNewer(remote: String, local: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = local.split(".").mapNotNull { it.toIntOrNull() }
        val maxLength = maxOf(remoteParts.size, localParts.size)

        for (i in 0 until maxLength) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r != l) return r > l
        }
        return false
    }

    fun downloadUpdate(
        context: Context,
        downloadUrl: String,
        onProgress: (DownloadProgress) -> Unit
    ) {
        try {
            val connection = URL(downloadUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                onProgress(DownloadProgress.Failed("Download failed with code ${connection.responseCode}."))
                return
            }

            val totalBytes = connection.contentLengthLong
            val outputFile = File(context.getExternalFilesDir(null), "webapps-update.apk")
            if (outputFile.exists()) outputFile.delete()

            var bytesDownloaded = 0L
            connection.inputStream.use { input ->
                outputFile.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead
                        val percent = if (totalBytes > 0) {
                            ((bytesDownloaded * 100) / totalBytes).toInt()
                        } else {
                            0
                        }
                        onProgress(DownloadProgress.InProgress(percent))
                    }
                }
            }

            onProgress(DownloadProgress.Completed(outputFile))
        } catch (e: Exception) {
            onProgress(DownloadProgress.Failed(e.message ?: "Download failed."))
        }
    }

    fun installUpdate(context: Context, apkFile: File) {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(installIntent)
    }

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }
}
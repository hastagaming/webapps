package com.web.apps.core.plugin

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.akuleshov7.ktoml.Toml
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed class PluginResult<out T> {
    data class Success<T>(val data: T) : PluginResult<T>()
    data class Failure(val message: String) : PluginResult<Nothing>()
}

private const val CATALOG_URL = "https://raw.githubusercontent.com/hastagaming/WebApps-plugin/main/index.json"

@Singleton
class PluginManager @Inject constructor() {

    private val toml = Toml

    suspend fun fetchCatalog(): PluginResult<List<PluginCatalogEntry>> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(CATALOG_URL).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext PluginResult.Failure("Could not reach plugin repository (code ${connection.responseCode}).")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val catalog = toml.decodeFromString(PluginCatalog.serializer(), body)
            PluginResult.Success(catalog.plugins)
        } catch (e: Exception) {
            PluginResult.Failure(e.message ?: "Failed to load plugin catalog.")
        }
    }

    suspend fun downloadPlugin(context: Context, entry: PluginCatalogEntry): PluginResult<File> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(entry.downloadUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext PluginResult.Failure("Download failed (code ${connection.responseCode}).")
            }

            val pluginsDir = File(context.filesDir, "plugins")
            if (!pluginsDir.exists()) pluginsDir.mkdirs()

            val outputFile = File(pluginsDir, "${entry.id}.wp")
            connection.inputStream.use { input ->
                outputFile.outputStream().use { output -> input.copyTo(output) }
            }

            PluginResult.Success(outputFile)
        } catch (e: Exception) {
            PluginResult.Failure(e.message ?: "Failed to download plugin.")
        }
    }

    suspend fun extractManifest(wpFile: File): PluginResult<PluginManifest> = withContext(Dispatchers.IO) {
        try {
            ZipInputStream(wpFile.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "manifest.toml") {
                        val tomlString = zip.bufferedReader().readText()
                        val manifest = toml.decodeFromString(PluginManifest.serializer(), tomlString)
                        return@withContext PluginResult.Success(manifest)
                    }
                    entry = zip.nextEntry
                }
            }
            PluginResult.Failure("manifest.toml not found inside the .wp file.")
        } catch (e: Exception) {
            PluginResult.Failure(e.message ?: "Failed to read plugin file.")
        }
    }

    fun isPluginDownloaded(context: Context, pluginId: String): Boolean {
        val file = File(File(context.filesDir, "plugins"), "$pluginId.wp")
        return file.exists()
    }
}
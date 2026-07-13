package com.web.apps.core.plugin

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

private const val LIST_URL = "https://raw.githubusercontent.com/hastagaming/WebApps-plugin/main/plugins-list.txt"
private const val CATALOG_BASE_URL = "https://raw.githubusercontent.com/hastagaming/WebApps-plugin/main/catalog"
private const val INDEX_URL = "https://raw.githubusercontent.com/hastagaming/WebApps-plugin/main/index.toml"
private const val PLUGIN_BASE_URL = "https://raw.githubusercontent.com/hastagaming/WebApps-plugin/main/plugins"

@Singleton
class PluginManager @Inject constructor() {

    private val KNOWN_PLUGIN_TYPES = listOf("theme", "ui")

    suspend fun fetchCatalog(): PluginResult<List<PluginCatalogEntry>> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(INDEX_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext PluginResult.Failure("Could not reach plugin repository (code ${connection.responseCode}).")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }

            val entries = mutableListOf<PluginCatalogEntry>()

            for (type in KNOWN_PLUGIN_TYPES) {
                val typeEntries = SimpleTomlParser.parseInlineTableArray(body, type)
                typeEntries.forEach { fields ->
                    val id = fields["id"] ?: return@forEach
                    entries.add(
                        PluginCatalogEntry(
                            id = id,
                            name = fields["name"] ?: id,
                            description = fields["description"] ?: "",
                            author = fields["author"] ?: "",
                            version = fields["version"] ?: "1.0.0",
                            type = type,
                            downloadUrl = "$PLUGIN_BASE_URL/$id.wp",
                            previewColorHex = fields["previewColorHex"]
                        )
                    )
                }
            }

            PluginResult.Success(entries)
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
                        val sections = SimpleTomlParser.parseSectioned(tomlString)
                        val root = sections[""] ?: emptyMap()
                        val colorsSection = sections["colors"] ?: emptyMap()
                        val uiTweaksSection = sections["uiTweaks"] ?: emptyMap()

                        val pluginType = root["type"] ?: "theme"

                        val colors = if (colorsSection.isNotEmpty()) {
                            PluginColors(
                                primary = colorsSection["primary"] ?: "#90CAF9",
                                background = colorsSection["background"] ?: "#121212",
                                surface = colorsSection["surface"] ?: "#1E1E1E",
                                surfaceVariant = colorsSection["surfaceVariant"] ?: "#2D2D2D",
                                onSurface = colorsSection["onSurface"] ?: "#FFFFFF",
                                onPrimary = colorsSection["onPrimary"] ?: "#000000",
                                error = colorsSection["error"] ?: "#CF6679"
                            )
                        } else null

                        val uiTweaks = if (uiTweaksSection.isNotEmpty()) {
                            PluginUiTweaks(
                                cornerRadiusDp = uiTweaksSection["cornerRadiusDp"]?.toIntOrNull() ?: 12,
                                gridMinTileWidthDp = uiTweaksSection["gridMinTileWidthDp"]?.toIntOrNull() ?: 100,
                                fabPosition = uiTweaksSection["fabPosition"] ?: "end",
                                showSearchBar = uiTweaksSection["showSearchBar"]?.toBooleanStrictOrNull() ?: true,
                                showBackupButton = uiTweaksSection["showBackupButton"]?.toBooleanStrictOrNull() ?: true,
                                showSignOutButton = uiTweaksSection["showSignOutButton"]?.toBooleanStrictOrNull() ?: true,
                                showSettingsButton = uiTweaksSection["showSettingsButton"]?.toBooleanStrictOrNull() ?: true,
                                showRefreshAllButton = uiTweaksSection["showRefreshAllButton"]?.toBooleanStrictOrNull() ?: true,
                                showStopAllButton = uiTweaksSection["showStopAllButton"]?.toBooleanStrictOrNull() ?: true,
                                showPinnedSection = uiTweaksSection["showPinnedSection"]?.toBooleanStrictOrNull() ?: true,
                                itemSpacingDp = uiTweaksSection["itemSpacingDp"]?.toIntOrNull() ?: 4
                            )
                        } else null

                        val manifest = PluginManifest(
                            id = root["id"] ?: "",
                            name = root["name"] ?: "",
                            version = root["version"] ?: "1.0.0",
                            type = pluginType,
                            colors = colors,
                            uiTweaks = uiTweaks
                        )

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
package com.web.apps.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.web.apps.core.plugin.PluginColors
import com.web.apps.core.plugin.PluginManifest
import com.web.apps.core.plugin.PluginUiTweaks
import com.web.apps.core.plugin.SimpleTomlParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.pluginDataStore by preferencesDataStore(name = "plugin_preferences")

@Singleton
class PluginPreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ACTIVE_PLUGIN_KEY = stringPreferencesKey("active_plugin_manifest")

    val activePlugin: Flow<PluginManifest?> = context.pluginDataStore.data.map { prefs ->
        val raw = prefs[ACTIVE_PLUGIN_KEY] ?: return@map null
        try {
            val sections = SimpleTomlParser.parseSectioned(raw)
            val root = sections[""] ?: emptyMap()
            val colorsSection = sections["colors"] ?: emptyMap()
            val uiTweaksSection = sections["uiTweaks"] ?: emptyMap()

            PluginManifest(
                id = root["id"] ?: return@map null,
                name = root["name"] ?: "",
                version = root["version"] ?: "1.0.0",
                type = root["type"] ?: "theme",
                colors = PluginColors(
                    primary = colorsSection["primary"] ?: "#90CAF9",
                    background = colorsSection["background"] ?: "#121212",
                    surface = colorsSection["surface"] ?: "#1E1E1E",
                    surfaceVariant = colorsSection["surfaceVariant"] ?: "#2D2D2D",
                    onSurface = colorsSection["onSurface"] ?: "#FFFFFF",
                    onPrimary = colorsSection["onPrimary"] ?: "#000000",
                    error = colorsSection["error"] ?: "#CF6679"
                ),
                uiTweaks = PluginUiTweaks(
                    cornerRadiusDp = uiTweaksSection["cornerRadiusDp"]?.toIntOrNull() ?: 12,
                    gridMinTileWidthDp = uiTweaksSection["gridMinTileWidthDp"]?.toIntOrNull() ?: 100,
                    fabPosition = uiTweaksSection["fabPosition"] ?: "end",
                    showSearchBar = uiTweaksSection["showSearchBar"]?.toBooleanStrictOrNull() ?: true
                )
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun setActivePlugin(manifest: PluginManifest?) {
        context.pluginDataStore.edit { prefs ->
            if (manifest == null) {
                prefs.remove(ACTIVE_PLUGIN_KEY)
            } else {
                val encoded = buildString {
                    appendLine("id = \"${manifest.id}\"")
                    appendLine("name = \"${manifest.name}\"")
                    appendLine("version = \"${manifest.version}\"")
                    appendLine("type = \"${manifest.type}\"")
                    appendLine("[colors]")
                    appendLine("primary = \"${manifest.colors.primary}\"")
                    appendLine("background = \"${manifest.colors.background}\"")
                    appendLine("surface = \"${manifest.colors.surface}\"")
                    appendLine("surfaceVariant = \"${manifest.colors.surfaceVariant}\"")
                    appendLine("onSurface = \"${manifest.colors.onSurface}\"")
                    appendLine("onPrimary = \"${manifest.colors.onPrimary}\"")
                    appendLine("error = \"${manifest.colors.error}\"")
                    appendLine("[uiTweaks]")
                    appendLine("cornerRadiusDp = ${manifest.uiTweaks.cornerRadiusDp}")
                    appendLine("gridMinTileWidthDp = ${manifest.uiTweaks.gridMinTileWidthDp}")
                    appendLine("fabPosition = \"${manifest.uiTweaks.fabPosition}\"")
                    appendLine("showSearchBar = ${manifest.uiTweaks.showSearchBar}")
                }
                prefs[ACTIVE_PLUGIN_KEY] = encoded
            }
        }
    }
}
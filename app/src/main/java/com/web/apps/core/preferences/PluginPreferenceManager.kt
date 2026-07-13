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
    private val ACTIVE_THEME_KEY = stringPreferencesKey("active_theme_plugin")
    private val ACTIVE_UI_KEY = stringPreferencesKey("active_ui_plugin")

    val activeThemePlugin: Flow<PluginManifest?> = context.pluginDataStore.data.map { prefs ->
        prefs[ACTIVE_THEME_KEY]?.let { decode(it) }
    }

    val activeUiPlugin: Flow<PluginManifest?> = context.pluginDataStore.data.map { prefs ->
        prefs[ACTIVE_UI_KEY]?.let { decode(it) }
    }

    suspend fun setActivePlugin(manifest: PluginManifest) {
        val key = if (manifest.type == "ui") ACTIVE_UI_KEY else ACTIVE_THEME_KEY
        context.pluginDataStore.edit { prefs -> prefs[key] = encode(manifest) }
    }

    suspend fun clearActivePlugin(type: String) {
        val key = if (type == "ui") ACTIVE_UI_KEY else ACTIVE_THEME_KEY
        context.pluginDataStore.edit { prefs -> prefs.remove(key) }
    }

    suspend fun getActiveIdForType(type: String): String? {
        val key = if (type == "ui") ACTIVE_UI_KEY else ACTIVE_THEME_KEY
        val raw = context.pluginDataStore.data.map { it[key] }
        var result: String? = null
        raw.collect { encoded ->
            result = encoded?.let { decode(it)?.id }
            return@collect
        }
        return result
    }

    private fun decode(raw: String): PluginManifest? {
        return try {
            val sections = SimpleTomlParser.parseSectioned(raw)
            val root = sections[""] ?: emptyMap()
            val colorsSection = sections["colors"] ?: emptyMap()
            val uiTweaksSection = sections["uiTweaks"] ?: emptyMap()

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

            PluginManifest(
                id = root["id"] ?: return null,
                name = root["name"] ?: "",
                version = root["version"] ?: "1.0.0",
                type = root["type"] ?: "theme",
                colors = colors,
                uiTweaks = uiTweaks
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun encode(manifest: PluginManifest): String {
        return buildString {
            appendLine("id = \"${manifest.id}\"")
            appendLine("name = \"${manifest.name}\"")
            appendLine("version = \"${manifest.version}\"")
            appendLine("type = \"${manifest.type}\"")
            manifest.colors?.let { c ->
                appendLine("[colors]")
                appendLine("primary = \"${c.primary}\"")
                appendLine("background = \"${c.background}\"")
                appendLine("surface = \"${c.surface}\"")
                appendLine("surfaceVariant = \"${c.surfaceVariant}\"")
                appendLine("onSurface = \"${c.onSurface}\"")
                appendLine("onPrimary = \"${c.onPrimary}\"")
                appendLine("error = \"${c.error}\"")
            }
            manifest.uiTweaks?.let { t ->
                appendLine("[uiTweaks]")
                appendLine("cornerRadiusDp = ${t.cornerRadiusDp}")
                appendLine("gridMinTileWidthDp = ${t.gridMinTileWidthDp}")
                appendLine("fabPosition = \"${t.fabPosition}\"")
                appendLine("showSearchBar = ${t.showSearchBar}")
                appendLine("showBackupButton = ${t.showBackupButton}")
                appendLine("showSignOutButton = ${t.showSignOutButton}")
                appendLine("showSettingsButton = ${t.showSettingsButton}")
                appendLine("showRefreshAllButton = ${t.showRefreshAllButton}")
                appendLine("showStopAllButton = ${t.showStopAllButton}")
                appendLine("showPinnedSection = ${t.showPinnedSection}")
                appendLine("itemSpacingDp = ${t.itemSpacingDp}")
            }
        }
    }
}
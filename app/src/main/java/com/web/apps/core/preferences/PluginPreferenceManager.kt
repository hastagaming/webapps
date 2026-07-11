package com.web.apps.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.web.apps.core.plugin.PluginManifest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.akuleshov7.ktoml.Toml
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
            Toml.decodeFromString<PluginManifest>(raw)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun setActivePlugin(manifest: PluginManifest?) {
        context.pluginDataStore.edit { prefs ->
            if (manifest == null) {
                prefs.remove(ACTIVE_PLUGIN_KEY)
            } else {
                prefs[ACTIVE_PLUGIN_KEY] = Toml.encodeToString(manifest)
            }
        }
    }
}
package com.web.apps.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}

private val Context.themeDataStore by preferencesDataStore(name = "theme_preferences")

@Singleton
class ThemePreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val ACCENT_COLOR_KEY = stringPreferencesKey("accent_color")
    private val FONT_SCALE_KEY = intPreferencesKey("font_scale_percent")

    val themeMode: Flow<AppThemeMode> = context.themeDataStore.data.map { prefs ->
        when (prefs[THEME_MODE_KEY]) {
            "LIGHT" -> AppThemeMode.LIGHT
            "DARK" -> AppThemeMode.DARK
            else -> AppThemeMode.SYSTEM
        }
    }

    val accentColor: Flow<String?> = context.themeDataStore.data.map { prefs ->
        prefs[ACCENT_COLOR_KEY]
    }

    val fontScalePercent: Flow<Int> = context.themeDataStore.data.map { prefs ->
        prefs[FONT_SCALE_KEY] ?: 100
    }

    fun getFontScalePercentBlocking(): Int {
        return runBlocking { fontScalePercent.first() }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.name
        }
    }

    suspend fun setAccentColor(hex: String?) {
        context.themeDataStore.edit { prefs ->
            if (hex == null) {
                prefs.remove(ACCENT_COLOR_KEY)
            } else {
                prefs[ACCENT_COLOR_KEY] = hex
            }
        }
    }

    suspend fun setFontScalePercent(percent: Int) {
        context.themeDataStore.edit { prefs ->
            prefs[FONT_SCALE_KEY] = percent
        }
    }
}
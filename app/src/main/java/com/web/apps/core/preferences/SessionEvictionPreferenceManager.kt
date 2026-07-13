package com.web.apps.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.evictionDataStore by preferencesDataStore(name = "session_eviction_preferences")

@Singleton
class SessionEvictionPreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ENABLED_KEY = booleanPreferencesKey("eviction_enabled")
    private val IDLE_MINUTES_KEY = intPreferencesKey("eviction_idle_minutes")

    val isEnabled: Flow<Boolean> = context.evictionDataStore.data.map { prefs ->
        prefs[ENABLED_KEY] ?: false
    }

    val idleMinutes: Flow<Int> = context.evictionDataStore.data.map { prefs ->
        prefs[IDLE_MINUTES_KEY] ?: 30
    }

    fun isEnabledBlocking(): Boolean = runBlocking { isEnabled.first() }
    fun getIdleMinutesBlocking(): Int = runBlocking { idleMinutes.first() }

    suspend fun setEnabled(enabled: Boolean) {
        context.evictionDataStore.edit { prefs -> prefs[ENABLED_KEY] = enabled }
    }

    suspend fun setIdleMinutes(minutes: Int) {
        context.evictionDataStore.edit { prefs -> prefs[IDLE_MINUTES_KEY] = minutes }
    }
}
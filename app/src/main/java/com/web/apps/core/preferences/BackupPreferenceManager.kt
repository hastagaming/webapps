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

private val Context.backupDataStore by preferencesDataStore(name = "backup_preferences")

@Singleton
class BackupPreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val AUTO_BACKUP_ENABLED_KEY = booleanPreferencesKey("auto_backup_enabled")
    private val AUTO_BACKUP_INTERVAL_DAYS_KEY = intPreferencesKey("auto_backup_interval_days")

    val isAutoBackupEnabled: Flow<Boolean> = context.backupDataStore.data.map { prefs ->
        prefs[AUTO_BACKUP_ENABLED_KEY] ?: false
    }

    val intervalDays: Flow<Int> = context.backupDataStore.data.map { prefs ->
        prefs[AUTO_BACKUP_INTERVAL_DAYS_KEY] ?: 7
    }

    fun isAutoBackupEnabledBlocking(): Boolean = runBlocking { isAutoBackupEnabled.first() }
    fun getIntervalDaysBlocking(): Int = runBlocking { intervalDays.first() }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.backupDataStore.edit { prefs -> prefs[AUTO_BACKUP_ENABLED_KEY] = enabled }
    }

    suspend fun setIntervalDays(days: Int) {
        context.backupDataStore.edit { prefs -> prefs[AUTO_BACKUP_INTERVAL_DAYS_KEY] = days }
    }
}
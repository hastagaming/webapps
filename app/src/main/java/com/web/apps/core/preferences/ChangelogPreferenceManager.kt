package com.web.apps.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.changelogDataStore by preferencesDataStore(name = "changelog_preferences")

@Singleton
class ChangelogPreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val LAST_SEEN_VERSION_KEY = stringPreferencesKey("last_seen_version")

    fun getLastSeenVersionBlocking(): String {
        return runBlocking {
            context.changelogDataStore.data.first()[LAST_SEEN_VERSION_KEY] ?: ""
        }
    }

    suspend fun setLastSeenVersion(version: String) {
        context.changelogDataStore.edit { prefs -> prefs[LAST_SEEN_VERSION_KEY] = version }
    }
}
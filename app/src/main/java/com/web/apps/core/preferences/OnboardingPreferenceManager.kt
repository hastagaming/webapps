package com.web.apps.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_preferences")

@Singleton
class OnboardingPreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")

    fun isOnboardingCompletedBlocking(): Boolean {
        return runBlocking {
            context.onboardingDataStore.data.first()[ONBOARDING_COMPLETED_KEY] ?: false
        }
    }

    suspend fun setOnboardingCompleted() {
        context.onboardingDataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED_KEY] = true
        }
    }
}
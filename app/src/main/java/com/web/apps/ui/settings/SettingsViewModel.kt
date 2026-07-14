package com.web.apps.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.web.apps.core.preferences.AppThemeMode
import com.web.apps.core.preferences.ThemePreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: com.web.apps.data.repository.AuthRepository,
    private val sessionEvictionPreferenceManager: com.web.apps.core.preferences.SessionEvictionPreferenceManager,
    private val sessionEvictionScheduler: com.web.apps.core.container.SessionEvictionScheduler,
    private val themePreferenceManager: ThemePreferenceManager,
    private val backupPreferenceManager: com.web.apps.core.preferences.BackupPreferenceManager,
    private val backupScheduler: com.web.apps.backup.BackupScheduler,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context
) : ViewModel() {

    val themeMode: Flow<AppThemeMode> = themePreferenceManager.themeMode
    val accentColor: Flow<String?> = themePreferenceManager.accentColor
    val fontScalePercent: Flow<Int> = themePreferenceManager.fontScalePercent
    val isAutoBackupEnabled: Flow<Boolean> = backupPreferenceManager.isAutoBackupEnabled
    val autoBackupIntervalDays: Flow<Int> = backupPreferenceManager.intervalDays
    val isEvictionEnabled: Flow<Boolean> = sessionEvictionPreferenceManager.isEnabled
    val evictionIdleMinutes: Flow<Int> = sessionEvictionPreferenceManager.idleMinutes
    val isDeveloper: kotlinx.coroutines.flow.Flow<Boolean> = authRepository.observeAuthState()
        .map { user -> user?.email.equals("nasaawakening@gmail.com", ignoreCase = true) }

    fun setEvictionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionEvictionPreferenceManager.setEnabled(enabled)
            if (enabled) {
                sessionEvictionScheduler.schedule(appContext)
            } else {
                sessionEvictionScheduler.cancel(appContext)
            }
        }
    }

    fun setEvictionIdleMinutes(minutes: Int) {
        viewModelScope.launch {
            sessionEvictionPreferenceManager.setIdleMinutes(minutes)
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            backupPreferenceManager.setAutoBackupEnabled(enabled)
            val days = backupPreferenceManager.getIntervalDaysBlocking()
            if (enabled) {
                backupScheduler.schedule(appContext, days)
            } else {
                backupScheduler.cancel(appContext)
            }
        }
    }

    fun setAutoBackupIntervalDays(days: Int) {
        viewModelScope.launch {
            backupPreferenceManager.setIntervalDays(days)
            if (backupPreferenceManager.isAutoBackupEnabledBlocking()) {
                backupScheduler.schedule(appContext, days)
            }
        }
    }

    fun setFontScalePercent(percent: Int) {
        viewModelScope.launch {
            themePreferenceManager.setFontScalePercent(percent)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            themePreferenceManager.setThemeMode(mode)
        }
    }

    fun setAccentColor(hex: String?) {
        viewModelScope.launch {
            themePreferenceManager.setAccentColor(hex)
        }
    }
}
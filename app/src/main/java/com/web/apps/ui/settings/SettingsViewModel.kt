package com.web.apps.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.web.apps.core.preferences.AppThemeMode
import com.web.apps.core.preferences.ThemePreferenceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreferenceManager: ThemePreferenceManager
) : ViewModel() {

    val themeMode: Flow<AppThemeMode> = themePreferenceManager.themeMode

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            themePreferenceManager.setThemeMode(mode)
        }
    }
}
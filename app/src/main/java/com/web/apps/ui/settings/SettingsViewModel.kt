package com.web.apps.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.web.apps.core.preferences.AppThemeMode
import com.web.apps.core.preferences.ThemePreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreferenceManager: ThemePreferenceManager
) : ViewModel() {

    val themeMode: Flow<AppThemeMode> = themePreferenceManager.themeMode
    val accentColor: Flow<String?> = themePreferenceManager.accentColor
    val fontScalePercent: Flow<Int> = themePreferenceManager.fontScalePercent

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
package com.web.apps.ui.plugin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.web.apps.core.plugin.PluginCatalogEntry
import com.web.apps.core.plugin.PluginManager
import com.web.apps.core.plugin.PluginManifest
import com.web.apps.core.plugin.PluginResult
import com.web.apps.core.preferences.PluginPreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PluginBrowserUiState(
    val isLoading: Boolean = true,
    val catalog: List<PluginCatalogEntry> = emptyList(),
    val activeThemeId: String? = null,
    val activeUiId: String? = null,
    val isApplying: Boolean = false,
    val errorMessage: String? = null,
    val resultMessage: String? = null
)

@HiltViewModel
class PluginBrowserViewModel @Inject constructor(
    private val pluginManager: PluginManager,
    private val pluginPreferenceManager: PluginPreferenceManager,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PluginBrowserUiState())
    val uiState: StateFlow<PluginBrowserUiState> = _uiState.asStateFlow()

    init {
        loadCatalog()
        viewModelScope.launch {
            combine(
                pluginPreferenceManager.activeThemePlugin,
                pluginPreferenceManager.activeUiPlugin
            ) { theme, ui -> theme?.id to ui?.id }
                .collect { (themeId, uiId) ->
                    _uiState.value = _uiState.value.copy(activeThemeId = themeId, activeUiId = uiId)
                }
        }
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = pluginManager.fetchCatalog()) {
                is PluginResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, catalog = result.data)
                }
                is PluginResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun isDownloaded(pluginId: String): Boolean = pluginManager.isPluginDownloaded(appContext, pluginId)

    fun isActive(entry: PluginCatalogEntry): Boolean {
        return if (entry.type == "ui") {
            _uiState.value.activeUiId == entry.id
        } else {
            _uiState.value.activeThemeId == entry.id
        }
    }

    fun downloadPlugin(entry: PluginCatalogEntry) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isApplying = true)
            when (val result = pluginManager.downloadPlugin(appContext, entry)) {
                is PluginResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isApplying = false,
                        resultMessage = "${entry.name} downloaded. Tap Use to apply it."
                    )
                }
                is PluginResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isApplying = false,
                        resultMessage = "Download failed: ${result.message}"
                    )
                }
            }
        }
    }

    fun usePlugin(entry: PluginCatalogEntry) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isApplying = true)

            val file = java.io.File(java.io.File(appContext.filesDir, "plugins"), "${entry.id}.wp")
            when (val result = pluginManager.extractManifest(file)) {
                is PluginResult.Success -> {
                    pluginPreferenceManager.setActivePlugin(result.data)
                    _uiState.value = _uiState.value.copy(
                        isApplying = false,
                        resultMessage = "${entry.name} is now active."
                    )
                }
                is PluginResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isApplying = false,
                        resultMessage = "Failed to apply plugin: ${result.message}"
                    )
                }
            }
        }
    }

    fun unusePlugin(entry: PluginCatalogEntry) {
        viewModelScope.launch {
            pluginPreferenceManager.clearActivePlugin(entry.type)
            _uiState.value = _uiState.value.copy(resultMessage = "${entry.name} disabled.")
        }
    }

    fun dismissResult() {
        _uiState.value = _uiState.value.copy(resultMessage = null)
    }
}
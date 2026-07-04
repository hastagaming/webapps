package com.web.apps.ui.backup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.web.apps.backup.BackupManager
import com.web.apps.backup.BackupResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun onEvent(event: BackupEvent) {
        when (event) {
            is BackupEvent.ExportRequested -> exportBackup(event.uri)
            is BackupEvent.ImportRequested -> importBackup(event.uri)
            is BackupEvent.ToggleEncryption -> {
                _uiState.value = _uiState.value.copy(encryptBackup = event.enabled)
            }
            is BackupEvent.ChangeMergeStrategy -> {
                _uiState.value = _uiState.value.copy(mergeStrategy = event.strategy)
            }
            is BackupEvent.DismissResult -> {
                _uiState.value = _uiState.value.copy(resultMessage = null, isError = false)
            }
        }
    }

    private fun exportBackup(uri: android.net.Uri) {
        _uiState.value = _uiState.value.copy(isProcessing = true)
        viewModelScope.launch {
            val result = backupManager.exportBackup(
                context = context,
                destinationUri = uri,
                encrypt = _uiState.value.encryptBackup
            )
            handleResult(result)
        }
    }

    private fun importBackup(uri: android.net.Uri) {
        _uiState.value = _uiState.value.copy(isProcessing = true)
        viewModelScope.launch {
            val result = backupManager.importBackup(
                context = context,
                sourceUri = uri,
                mergeStrategy = _uiState.value.mergeStrategy
            )
            handleResult(result)
        }
    }

    private fun handleResult(result: BackupResult) {
        when (result) {
            is BackupResult.Success -> {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    resultMessage = result.message,
                    isError = false
                )
            }
            is BackupResult.Failure -> {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    resultMessage = result.reason,
                    isError = true
                )
            }
        }
    }
}
package com.web.apps.ui.update

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.web.apps.core.update.DownloadProgress
import com.web.apps.core.update.UpdateCheckResult
import com.web.apps.core.update.UpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateManager: UpdateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private var hasStarted = false

    fun startUpdateFlow(context: Context) {
        if (hasStarted) return
        hasStarted = true

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                updateManager.checkForUpdate()
            }
            when (result) {
                is UpdateCheckResult.UpToDate -> {
                    _uiState.value = _uiState.value.copy(isBusy = false, isUpToDate = true)
                }
                is UpdateCheckResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isBusy = false, errorMessage = result.message)
                }
                is UpdateCheckResult.UpdateAvailable -> {
                    if (!updateManager.canRequestPackageInstalls(context)) {
                        _uiState.value = _uiState.value.copy(
                            isBusy = false,
                            needsInstallPermission = true
                        )
                        return@launch
                    }

                    _uiState.value = _uiState.value.copy(
                        statusMessage = "Downloading version ${result.versionName}..."
                    )

                    withContext(Dispatchers.IO) {
                        updateManager.downloadUpdate(context, result.downloadUrl) { progress ->
                            when (progress) {
                                is DownloadProgress.InProgress -> {
                                    _uiState.value = _uiState.value.copy(
                                        progressPercent = progress.percent,
                                        statusMessage = "Downloading version ${result.versionName}..."
                                    )
                                }
                                is DownloadProgress.Completed -> {
                                    _uiState.value = _uiState.value.copy(
                                        progressPercent = 100,
                                        statusMessage = "Ready to install.",
                                        isInstalling = true,
                                        isBusy = false
                                    )
                                    updateManager.installUpdate(context, progress.file)
                                }
                                is DownloadProgress.Failed -> {
                                    _uiState.value = _uiState.value.copy(
                                        isBusy = false,
                                        errorMessage = progress.message
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
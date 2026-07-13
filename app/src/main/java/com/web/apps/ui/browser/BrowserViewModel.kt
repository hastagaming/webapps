package com.web.apps.ui.browser

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.web.apps.core.container.ContainerManager
import com.web.apps.data.local.entity.OrientationMode
import com.web.apps.data.repository.ContainerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val containerRepository: ContainerRepository,
    private val containerManager: ContainerManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val containerId: Long = checkNotNull(savedStateHandle["containerId"])
    private val sessionStartTime: Long = System.currentTimeMillis()

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    init {
        observeContainer()
    }

    private fun observeContainer() {
        containerRepository.observeContainerById(containerId)
            .onEach { container ->
                if (container != null) {
                    _uiState.value = _uiState.value.copy(
                        container = container,
                        isLocked = container.isLocked,
                        showUnlockDialog = container.isLocked
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: BrowserEvent) {
        when (event) {
            is BrowserEvent.LoadUrl -> loadUrl(event.url)
            is BrowserEvent.Refresh -> refresh()
            is BrowserEvent.GoBack -> goBack()
            is BrowserEvent.GoForward -> goForward()
            is BrowserEvent.ToggleDesktopMode -> toggleDesktopMode()
            is BrowserEvent.ToggleFullscreen -> toggleFullscreen()
            is BrowserEvent.UnlockAttempt -> attemptUnlock(event.pin)
            is BrowserEvent.DismissDangerousWarning -> dismissDangerousWarning()
            is BrowserEvent.AllowHttpOnce -> allowHttpOnce()
            is BrowserEvent.DismissHttpWarning -> dismissHttpWarning()
        }
    }

    private fun loadUrl(url: String) {
        val session = containerManager.getSession(containerId) ?: return
        val normalized = try {
            com.web.apps.data.repository.UrlValidator.normalize(url)
        } catch (e: IllegalArgumentException) {
            _uiState.value = _uiState.value.copy(errorMessage = e.message)
            return
        }
        session.webView.loadUrl(normalized)
    }

    private fun refresh() {
        containerManager.refreshContainer(containerId)
    }

    private fun goBack() {
        containerManager.goBack(containerId)
    }

    private fun goForward() {
        containerManager.goForward(containerId)
    }

    private fun toggleDesktopMode() {
        val container = _uiState.value.container ?: return
        viewModelScope.launch {
            containerRepository.setDesktopMode(containerId, !container.isDesktopMode)
            containerManager.refreshContainer(containerId)
        }
    }

    private fun toggleFullscreen() {
        _uiState.value = _uiState.value.copy(isFullscreen = !_uiState.value.isFullscreen)
    }

    private fun attemptUnlock(pin: String) {
        viewModelScope.launch {
            val success = containerRepository.unlockContainer(containerId, pin)
            if (success) {
                _uiState.value = _uiState.value.copy(
                    isLocked = false,
                    showUnlockDialog = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "PIN salah, coba lagi"
                )
            }
        }
    }

    private fun dismissDangerousWarning() {
        _uiState.value = _uiState.value.copy(
            showDangerousSiteWarning = false,
            dangerousUrl = null
        )
        goBack()
    }

    private fun allowHttpOnce() {
        val blockedUrl = _uiState.value.blockedUrl ?: return
        val session = containerManager.getSession(containerId) ?: return
        session.webView.loadUrl(blockedUrl)
        _uiState.value = _uiState.value.copy(
            showHttpBlockedWarning = false,
            blockedUrl = null
        )
    }

    private fun dismissHttpWarning() {
        _uiState.value = _uiState.value.copy(
            showHttpBlockedWarning = false,
            blockedUrl = null
        )
    }

    fun updateProgress(progress: Int) {
        _uiState.value = _uiState.value.copy(
            loadProgress = progress,
            isLoading = progress in 1..99
        )
    }

    fun updateFavicon(bitmap: android.graphics.Bitmap) {
        _uiState.value = _uiState.value.copy(faviconBitmap = bitmap)
    }

    fun onDangerousSiteDetected(url: String) {
        _uiState.value = _uiState.value.copy(
            showDangerousSiteWarning = true,
            dangerousUrl = url
        )
    }

    fun onHttpBlocked(url: String) {
        _uiState.value = _uiState.value.copy(
            showHttpBlockedWarning = true,
            blockedUrl = url
        )
    }

    override fun onCleared() {
        super.onCleared()

        val elapsed = System.currentTimeMillis() - sessionStartTime
        if (elapsed > 0) {
            containerRepository.addUsageMillis(containerId, elapsed)
        }
    }
}
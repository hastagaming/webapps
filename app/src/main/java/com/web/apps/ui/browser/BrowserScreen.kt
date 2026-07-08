package com.web.apps.ui.browser

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.web.apps.core.container.ContainerManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    containerManager: ContainerManager,
    onNavigateBack: () -> Unit,
    onNavigateToSourceInspector: (Long) -> Unit = {},
    onNavigateToPermissionManager: (Long) -> Unit = {},
    onNavigateToSwitchedContainer: (Long) -> Unit = {},
    viewModel: BrowserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val recoveryViewModel: com.web.apps.ui.recovery.RecoveryViewModel = hiltViewModel()
    val pendingRecoveryEvent by recoveryViewModel.pendingEvent.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val appSwitcherViewModel: com.web.apps.ui.appswitcher.AppSwitcherViewModel = hiltViewModel()
    val appSwitcherState by appSwitcherViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            if (!uiState.isFullscreen) {
                TopAppBar(
                    title = { Text(uiState.container?.name ?: "WebApps") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back to list")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.onEvent(BrowserEvent.GoBack) }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Web back")
                        }
                        IconButton(onClick = { viewModel.onEvent(BrowserEvent.GoForward) }) {
                            Icon(Icons.Filled.ArrowForward, contentDescription = "Web forward")
                        }
                        IconButton(onClick = { viewModel.onEvent(BrowserEvent.Refresh) }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = { viewModel.onEvent(BrowserEvent.ToggleDesktopMode) }) {
                            Icon(Icons.Filled.Computer, contentDescription = "Desktop mode")
                        }
                        IconButton(onClick = { viewModel.onEvent(BrowserEvent.ToggleFullscreen) }) {
                            Icon(Icons.Filled.Fullscreen, contentDescription = "Fullscreen")
                        }
                        IconButton(onClick = {
                            uiState.container?.let { onNavigateToSourceInspector(it.containerId) }
                        }) {
                            Icon(
                                Icons.Filled.Code,
                                contentDescription = "Source Inspector"
                            )
                        }
                        IconButton(onClick = {
                            uiState.container?.let { onNavigateToPermissionManager(it.containerId) }
                        }) {
                            Icon(
                                Icons.Filled.Security,
                                contentDescription = "Site Permissions"
                            )
                        }
                        IconButton(onClick = {
                            uiState.container?.let { container ->
                                recoveryViewModel.triggerManualRecovery(container.containerId)
                            }
                        }) {
                            Icon(
                                androidx.compose.material.icons.Icons.Filled.Refresh,
                                contentDescription = "Force Recovery"
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
           modifier = Modifier
               .fillMaxSize()
               .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    progress = { uiState.loadProgress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val container = uiState.container
                if (container != null && !uiState.isLocked) {
                    AndroidView(
                        factory = { context ->
                            val webView = containerManager.getOrCreateSession(
                                context = context,
                                container = container,
                                onFaviconReceived = { _, bitmap -> viewModel.updateFavicon(bitmap) },
                                onProgressChanged = { _, progress -> viewModel.updateProgress(progress) },
                                onPageFinished = { _, _ -> },
                                onDangerousSiteDetected = { _, url -> viewModel.onDangerousSiteDetected(url) },
                                onHttpBlocked = { _, url -> viewModel.onHttpBlocked(url) },
                                onDownloadRequested = { _, _, _, _, _ -> },
                                onShowCustomView = { },
                                onHideCustomView = { }
                            ).webView

                            (webView.parent as? android.view.ViewGroup)?.removeView(webView)
                            webView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (uiState.isLocked) {
                    ContainerLockOverlay(
                        onUnlockAttempt = { pin -> viewModel.onEvent(BrowserEvent.UnlockAttempt(pin)) },
                        errorMessage = uiState.errorMessage
                    )
                }
            }

            com.web.apps.ui.appswitcher.SwipeUpHandle(
                onSwipeUpTriggered = {
                    appSwitcherViewModel.onEvent(com.web.apps.ui.appswitcher.AppSwitcherEvent.Show)
                }
            )
        }
    }

    if (uiState.showDangerousSiteWarning) {
        DangerousSiteDialog(
            url = uiState.dangerousUrl.orEmpty(),
            onDismiss = { viewModel.onEvent(BrowserEvent.DismissDangerousWarning) }
        )
    }

    val currentContainerId = uiState.container?.containerId
    if (pendingRecoveryEvent != null && pendingRecoveryEvent?.containerId == currentContainerId) {
        com.web.apps.ui.recovery.RecoveryDialog(
            event = pendingRecoveryEvent!!,
            onSoftReset = {
                recoveryViewModel.onSoftReset(pendingRecoveryEvent!!.containerId)
            },
            onHardReset = {
                recoveryViewModel.onHardReset(context, pendingRecoveryEvent!!.containerId)
            },
            onDismiss = {
                recoveryViewModel.dismiss()
            }
        )
    }

    if (appSwitcherState.isVisible) {
        com.web.apps.ui.appswitcher.AppSwitcherOverlay(
            activeContainers = appSwitcherState.activeContainers,
            onSwitchToContainer = { containerId ->
                appSwitcherViewModel.onEvent(com.web.apps.ui.appswitcher.AppSwitcherEvent.SwitchToContainer(containerId))
                onNavigateToSwitchedContainer(containerId)
            },
            onDismissContainer = { containerId ->
                appSwitcherViewModel.onEvent(com.web.apps.ui.appswitcher.AppSwitcherEvent.DismissContainer(containerId))
            },
            onDismissAll = {
                appSwitcherViewModel.onEvent(com.web.apps.ui.appswitcher.AppSwitcherEvent.DismissAll)
            },
            onClose = {
                appSwitcherViewModel.onEvent(com.web.apps.ui.appswitcher.AppSwitcherEvent.Hide)
            }
        )
    }

    if (uiState.showHttpBlockedWarning) {
        HttpBlockedDialog(
            url = uiState.blockedUrl.orEmpty(),
            onAllowOnce = { viewModel.onEvent(BrowserEvent.AllowHttpOnce) },
            onDismiss = { viewModel.onEvent(BrowserEvent.DismissHttpWarning) }
        )
    }
}

@Composable
private fun ContainerLockOverlay(
    onUnlockAttempt: (String) -> Unit,
    errorMessage: String?
) {
    var pinInput by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Container Locked",
                style = MaterialTheme.typography.headlineSmall
            )
            OutlinedTextField(
                value = pinInput,
                onValueChange = { pinInput = it },
                label = { Text("Enter PIN") }
            )
            if (errorMessage != null) {
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            }
            TextButton(onClick = { onUnlockAttempt(pinInput) }) {
                Text("Unlock")
            }
        }
    }
}

@Composable
private fun DangerousSiteDialog(
    url: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dangerous Website Detected") },
        text = { Text("The following website was blocked because it was detected as dangerous:\n$url") },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Go Back") }
        }
    )
}

@Composable
private fun HttpBlockedDialog(
    url: String,
    onAllowOnce: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Insecure HTTP Connection") },
        text = { Text("This URL uses HTTP instead of HTTPS:\n$url\n\nOnly continue if you trust this website.") },
        confirmButton = {
            TextButton(onClick = onAllowOnce) { Text("Continue Once") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
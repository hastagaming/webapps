package com.web.apps.ui.containerlock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainerLockScreen(
    onNavigateBack: () -> Unit,
    viewModel: ContainerLockViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Container Lock") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when (uiState.step) {
                LockSetupStep.IDLE -> {
                    Text(
                        text = if (uiState.isLocked) {
                            "This container is currently locked with a PIN."
                        } else {
                            "This container is not locked. Set a PIN to restrict access."
                        }
                    )

                    if (uiState.isLocked) {
                        Button(
                            modifier = Modifier.padding(top = 16.dp),
                            onClick = { viewModel.onEvent(ContainerLockEvent.StartRemoveLock) }
                        ) {
                            Text("Remove Lock")
                        }
                    } else {
                        Button(
                            modifier = Modifier.padding(top = 16.dp),
                            onClick = { viewModel.onEvent(ContainerLockEvent.StartSetup) }
                        ) {
                            Text("Set Up PIN Lock")
                        }
                    }
                }

                LockSetupStep.ENTER_NEW_PIN -> {
                    PinPad(
                        title = "Create a New PIN",
                        subtitle = "Choose a 4 to 8 digit PIN.",
                        errorMessage = uiState.errorMessage,
                        onPinComplete = { pin -> viewModel.onEvent(ContainerLockEvent.SubmitPin(pin)) },
                        onCancel = { viewModel.onEvent(ContainerLockEvent.CancelSetup) }
                    )
                }

                LockSetupStep.CONFIRM_NEW_PIN -> {
                    PinPad(
                        title = "Confirm Your PIN",
                        subtitle = "Enter the same PIN again to confirm.",
                        errorMessage = uiState.errorMessage,
                        onPinComplete = { pin -> viewModel.onEvent(ContainerLockEvent.SubmitPin(pin)) },
                        onCancel = { viewModel.onEvent(ContainerLockEvent.CancelSetup) }
                    )
                }

                LockSetupStep.ENTER_CURRENT_PIN_TO_REMOVE -> {
                    PinPad(
                        title = "Enter Current PIN",
                        subtitle = "Enter your current PIN to remove the lock.",
                        errorMessage = uiState.errorMessage,
                        onPinComplete = { pin -> viewModel.onEvent(ContainerLockEvent.SubmitPin(pin)) },
                        onCancel = { viewModel.onEvent(ContainerLockEvent.CancelSetup) }
                    )
                }
            }
        }
    }

    if (uiState.successMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(ContainerLockEvent.DismissMessage) },
            title = { Text("Success") },
            text = { Text(uiState.successMessage.orEmpty()) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEvent(ContainerLockEvent.DismissMessage)
                    onNavigateBack()
                }) {
                    Text("OK")
                }
            }
        )
    }
}
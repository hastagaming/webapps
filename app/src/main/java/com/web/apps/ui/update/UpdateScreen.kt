package com.web.apps.ui.update

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun UpdateScreen(
    onFinished: () -> Unit,
    viewModel: UpdateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    BackHandler(enabled = uiState.isBusy) {
        // Block back button saat update sedang berlangsung
    }

    LaunchedEffect(Unit) {
        viewModel.startUpdateFlow(context)
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "System Update", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(onClick = onFinished) { Text("Back") }
                }
                uiState.isUpToDate -> {
                    Text("WebApps is already up to date.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onFinished) { Text("Back") }
                }
                uiState.isInstalling -> {
                    Text("Please confirm the installation prompt to finish updating.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "The installer will complete the update process automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    Text(text = uiState.statusMessage, modifier = Modifier.padding(bottom = 16.dp))
                    LinearProgressIndicator(
                        progress = { uiState.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${uiState.progressPercent}%",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Do not leave this screen while the update is in progress.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
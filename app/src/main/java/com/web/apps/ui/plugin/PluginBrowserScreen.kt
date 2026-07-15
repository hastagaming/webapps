package com.web.apps.ui.plugin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect
import com.web.apps.core.plugin.PluginCatalogEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginBrowserScreen(
    onNavigateBack: () -> Unit,
    viewModel: PluginBrowserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Plugins") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                }
                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        uiState.catalog.forEach { entry ->
                            PluginRow(
                                entry = entry,
                                isDownloaded = viewModel.isDownloaded(entry.id),
                                isActive = viewModel.isActive(entry),
                                onDownload = { viewModel.downloadPlugin(entry) },
                                onUse = { viewModel.usePlugin(entry) },
                                onUnuse = { viewModel.unusePlugin(entry) }
                            )
                        }
                    }
                }
            }

            if (uiState.isApplying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text("Applying plugin...", modifier = Modifier.padding(top = 16.dp))
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState.resultMessage) {
        uiState.resultMessage?.let { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message)
            }
            viewModel.dismissResult()
        }
    }
}

@Composable
private fun PluginRow(
    entry: PluginCatalogEntry,
    isDownloaded: Boolean,
    isActive: Boolean,
    onDownload: () -> Unit,
    onUse: () -> Unit,
    onUnuse: () -> Unit
) {
    ListItem(
        headlineContent = { Text(entry.name) },
        supportingContent = { Text("${entry.description} • v${entry.version} by ${entry.author}") },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        try {
                            androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(entry.previewColorHex ?: "#888888"))
                        } catch (e: Exception) {
                            androidx.compose.ui.graphics.Color.Gray
                        }
                    )
            )
        },
        trailingContent = {
            when {
                isActive -> OutlinedButton(onClick = onUnuse) { Text("Unuse") }
                isDownloaded -> Button(onClick = onUse) { Text("Use") }
                else -> OutlinedButton(onClick = onDownload) { Text("Download") }
            }
        }
    )
}
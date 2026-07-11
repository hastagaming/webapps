package com.web.apps.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.web.apps.backup.ImportMergeStrategy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQrExport: () -> Unit,
    onNavigateToQrScan: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            viewModel.onEvent(BackupEvent.ExportRequested(uri))
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onEvent(BackupEvent.ImportRequested(uri))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Backup & Restore") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Text("Backup Encription (Android Keystore)")
                Switch(
                    checked = uiState.encryptBackup,
                    onCheckedChange = { viewModel.onEvent(BackupEvent.ToggleEncryption(it)) }
                )
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                onClick = {
                    val fileName = "webapps_backup_${System.currentTimeMillis()}.txt"
                    exportLauncher.launch(fileName)
                }
            ) {
                Text("Export Backup")
            }

            Text(
                text = "Restore Strategy",
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(
                    selected = uiState.mergeStrategy == ImportMergeStrategy.MERGE_KEEP_BOTH,
                    onClick = {
                        viewModel.onEvent(BackupEvent.ChangeMergeStrategy(ImportMergeStrategy.MERGE_KEEP_BOTH))
                    }
                )
                Text("Merge (save old and new data)")
            }

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(
                    selected = uiState.mergeStrategy == ImportMergeStrategy.REPLACE_ALL,
                    onClick = {
                        viewModel.onEvent(BackupEvent.ChangeMergeStrategy(ImportMergeStrategy.REPLACE_ALL))
                    }
                )
                Text("Change All File")
            }

            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                onClick = {
                    importLauncher.launch(arrayOf("text/plain"))
                }
            ) {
                Text("Import Backup")
            }

            androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Quick Transfer via QR Code", style = MaterialTheme.typography.titleSmall)

            OutlinedButton(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                onClick = onNavigateToQrExport
            ) {
                Text("Export via QR Code")
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                onClick = onNavigateToQrScan
            ) {
                Text("Import via QR Code")
            }

            if (uiState.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
            }
        }
    }

    if (uiState.resultMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(BackupEvent.DismissResult) },
            title = { Text(if (uiState.isError) "Failed" else "Success") },
            text = { Text(uiState.resultMessage.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(BackupEvent.DismissResult) }) {
                    Text("OK")
                }
            }
        )
    }
}
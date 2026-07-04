package com.web.apps.ui.permission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.web.apps.core.permission.PermissionDecision
import com.web.apps.core.permission.WebAppPermissionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionManagerScreen(
    onNavigateBack: () -> Unit,
    viewModel: PermissionManagerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Site Permissions") },
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
        ) {
            Text(
                text = "Permissions for ${uiState.containerName}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.entries, key = { it.type }) { entry ->
                    PermissionEntryCard(
                        entry = entry,
                        onDecisionChanged = { decision ->
                            viewModel.onEvent(PermissionManagerEvent.ChangeDecision(entry.type, decision))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionEntryCard(
    entry: PermissionEntry,
    onDecisionChanged: (PermissionDecision) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = permissionLabel(entry.type), style = MaterialTheme.typography.titleSmall)

            if (!entry.hasSystemPermission) {
                Text(
                    text = "System permission not yet granted. The app will still ask Android for this permission when needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            PermissionDecision.entries.forEach { decision ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    RadioButton(
                        selected = entry.decision == decision,
                        onClick = { onDecisionChanged(decision) }
                    )
                    Text(text = decisionLabel(decision))
                }
            }
        }
    }
}

private fun permissionLabel(type: WebAppPermissionType): String {
    return when (type) {
        WebAppPermissionType.CAMERA -> "Camera"
        WebAppPermissionType.MICROPHONE -> "Microphone"
        WebAppPermissionType.LOCATION -> "Location"
        WebAppPermissionType.NOTIFICATIONS -> "Notifications"
        WebAppPermissionType.STORAGE -> "Storage"
    }
}

private fun decisionLabel(decision: PermissionDecision): String {
    return when (decision) {
        PermissionDecision.ALLOWED -> "Always Allow"
        PermissionDecision.DENIED -> "Always Deny"
        PermissionDecision.ASK_EVERY_TIME -> "Ask Every Time"
    }
}
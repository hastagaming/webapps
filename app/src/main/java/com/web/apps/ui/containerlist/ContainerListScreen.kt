@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.web.apps.ui.containerlist

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.web.apps.data.local.entity.ContainerEntity
import com.web.apps.data.local.entity.GroupEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainerListScreen(
    onContainerClick: (Long) -> Unit,
    onNavigateToBackup: () -> Unit = {},
    onNavigateToLockSettings: (Long) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onSignOut: () -> Unit = {},
    viewModel: ContainerListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var fabMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WebApps") },
                actions = {
                    IconButton(onClick = { onNavigateToBackup() }) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = "Backup & Restore")
                    }
                    IconButton(onClick = {
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                        onSignOut()
                    }) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Logout, contentDescription = "Sign Out")
                    }
                    IconButton(onClick = { onNavigateToSettings() }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { viewModel.onEvent(ContainerListEvent.RefreshAll) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh All")
                    }
                    IconButton(onClick = { viewModel.onEvent(ContainerListEvent.StopAll) }) {
                        Icon(Icons.Filled.Stop, contentDescription = "Stop All")
                    }
                }
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { fabMenuExpanded = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                }
                DropdownMenu(expanded = fabMenuExpanded, onDismissRequest = { fabMenuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("New Container") },
                        onClick = {
                            fabMenuExpanded = false
                            viewModel.onEvent(ContainerListEvent.OpenAddContainerDialog(groupId = null))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("New Grup") },
                        onClick = {
                            fabMenuExpanded = false
                            viewModel.onEvent(ContainerListEvent.OpenAddGroupDialog)
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onEvent(ContainerListEvent.SearchQueryChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                label = { Text("Search Container") },
                singleLine = true
            )

            if (uiState.isSearching) {
                SearchResultsList(
                    results = uiState.searchResults,
                    onContainerClick = onContainerClick,
                    onRefresh = { viewModel.onEvent(ContainerListEvent.RefreshContainer(it)) },
                    onStop = { viewModel.onEvent(ContainerListEvent.StopContainer(it)) },
                    onDelete = { viewModel.onEvent(ContainerListEvent.DeleteContainer(it)) }
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (uiState.ungroupedContainers.isNotEmpty()) {
                        item {
                            GroupSection(
                                groupName = "Without Group",
                                groupColor = MaterialTheme.colorScheme.surfaceVariant,
                                containers = uiState.ungroupedContainers,
                                onContainerClick = onContainerClick,
                                onRefresh = { viewModel.onEvent(ContainerListEvent.RefreshContainer(it)) },
                                onStop = { viewModel.onEvent(ContainerListEvent.StopContainer(it)) },
                                onDelete = { viewModel.onEvent(ContainerListEvent.DeleteContainer(it)) },
                                onAddContainer = {
                                    viewModel.onEvent(ContainerListEvent.OpenAddContainerDialog(groupId = null))
                                },
                                onOpenLockSettings = { containerId ->
                                    onNavigateToLockSettings(containerId)
                                }
                            )
                        }
                    }

                    items(uiState.groups, key = { it.groupId }) { group ->
                        GroupSection(
                            groupName = group.name,
                            groupColor = Color(android.graphics.Color.parseColor(group.colorHex)),
                            containers = uiState.containersByGroup[group.groupId].orEmpty(),
                            onContainerClick = onContainerClick,
                            onRefresh = { viewModel.onEvent(ContainerListEvent.RefreshContainer(it)) },
                            onStop = { viewModel.onEvent(ContainerListEvent.StopContainer(it)) },
                            onDelete = { viewModel.onEvent(ContainerListEvent.DeleteContainer(it)) },
                            onAddContainer = {
                                viewModel.onEvent(ContainerListEvent.OpenAddContainerDialog(groupId = group.groupId))
                            },
                            onOpenLockSettings = { containerId ->
                                onNavigateToLockSettings(containerId)
                            },
                            onDeleteGroup = { viewModel.onEvent(ContainerListEvent.DeleteGroup(group)) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.showAddContainerDialog) {
        AddContainerDialog(
            onConfirm = { name, url ->
                viewModel.onEvent(
                    ContainerListEvent.CreateContainer(
                        name = name,
                        url = url,
                        groupId = uiState.addContainerTargetGroupId
                    )
                )
            },
            onDismiss = { viewModel.onEvent(ContainerListEvent.DismissAddContainerDialog) },
            errorMessage = uiState.errorMessage
        )
    }

    if (uiState.showAddGroupDialog) {
        AddGroupDialog(
            onConfirm = { name, color, iconUri -> viewModel.onEvent(ContainerListEvent.CreateGroup(name, color, iconUri)) },
            onDismiss = { viewModel.onEvent(ContainerListEvent.DismissAddGroupDialog) }
        )
    }
}

@Composable
private fun GroupSection(
    groupName: String,
    groupColor: androidx.compose.ui.graphics.Color,
    containers: List<com.web.apps.data.local.entity.ContainerEntity>,
    onContainerClick: (Long) -> Unit,
    onRefresh: (Long) -> Unit,
    onStop: (Long) -> Unit,
    onDelete: (com.web.apps.data.local.entity.ContainerEntity) -> Unit,
    onAddContainer: () -> Unit,
    onOpenLockSettings: (Long) -> Unit,
    onDeleteGroup: (() -> Unit)? = null
) {

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(groupColor.copy(alpha = 0.15f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(text = groupName, style = MaterialTheme.typography.titleMedium)
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            val minTileWidth = 100.dp
            val columnCount = (maxWidth / minTileWidth).toInt().coerceAtLeast(2)

            val allItems: List<ContainerEntity?> = containers + listOf(null)
            val rows = allItems.chunked(columnCount)

            Column(modifier = Modifier.fillMaxWidth()) {
                rows.forEach { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEach { container ->
                            Box(modifier = Modifier.fillMaxWidth(1f / columnCount)) {
                                if (container != null) {
                                    ContainerTile(
                                        container = container,
                                        onClick = { onContainerClick(container.containerId) },
                                        onRefresh = { onRefresh(container.containerId) },
                                        onStop = { onStop(container.containerId) },
                                        onDelete = { onDelete(container) },
                                        onOpenLockSettings = { onOpenLockSettings(container.containerId) }
                                    )
                                } else {
                                    AddContainerTile(onClick = onAddContainer)
                                }
                            }
                        }
                        repeat(columnCount - rowItems.size) {
                            Spacer(modifier = Modifier.fillMaxWidth(1f / columnCount))
                        }
                    }
                }
            }
        }

        if (onDeleteGroup != null) {
            TextButton(onClick = onDeleteGroup, modifier = Modifier.padding(start = 8.dp)) {
                Text("Delete Grup", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ContainerTile(
    container: ContainerEntity,
    onClick: () -> Unit,
    onRefresh: () -> Unit,
    onStop: () -> Unit,
    onDelete: () -> Unit,
    onOpenLockSettings: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(4.dp)) {
        Card(
            modifier = Modifier
                .aspectRatio(1f)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                coil.compose.AsyncImage(
                    model = "https://www.google.com/s2/favicons?sz=64&domain=${android.net.Uri.parse(container.url).host}",
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = container.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
            }
        }

        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text("Refresh") },
                onClick = { menuExpanded = false; onRefresh() }
            )
            DropdownMenuItem(
                text = { Text("Stop") },
                onClick = { menuExpanded = false; onStop() }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = { menuExpanded = false; onDelete() }
            )
            DropdownMenuItem(
                text = { Text("Lock Settings") },
                onClick = { menuExpanded = false; onOpenLockSettings() }
            )
        }
    }
}

@Composable
private fun AddContainerTile(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp)),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Add container",
                modifier = Modifier.padding(24.dp)
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SearchResultsList(
    results: List<ContainerEntity>,
    onContainerClick: (Long) -> Unit,
    onRefresh: (Long) -> Unit,
    onStop: (Long) -> Unit,
    onDelete: (ContainerEntity) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(results, key = { it.containerId }) { container ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .combinedClickable(
                        onClick = { onContainerClick(container.containerId) },
                        onLongClick = { }
                    )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(container.name, style = MaterialTheme.typography.titleSmall)
                    Text(container.url, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddContainerDialog(
    onConfirm: (name: String, url: String) -> Unit,
    onDismiss: () -> Unit,
    errorMessage: String?
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Container") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    singleLine = true,
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank() && url.isNotBlank()) onConfirm(name, url) }) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelled") }
        }
    )
}

@Composable
private fun AddGroupDialog(
    onConfirm: (name: String, colorHex: String, iconUri: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    val presetColors = listOf("#2196F3", "#4CAF50", "#FF9800", "#E91E63", "#9C27B0")
    var selectedColor by remember { mutableStateOf(presetColors.first()) }
    var pickedIconPath by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val savedPath = copyImageToInternalStorage(context, uri)
            if (savedPath != null) {
                pickedIconPath = savedPath
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Grup") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name") },
                    singleLine = true
                )

                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text("Group Icon (optional)")
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .aspectRatio(1f)
                            .fillMaxWidth(0.35f)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .combinedClickable(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                onLongClick = {}
                            ),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        if (pickedIconPath != null) {
                            AsyncImage(
                                model = File(pickedIconPath!!),
                                contentDescription = "Selected group icon",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Filled.Add, contentDescription = "Pick icon")
                        }
                    }
                }

                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text("Pick a Color")
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetColors.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .background(
                                        color = Color(android.graphics.Color.parseColor(hex)),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .combinedClickable(onClick = { selectedColor = hex }, onLongClick = {})
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name, selectedColor, pickedIconPath) }) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("cancelle") }
        }
    )
}

private fun copyImageToInternalStorage(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val iconsDir = File(context.filesDir, "group_icons")
        if (!iconsDir.exists()) iconsDir.mkdirs()

        val fileName = "icon_${System.currentTimeMillis()}.png"
        val destFile = File(iconsDir, fileName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        destFile.absolutePath
    } catch (e: Exception) {
        null
    }
}
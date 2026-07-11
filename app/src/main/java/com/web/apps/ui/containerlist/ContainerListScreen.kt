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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.ListItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
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
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        com.web.apps.core.notification.BadgeCountManager().clearBadge(context)
    }
    var accountMenuExpanded by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var fabMenuExpanded by remember { mutableStateOf(false) }
    var containerForGroupMove by remember { mutableStateOf<ContainerEntity?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("WebApps")
                        if (currentUser != null) {
                            Box {
                                androidx.compose.foundation.layout.Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { accountMenuExpanded = true }
                                        .padding(top = 2.dp)
                                ) {
                                    if (currentUser?.photoUrl != null) {
                                        AsyncImage(
                                            model = currentUser?.photoUrl,
                                            contentDescription = "Account photo",
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Filled.AccountCircle,
                                            contentDescription = "Account",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = currentUser?.displayName ?: currentUser?.email ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 6.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = accountMenuExpanded,
                                    onDismissRequest = { accountMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(currentUser?.email ?: "") },
                                        onClick = { accountMenuExpanded = false },
                                        enabled = false
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Sign Out") },
                                        onClick = {
                                            accountMenuExpanded = false
                                            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                                            onSignOut()
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
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
                    IconButton(onClick = {
                        val activeCount = viewModel.getActiveSessionCount()
                        if (activeCount == 0) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("No active containers to refresh. Open a container first.")
                            }
                        } else {
                            viewModel.onEvent(ContainerListEvent.RefreshAll)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Refreshed $activeCount container(s)")
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh All")
                    }
                    IconButton(onClick = {
                        val activeCount = viewModel.getActiveSessionCount()
                        if (activeCount == 0) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("No active containers to stop.")
                            }
                        } else {
                            viewModel.onEvent(ContainerListEvent.StopAll)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Stopped $activeCount container(s)")
                            }
                        }
                    }) {
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
                        text = { Text("New Group") },
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
                    if (uiState.pinnedContainers.isNotEmpty()) {
                        item {
                            GroupSection(
                                groupName = "Pinned",
                                groupColor = MaterialTheme.colorScheme.primaryContainer,
                                containers = uiState.pinnedContainers,
                                onContainerClick = onContainerClick,
                                onRefresh = { viewModel.onEvent(ContainerListEvent.RefreshContainer(it)) },
                                onStop = { viewModel.onEvent(ContainerListEvent.StopContainer(it)) },
                                onMoveUp = { viewModel.onEvent(ContainerListEvent.MoveContainerUp(it)) },
                                onMoveDown = { viewModel.onEvent(ContainerListEvent.MoveContainerDown(it)) },
                                onRequestMoveToGroup = { container -> containerForGroupMove = container },
                                onToggleNotification = { containerId, enabled ->
                                    viewModel.onEvent(ContainerListEvent.ToggleNotification(containerId, enabled))
                                },
                                onChangeIcon = { containerId, path ->
                                    viewModel.onEvent(ContainerListEvent.ChangeContainerIcon(containerId, path))
                                },
                                onToggleKeepAlive = { containerId, enabled ->
                                    viewModel.onEvent(ContainerListEvent.ToggleKeepAlive(containerId, enabled))
                                },
                                onDelete = { viewModel.onEvent(ContainerListEvent.DeleteContainer(it)) },
                                onAddContainer = { },
                                onOpenLockSettings = { containerId -> onNavigateToLockSettings(containerId) },
                                onTogglePin = { containerId, pinned ->
                                    viewModel.onEvent(ContainerListEvent.TogglePin(containerId, pinned))
                                }
                            )
                        }
                    }

                    if (uiState.ungroupedContainers.isNotEmpty()) {
                        item {
                            GroupSection(
                                groupName = "Without Group",
                                groupColor = MaterialTheme.colorScheme.surfaceVariant,
                                containers = uiState.ungroupedContainers,
                                onContainerClick = onContainerClick,
                                onRefresh = { viewModel.onEvent(ContainerListEvent.RefreshContainer(it)) },
                                onStop = { viewModel.onEvent(ContainerListEvent.StopContainer(it)) },
                                onMoveUp = { viewModel.onEvent(ContainerListEvent.MoveContainerUp(it)) },
                                onMoveDown = { viewModel.onEvent(ContainerListEvent.MoveContainerDown(it)) },
                                onRequestMoveToGroup = { container -> containerForGroupMove = container },
                                onToggleNotification = { containerId, enabled ->
                                    viewModel.onEvent(ContainerListEvent.ToggleNotification(containerId, enabled))
                                },
                                onTogglePin = { containerId, pinned ->
                                    viewModel.onEvent(ContainerListEvent.TogglePin(containerId, pinned))
                                },
                                onToggleKeepAlive = { containerId, enabled ->
                                    viewModel.onEvent(ContainerListEvent.ToggleKeepAlive(containerId, enabled))
                                },
                                onDelete = { viewModel.onEvent(ContainerListEvent.DeleteContainer(it)) },
                                onAddContainer = {
                                    viewModel.onEvent(ContainerListEvent.OpenAddContainerDialog(groupId = null))
                                },
                                onOpenLockSettings = { containerId ->
                                    onNavigateToLockSettings(containerId)
                                },
                                onChangeIcon = { containerId, path ->
                                    viewModel.onEvent(ContainerListEvent.ChangeContainerIcon(containerId, path))
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
                            onMoveUp = { viewModel.onEvent(ContainerListEvent.MoveContainerUp(it)) },
                            onMoveDown = { viewModel.onEvent(ContainerListEvent.MoveContainerDown(it)) },
                            onRequestMoveToGroup = { container -> containerForGroupMove = container },
                            onToggleNotification = { containerId, enabled ->
                                viewModel.onEvent(ContainerListEvent.ToggleNotification(containerId, enabled))
                            },
                            onToggleKeepAlive = { containerId, enabled ->
                                viewModel.onEvent(ContainerListEvent.ToggleKeepAlive(containerId, enabled))
                            },
                            onDelete = { viewModel.onEvent(ContainerListEvent.DeleteContainer(it)) },
                            onAddContainer = {
                                viewModel.onEvent(ContainerListEvent.OpenAddContainerDialog(groupId = group.groupId))
                            },
                            onOpenLockSettings = { containerId ->
                                onNavigateToLockSettings(containerId)
                            },
                            onDeleteGroup = { viewModel.onEvent(ContainerListEvent.DeleteGroup(group)) },
                            onChangeIcon = { containerId, path ->
                                    viewModel.onEvent(ContainerListEvent.ChangeContainerIcon(containerId, path))
                            },
                            onTogglePin = { containerId, pinned ->
                                viewModel.onEvent(ContainerListEvent.TogglePin(containerId, pinned))
                            }
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

    val moveTarget = containerForGroupMove
    if (moveTarget != null) {
        AlertDialog(
            onDismissRequest = { containerForGroupMove = null },
            title = { Text("Move to Group") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("No Group") },
                        modifier = Modifier.clickable {
                            viewModel.onEvent(ContainerListEvent.MoveContainerToGroup(moveTarget.containerId, null))
                            containerForGroupMove = null
                        }
                    )
                    uiState.groups.forEach { group ->
                        ListItem(
                            headlineContent = { Text(group.name) },
                            modifier = Modifier.clickable {
                                viewModel.onEvent(ContainerListEvent.MoveContainerToGroup(moveTarget.containerId, group.groupId))
                                containerForGroupMove = null
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { containerForGroupMove = null }) {
                    Text("Cancel")
                }
            }
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
    onToggleKeepAlive: (Long, Boolean) -> Unit,
    onOpenLockSettings: (Long) -> Unit,
    onChangeIcon: (Long, String) -> Unit,
    onMoveUp: (Long) -> Unit,
    onMoveDown: (Long) -> Unit,
    onToggleNotification: (Long, Boolean) -> Unit,
    onTogglePin: (Long, Boolean) -> Unit,
    onRequestMoveToGroup: (com.web.apps.data.local.entity.ContainerEntity) -> Unit,
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
                                        onToggleNotification = { enabled ->
                                             onToggleNotification(container.containerId, enabled)
                                        },
                                        onTogglePin = { pinned -> onTogglePin(container.containerId, pinned) },
                                        onDelete = { onDelete(container) },
                                        onOpenLockSettings = { onOpenLockSettings(container.containerId) },
                                        onChangeIcon = { path -> onChangeIcon(container.containerId, path) },
                                        onMoveUp = { onMoveUp(container.containerId) },
                                        onMoveDown = { onMoveDown(container.containerId) },
                                        onRequestMoveToGroup = { onRequestMoveToGroup(container) }
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
    onToggleNotification: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onOpenLockSettings: () -> Unit,
    onChangeIcon: (String) -> Unit,
    onMoveUp: () -> Unit,
    onTogglePin: (Boolean) -> Unit,
    onToggleKeepAlive: (Boolean) -> Unit,
    onMoveDown: () -> Unit,
    onRequestMoveToGroup: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val savedPath = copyContainerIconToInternalStorage(context, uri)
            if (savedPath != null) {
                onChangeIcon(savedPath)
            }
        }
    }

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
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                if (container.faviconLocalPath != null) {
                    AsyncImage(
                        model = File(container.faviconLocalPath),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    AsyncImage(
                        model = "https://www.google.com/s2/favicons?sz=64&domain=${android.net.Uri.parse(container.url).host}",
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = container.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
                Text(
                    text = container.url,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(
            onClick = { onToggleKeepAlive(!container.isKeepAliveEnabled) },
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.TopEnd)
                .size(28.dp)
        ) {
            Icon(
                imageVector = if (container.isKeepAliveEnabled) {
                    androidx.compose.material.icons.Icons.Filled.Bolt
                } else {
                    androidx.compose.material.icons.Icons.Outlined.Bolt
                },
                contentDescription = if (container.isKeepAliveEnabled) "Keep Alive is ON" else "Keep Alive is OFF",
                tint = if (container.isKeepAliveEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
                modifier = Modifier.size(18.dp)
            )
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
                text = { Text("Change Icon") },
                onClick = { menuExpanded = false; iconPickerLauncher.launch("image/*") }
            )
            DropdownMenuItem(
                text = { Text("Move Up") },
                onClick = { menuExpanded = false; onMoveUp() }
            )
            DropdownMenuItem(
                text = { Text("Move Down") },
                onClick = { menuExpanded = false; onMoveDown() }
            )
            DropdownMenuItem(
                text = { Text("Move to Group") },
                onClick = { menuExpanded = false; onRequestMoveToGroup() }
            )
            DropdownMenuItem(
                text = { Text(if (container.isNotificationEnabled) "Disable Notifications" else "Enable Notifications") },
                onClick = { menuExpanded = false; onToggleNotification(!container.isNotificationEnabled) }
            )
            DropdownMenuItem(
                text = { Text(if (container.isPinned) "Unpin" else "Pin to Top") },
                onClick = { menuExpanded = false; onTogglePin(!container.isPinned) }
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

private fun copyContainerIconToInternalStorage(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val iconsDir = File(context.filesDir, "container_icons")
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

private enum class ContainerInputMode { URL, SEARCH }

private data class SearchEngineOption(val label: String, val urlTemplate: String)

private val SEARCH_ENGINE_OPTIONS = listOf(
    SearchEngineOption("Google", "https://www.google.com/search?q="),
    SearchEngineOption("Bing", "https://www.bing.com/search?q="),
    SearchEngineOption("DuckDuckGo", "https://duckduckgo.com/?q="),
    SearchEngineOption("Yahoo", "https://search.yahoo.com/search?p=")
)

@Composable
private fun AddContainerDialog(
    onConfirm: (name: String, url: String) -> Unit,
    onDismiss: () -> Unit,
    errorMessage: String?
) {
    var name by remember { mutableStateOf("") }
    var inputValue by remember { mutableStateOf("") }
    var inputMode by remember { mutableStateOf(ContainerInputMode.URL) }
    var selectedEngine by remember { mutableStateOf(SEARCH_ENGINE_OPTIONS.first()) }
    var engineMenuExpanded by remember { mutableStateOf(false) }

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

                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.FilterChip(
                        selected = inputMode == ContainerInputMode.URL,
                        onClick = { inputMode = ContainerInputMode.URL },
                        label = { Text("URL") }
                    )
                    androidx.compose.material3.FilterChip(
                        selected = inputMode == ContainerInputMode.SEARCH,
                        onClick = { inputMode = ContainerInputMode.SEARCH },
                        label = { Text("Search Query") }
                    )
                }

                if (inputMode == ContainerInputMode.SEARCH) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text("Search Engine", style = MaterialTheme.typography.labelMedium)
                        Box {
                            androidx.compose.material3.OutlinedButton(
                                onClick = { engineMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                androidx.compose.foundation.layout.Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Text(selectedEngine.label)
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Filled.ArrowDropDown,
                                        contentDescription = "Show search engine options"
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = engineMenuExpanded,
                                onDismissRequest = { engineMenuExpanded = false }
                            ) {
                                SEARCH_ENGINE_OPTIONS.forEach { engine ->
                                    DropdownMenuItem(
                                        text = { Text(engine.label) },
                                        onClick = {
                                            selectedEngine = engine
                                            engineMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    label = { Text(if (inputMode == ContainerInputMode.URL) "URL" else "Search Keywords") },
                    singleLine = true,
                    modifier = Modifier.padding(top = 8.dp)
                )

                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && inputValue.isNotBlank()) {
                    val finalUrl = if (inputMode == ContainerInputMode.URL) {
                        inputValue
                    } else {
                        val encodedQuery = java.net.URLEncoder.encode(inputValue, "UTF-8")
                        selectedEngine.urlTemplate + encodedQuery
                    }
                    onConfirm(name, finalUrl)
                }
            }) {
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
    val presetColors = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7",
        "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
        "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
        "#FFEB3B", "#FFC107", "#FF9800", "#795548"
    )
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
            TextButton(onClick = onDismiss) { Text("cancell") }
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
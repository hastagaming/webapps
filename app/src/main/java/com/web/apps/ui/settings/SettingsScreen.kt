package com.web.apps.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Palette
import androidx.compose.ui.res.painterResource
import com.web.apps.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Switch
import androidx.compose.material.icons.filled.TimerOff
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.web.apps.core.preferences.AppThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToUpdate: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToPlugins: () -> Unit,
    isDeveloper: Boolean = false,
    onNavigateToLogViewer: () -> Unit = {},
    onNavigateToBugReport: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isDeveloper by viewModel.isDeveloper.collectAsState(initial = false)
    val fontScalePercent by viewModel.fontScalePercent.collectAsState(initial = 100)
    var showFontSizeDialog by remember { mutableStateOf(false) }
    val isEvictionEnabled by viewModel.isEvictionEnabled.collectAsState(initial = false)
    val evictionIdleMinutes by viewModel.evictionIdleMinutes.collectAsState(initial = 30)
    var showEvictionDialog by remember { mutableStateOf(false) }
    val accentColor by viewModel.accentColor.collectAsState(initial = null)
    var showAccentDialog by remember { mutableStateOf(false) }
    val isAutoBackupEnabled by viewModel.isAutoBackupEnabled.collectAsState(initial = false)
    val autoBackupIntervalDays by viewModel.autoBackupIntervalDays.collectAsState(initial = 7)
    val themeMode by viewModel.themeMode.collectAsState(initial = AppThemeMode.SYSTEM)
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(paddingValues)
                .fillMaxWidth()
        ) {
            ListItem(
                headlineContent = { Text("Accent Color") },
                supportingContent = { Text(if (accentColor == null) "Default" else "Custom") },
                leadingContent = { Icon(Icons.Filled.Palette, contentDescription = null) },
                modifier = Modifier.clickable { showAccentDialog = true }
            )

            ListItem(
                headlineContent = { Text("Web Page Font Size") },
                supportingContent = { Text(fontSizeLabel(fontScalePercent)) },
                leadingContent = { Icon(Icons.Filled.TextFields, contentDescription = null) },
                modifier = Modifier.clickable { showFontSizeDialog = true }
            )

            ListItem(
                headlineContent = { Text("Theme") },
                supportingContent = { Text(themeModeLabel(themeMode)) },
                leadingContent = { Icon(Icons.Filled.DarkMode, contentDescription = null) },
                modifier = Modifier.clickable { showThemeDialog = true }
            )

            ListItem(
                headlineContent = { Text("Update System") },
                supportingContent = { Text("Check for and install the latest version of WebApps") },
                leadingContent = { Icon(Icons.Filled.SystemUpdate, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onNavigateToUpdate)
            )

            ListItem(
                headlineContent = { Text("Usage Statistics") },
                supportingContent = { Text("See how often each container is used") },
                leadingContent = { Icon(androidx.compose.material.icons.Icons.Filled.BarChart, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onNavigateToStatistics)
            )

            ListItem(
                headlineContent = { Text("Plugins") },
                supportingContent = { Text("Browse and apply community themes") },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_plugin),
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable(onClick = onNavigateToPlugins)
            )

            ListItem(
                headlineContent = { Text("Auto-Stop Idle Containers") },
                supportingContent = {
                    Text(if (isEvictionEnabled) "Stop containers idle for $evictionIdleMinutes+ min" else "Off")
                },
                leadingContent = { Icon(Icons.Filled.TimerOff, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = isEvictionEnabled,
                        onCheckedChange = { viewModel.setEvictionEnabled(it) }
                    )
                },
                modifier = Modifier.clickable(enabled = isEvictionEnabled) { showEvictionDialog = true }
            )

            ListItem(
                headlineContent = { Text("Auto Backup") },
                supportingContent = { Text(if (isAutoBackupEnabled) "Every $autoBackupIntervalDays days to Downloads" else "Off") },
                leadingContent = { Icon(androidx.compose.material.icons.Icons.Filled.CloudDone, contentDescription = null) },
                trailingContent = {
                    androidx.compose.material3.Switch(
                        checked = isAutoBackupEnabled,
                        onCheckedChange = { viewModel.setAutoBackupEnabled(it) }
                    )
                }
            )
            if (isDeveloper) {
                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    "Developer Tools",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                ListItem(
                    headlineContent = { Text("Log Viewer") },
                    supportingContent = { Text("View crash and debug logs in-app") },
                    leadingContent = { Icon(androidx.compose.material.icons.Icons.Filled.BugReport, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onNavigateToLogViewer)
                )
            }

            ListItem(
                headlineContent = { Text("Report a Bug") },
                supportingContent = { Text("Found something broken? Let us know") },
                leadingContent = { Icon(androidx.compose.material.icons.Icons.Filled.BugReport, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onNavigateToBugReport)
            )
        }
    }

    if (showFontSizeDialog) {
        val options = listOf(85 to "Small", 100 to "Default", 115 to "Large", 130 to "Extra Large")
        AlertDialog(
            onDismissRequest = { showFontSizeDialog = false },
            title = { Text("Web Page Font Size") },
            text = {
                Column {
                    options.forEach { (percent, label) ->
                        ListItem(
                            headlineContent = { Text(label) },
                            leadingContent = {
                                RadioButton(
                                    selected = fontScalePercent == percent,
                                    onClick = {
                                        viewModel.setFontScalePercent(percent)
                                        showFontSizeDialog = false
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                viewModel.setFontScalePercent(percent)
                                showFontSizeDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontSizeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showEvictionDialog) {
        val options = listOf(15, 30, 60, 120)
        AlertDialog(
            onDismissRequest = { showEvictionDialog = false },
            title = { Text("Idle Timeout") },
            text = {
                Column {
                    options.forEach { minutes ->
                        ListItem(
                            headlineContent = { Text("$minutes minutes") },
                            leadingContent = {
                                RadioButton(
                                    selected = evictionIdleMinutes == minutes,
                                    onClick = {
                                        viewModel.setEvictionIdleMinutes(minutes)
                                        showEvictionDialog = false
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                viewModel.setEvictionIdleMinutes(minutes)
                                showEvictionDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEvictionDialog = false }) { Text("Close") }
            }
        )
    }

    if (showAccentDialog) {
        val presetColors = listOf(
            "#F44336", "#E91E63", "#9C27B0", "#673AB7",
            "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
            "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
            "#FFEB3B", "#FFC107", "#FF9800", "#795548"
        )
        AlertDialog(
            onDismissRequest = { showAccentDialog = false },
            title = { Text("Accent Color") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Default (Dynamic)") },
                        modifier = Modifier.clickable {
                            viewModel.setAccentColor(null)
                            showAccentDialog = false
                        }
                    )
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(presetColors) { hex ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = Color(android.graphics.Color.parseColor(hex)),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .clickable {
                                        viewModel.setAccentColor(hex)
                                        showAccentDialog = false
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccentDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column {
                    AppThemeMode.entries.forEach { mode ->
                        ListItem(
                            headlineContent = { Text(themeModeLabel(mode)) },
                            leadingContent = {
                                RadioButton(
                                    selected = themeMode == mode,
                                    onClick = {
                                        viewModel.setThemeMode(mode)
                                        showThemeDialog = false
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                viewModel.setThemeMode(mode)
                                showThemeDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun themeModeLabel(mode: AppThemeMode): String = when (mode) {
    AppThemeMode.SYSTEM -> "Follow System"
    AppThemeMode.LIGHT -> "Light"
    AppThemeMode.DARK -> "Dark"
}

private fun fontSizeLabel(percent: Int): String = when (percent) {
    85 -> "Small"
    115 -> "Large"
    130 -> "Extra Large"
    else -> "Default"
}
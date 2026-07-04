package com.web.apps.ui.inspector

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceInspectorScreen(
    onNavigateBack: () -> Unit,
    viewModel: SourceInspectorViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Page Source", "Network Log")

    val pageSource by viewModel.pageSource.collectAsState()
    val resourceLog by viewModel.resourceLog.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPageSource()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Source Inspector") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedTab == 1) {
                        IconButton(onClick = { viewModel.clearResourceLog() }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear network log")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> PageSourceTab(pageSource)
                1 -> NetworkLogTab(resourceLog)
            }
        }
    }
}

@Composable
private fun PageSourceTab(pageSource: String) {
    SelectionContainer {
        Text(
            text = pageSource.ifBlank { "Loading page source..." },
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        )
    }
}

@Composable
private fun NetworkLogTab(resources: List<com.web.apps.core.inspector.InspectedResource>) {
    if (resources.isEmpty()) {
        Text(
            text = "No network requests captured yet for this container.",
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(resources) { resource ->
            Card(modifier = Modifier.padding(8.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "${resource.method}  ${resource.mimeType}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = resource.url,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
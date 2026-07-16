package com.web.apps.ui.logviewer

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var crashEntries by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var debugLogContent by remember { mutableStateOf("") }

    fun refresh() {
        try {
            val resolver = context.contentResolver
            val newCrashEntries = mutableListOf<Pair<String, String>>()

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val projection = arrayOf(
                    android.provider.MediaStore.Downloads._ID,
                    android.provider.MediaStore.Downloads.DISPLAY_NAME,
                    android.provider.MediaStore.Downloads.RELATIVE_PATH
                )

                resolver.query(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection, null, null,
                    "${android.provider.MediaStore.Downloads.DATE_ADDED} DESC"
                )?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Downloads._ID)
                    val nameIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Downloads.DISPLAY_NAME)
                    val pathIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Downloads.RELATIVE_PATH)

                    while (cursor.moveToNext()) {
                        val relativePath = cursor.getString(pathIndex) ?: ""
                        val displayName = cursor.getString(nameIndex) ?: ""

                        if (relativePath.contains("WebApps/crashes")) {
                            val id = cursor.getLong(idIndex)
                            val uri = android.content.ContentUris.withAppendedId(
                                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, id
                            )
                            val content = try {
                                resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                            } catch (e: Exception) {
                                "(failed to read: ${e.message})"
                            }
                            newCrashEntries.add(displayName to content)
                        }

                        if (relativePath.contains("WebApps/debug_logs") && displayName == "debug.txt") {
                            val id = cursor.getLong(idIndex)
                            val uri = android.content.ContentUris.withAppendedId(
                                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, id
                            )
                            debugLogContent = try {
                                resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: "Empty log."
                            } catch (e: Exception) {
                                "Failed to read debug log: ${e.message}"
                            }
                        }
                    }
                }
            }

            crashEntries = newCrashEntries
            if (debugLogContent.isBlank()) debugLogContent = "No debug log found."
        } catch (e: Exception) {
            debugLogContent = "Failed to read logs: ${e.message}"
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Viewer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Crashes") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Debug Log") })
            }

            when (selectedTab) {
                0 -> {
                    if (crashEntries.isEmpty()) {
                        Text("No crash logs found.", modifier = Modifier.padding(16.dp))
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(crashEntries) { (name, content) ->
                                ListItem(
                                    headlineContent = { Text(name) },
                                    supportingContent = {
                                        Text(
                                            content.take(200),
                                            maxLines = 3,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    SelectionContainer {
                        Text(
                            text = debugLogContent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            }
        }
    }
}
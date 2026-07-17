package com.web.apps.ui.bugreport

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.web.apps.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugReportScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var stepsToReproduce by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report a Bug") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Bug Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("What happened?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(top = 12.dp)
            )
            OutlinedTextField(
                value = stepsToReproduce,
                onValueChange = { stepsToReproduce = it },
                label = { Text("Steps to reproduce (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(top = 12.dp)
            )

            Button(
                onClick = {
                    val body = buildString {
                        appendLine("**Description:**")
                        appendLine(description.ifBlank { "_No description provided._" })
                        appendLine()
                        appendLine("**Steps to reproduce:**")
                        appendLine(stepsToReproduce.ifBlank { "_Not provided._" })
                        appendLine()
                        appendLine("**App version:** ${BuildConfig.VERSION_NAME}")
                        appendLine("**Device:** ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                        appendLine("**Android version:** ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
                    }

                    val encodedTitle = java.net.URLEncoder.encode(title.ifBlank { "Bug report" }, "UTF-8")
                    val encodedBody = java.net.URLEncoder.encode(body, "UTF-8")
                    val url = "https://github.com/hastagaming/webapps/issues/new?title=$encodedTitle&body=$encodedBody&labels=bug"

                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    context.startActivity(intent)
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) {
                Text("Open GitHub to Submit")
            }

            Text(
                text = "This will open GitHub in your browser with the details pre-filled. You'll need a GitHub account to submit.",
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
package com.web.apps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.web.apps.service.ContainerForegroundService
import com.web.apps.ui.theme.WebAppsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val requestedContainerId = intent
            .getLongExtra(ContainerForegroundService.EXTRA_CONTAINER_ID, -1L)
            .takeIf { it != -1L }

        setContent {
            WebAppsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WebAppsNavHost(initialContainerId = requestedContainerId)
                }
            }
        }
    }
}
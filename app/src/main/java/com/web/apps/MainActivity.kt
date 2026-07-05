package com.web.apps

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.google.firebase.auth.FirebaseAuth
import com.web.apps.ui.navigation.WebAppsNavHost
import com.web.apps.ui.theme.WebAppsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var isUpdateScreenActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        val initialContainerId = intent.getLongExtra("EXTRA_CONTAINER_ID", -1L).takeIf { it != -1L }

        setContent {
            WebAppsTheme {
                var updateScreenActive by remember { mutableStateOf(false) }
                isUpdateScreenActive = updateScreenActive

                WebAppsNavHost(
                    initialContainerId = initialContainerId,
                    onUpdateScreenActiveChanged = { active ->
                        updateScreenActive = active
                    }
                )
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_APP_SWITCH -> {
                if (isUpdateScreenActive) true else super.onKeyDown(keyCode, event)
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onBackPressed() {
        if (!isUpdateScreenActive) {
            super.onBackPressed()
        }
    }
}
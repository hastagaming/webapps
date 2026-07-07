package com.web.apps

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import com.web.apps.core.auth.GoogleSignInHelper
import com.web.apps.ui.navigation.WebAppsNavHost
import com.web.apps.ui.theme.WebAppsTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var isUpdateScreenActive = false

    @Inject
    lateinit var googleSignInHelper: GoogleSignInHelper

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            googleSignInHelper.handleSignInResult(result.data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        val initialContainerId = intent.getLongExtra("EXTRA_CONTAINER_ID", -1L).takeIf { it != -1L }

        setContent {
            WebAppsTheme {
                WebAppsNavHost(
                    initialContainerId = initialContainerId,
                    onUpdateScreenActiveChanged = { isActive ->
                        isUpdateScreenActive = isActive
                    },
                    onGoogleSignInRequested = { webClientId ->
                        googleSignInHelper.initializeGoogleSignIn(this@MainActivity, webClientId)
                        val intent = googleSignInHelper.getSignInIntent(this@MainActivity)
                        if (intent != null) {
                            googleSignInLauncher.launch(intent)
                        }
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
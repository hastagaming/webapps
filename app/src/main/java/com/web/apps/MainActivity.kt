package com.web.apps

import android.os.Bundle
import android.view.KeyEvent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import com.web.apps.core.auth.GoogleSignInHelper
import com.web.apps.core.auth.GoogleSignInResultBus
import com.web.apps.ui.navigation.WebAppsNavHost
import com.web.apps.ui.theme.WebAppsTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var isUpdateScreenActive = false

    @Inject
    lateinit var googleSignInHelper: GoogleSignInHelper

    @Inject
    lateinit var themePreferenceManager: com.web.apps.core.preferences.ThemePreferenceManager

    @Inject
    lateinit var googleSignInResultBus: GoogleSignInResultBus

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        googleSignInResultBus.emit(if (result.resultCode == RESULT_OK) result.data else null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        val webClientId = getString(R.string.default_web_client_id)
        googleSignInHelper.initializeGoogleSignIn(this, webClientId)

        val initialContainerId = intent.getLongExtra("EXTRA_CONTAINER_ID", -1L).takeIf { it != -1L }

        setContent {
            val themeMode by themePreferenceManager.themeMode.collectAsState(
                initial = com.web.apps.core.preferences.AppThemeMode.SYSTEM
            )
            WebAppsTheme(themeMode = themeMode) {
                WebAppsNavHost(
                    initialContainerId = initialContainerId,
                    onUpdateScreenActiveChanged = { isActive -> isUpdateScreenActive = isActive },
                    onGoogleSignInRequested = {
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
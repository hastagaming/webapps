package com.web.apps

import android.os.Bundle
import android.view.KeyEvent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import android.annotation.SuppressLint
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

    @Inject
    lateinit var appSwitcherTrigger: com.web.apps.core.appswitcher.AppSwitcherTrigger

    @Inject
    lateinit var themePreferenceManager: com.web.apps.core.preferences.ThemePreferenceManager

    @Inject
    lateinit var pluginPreferenceManager: com.web.apps.core.preferences.PluginPreferenceManager

    private val notificationPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { /* granted or not, tidak perlu aksi khusus */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        val webClientId = getString(R.string.default_web_client_id)
        googleSignInHelper.initializeGoogleSignIn(this, webClientId)

        val initialContainerId = intent.getLongExtra("EXTRA_CONTAINER_ID", -1L).takeIf { it != -1L }

        setContent {
            val themeMode by themePreferenceManager.themeMode.collectAsState(
                initial = com.web.apps.core.preferences.AppThemeMode.SYSTEM
            )
            val accentColor by themePreferenceManager.accentColor.collectAsState(initial = null)
            val activeThemePlugin by pluginPreferenceManager.activeThemePlugin.collectAsState(initial = null)
            WebAppsTheme(themeMode = themeMode, accentColorHex = accentColor, activeThemePlugin = activeThemePlugin) {
                androidx.compose.foundation.layout.Box {
                    WebAppsNavHost(
                        initialContainerId = initialContainerId,
                        onUpdateScreenActiveChanged = { isActive -> isUpdateScreenActive = isActive }
                    )

                    val appSwitcherViewModel: com.web.apps.ui.appswitcher.AppSwitcherViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    val switcherState by appSwitcherViewModel.uiState.collectAsState()

                    if (switcherState.isVisible) {
                        com.web.apps.ui.appswitcher.AppSwitcherOverlay(
                            activeContainers = switcherState.activeContainers,
                            onSwitchToContainer = { containerId ->
                                appSwitcherViewModel.onEvent(com.web.apps.ui.appswitcher.AppSwitcherEvent.SwitchToContainer(containerId))
                                val intent = Intent(this@MainActivity, MainActivity::class.java).apply {
                                    putExtra("EXTRA_CONTAINER_ID", containerId)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                }
                                startActivity(intent)
                            },
                            onDismissContainer = { containerId ->
                                appSwitcherViewModel.onEvent(com.web.apps.ui.appswitcher.AppSwitcherEvent.DismissContainer(containerId))
                            },
                            onDismissAll = {
                                appSwitcherViewModel.onEvent(com.web.apps.ui.appswitcher.AppSwitcherEvent.DismissAll)
                            },
                            onClose = {
                                appSwitcherViewModel.onEvent(com.web.apps.ui.appswitcher.AppSwitcherEvent.Hide)
                            }
                        )
                    }
                }
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

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN &&
            event.keyCode == KeyEvent.KEYCODE_TAB &&
            event.isCtrlPressed
        ) {
            appSwitcherTrigger.trigger()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onBackPressed() {
        if (!isUpdateScreenActive) {
            super.onBackPressed()
        }
    }
}
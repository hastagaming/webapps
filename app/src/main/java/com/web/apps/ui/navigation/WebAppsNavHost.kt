@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.web.apps.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.web.apps.core.container.ContainerManager
import com.web.apps.ui.backup.BackupScreen
import com.web.apps.ui.browser.BrowserScreen
import com.web.apps.ui.containerlist.ContainerListScreen
import com.web.apps.ui.containerlock.ContainerLockScreen
import com.web.apps.ui.inspector.SourceInspectorScreen
import com.web.apps.ui.login.LoginScreen
import com.web.apps.ui.permission.PermissionManagerScreen
import com.web.apps.ui.settings.SettingsScreen
import com.web.apps.ui.update.UpdateScreen
import kotlinx.coroutines.launch
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

object WebAppsDestinations {
    const val LOGIN = "login"
    const val CONTAINER_LIST = "container_list"
    const val BROWSER = "browser/{containerId}"
    const val BACKUP = "backup"
    const val LOG_VIEWER = "log_viewer"
    const val QR_EXPORT = "qr_export"
    const val QR_SCAN = "qr_scan"
    const val STATISTICS = "statistics"
    const val CONTAINER_LOCK = "container_lock/{containerId}"
    const val BUG_REPORT = "bug_report"
    const val SOURCE_INSPECTOR = "source_inspector/{containerId}"
    const val PLUGIN_BROWSER = "plugin_browser"
    const val ONBOARDING = "onboarding"
    const val PERMISSION_MANAGER = "permission_manager/{containerId}"
    const val SETTINGS = "settings"
    const val UPDATE_SYSTEM = "update_system"

    fun browserRoute(containerId: Long) = "browser/$containerId"
    fun containerLockRoute(containerId: Long) = "container_lock/$containerId"
    fun sourceInspectorRoute(containerId: Long) = "source_inspector/$containerId"
    fun permissionManagerRoute(containerId: Long) = "permission_manager/$containerId"
}

private const val TRANSITION_DURATION_MS = 300

private fun defaultEnterTransition(): androidx.navigation.NavBackStackEntry.() -> androidx.compose.animation.EnterTransition = {
    slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(TRANSITION_DURATION_MS)
    ) + fadeIn(animationSpec = tween(TRANSITION_DURATION_MS))
}

private fun defaultExitTransition(): androidx.navigation.NavBackStackEntry.() -> androidx.compose.animation.ExitTransition = {
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth / 4 },
        animationSpec = tween(TRANSITION_DURATION_MS)
    ) + fadeOut(animationSpec = tween(TRANSITION_DURATION_MS))
}

private fun defaultPopEnterTransition(): androidx.navigation.NavBackStackEntry.() -> androidx.compose.animation.EnterTransition = {
    slideInHorizontally(
        initialOffsetX = { fullWidth -> -fullWidth / 4 },
        animationSpec = tween(TRANSITION_DURATION_MS)
    ) + fadeIn(animationSpec = tween(TRANSITION_DURATION_MS))
}

private fun defaultPopExitTransition(): androidx.navigation.NavBackStackEntry.() -> androidx.compose.animation.ExitTransition = {
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(TRANSITION_DURATION_MS)
    ) + fadeOut(animationSpec = tween(TRANSITION_DURATION_MS))
}

@Composable
fun WebAppsNavHost(
    navController: NavHostController = rememberNavController(),
    initialContainerId: Long? = null,
    onUpdateScreenActiveChanged: (Boolean) -> Unit = {},
    onGoogleSignInRequested: (String) -> Unit = {}
) {
    val firebaseAuth = FirebaseAuth.getInstance()
    val context = androidx.compose.ui.platform.LocalContext.current
    val onboardingManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            OnboardingEntryPoint::class.java
        ).onboardingPreferenceManager()
    }
    val onboardingCompleted = remember { onboardingManager.isOnboardingCompletedBlocking() }

    val startDestination = when {
        initialContainerId != null -> WebAppsDestinations.browserRoute(initialContainerId)
        firebaseAuth.currentUser != null -> WebAppsDestinations.CONTAINER_LIST
        !onboardingCompleted -> WebAppsDestinations.ONBOARDING
        else -> WebAppsDestinations.LOGIN
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(
            WebAppsDestinations.LOGIN,
            enterTransition = defaultEnterTransition(),
            exitTransition = defaultExitTransition(),
            popEnterTransition = defaultPopEnterTransition(),
            popExitTransition = defaultPopExitTransition()
        ) {
            onUpdateScreenActiveChanged(false)
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(WebAppsDestinations.CONTAINER_LIST) {
                        popUpTo(WebAppsDestinations.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(
            WebAppsDestinations.ONBOARDING,
            enterTransition = defaultEnterTransition(),
            exitTransition = defaultExitTransition(),
            popEnterTransition = defaultPopEnterTransition(),
            popExitTransition = defaultPopExitTransition()
        ) {
            onUpdateScreenActiveChanged(false)
            val onboardingScope = androidx.compose.runtime.rememberCoroutineScope()
            com.web.apps.ui.onboarding.OnboardingScreen(
                onFinished = {
                    onboardingScope.launch {
                        onboardingManager.setOnboardingCompleted()
                    }
                    navController.navigate(WebAppsDestinations.LOGIN) {
                        popUpTo(WebAppsDestinations.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(
            WebAppsDestinations.BUG_REPORT,
            enterTransition = defaultEnterTransition(),
            exitTransition = defaultExitTransition(),
            popEnterTransition = defaultPopEnterTransition(),
            popExitTransition = defaultPopExitTransition()
        ) {
            onUpdateScreenActiveChanged(false)
            com.web.apps.ui.bugreport.BugReportScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            WebAppsDestinations.CONTAINER_LIST,
            enterTransition = defaultEnterTransition(),
            exitTransition = defaultExitTransition(),
            popEnterTransition = defaultPopEnterTransition(),
            popExitTransition = defaultPopExitTransition()
        ) {
            onUpdateScreenActiveChanged(false)
            ContainerListScreen(
                onContainerClick = { containerId ->
                    navController.navigate(WebAppsDestinations.browserRoute(containerId))
                },
                onNavigateToBackup = {
                    navController.navigate(WebAppsDestinations.BACKUP)
                },
                onNavigateToLockSettings = { containerId ->
                    navController.navigate(WebAppsDestinations.containerLockRoute(containerId))
                },
                onNavigateToSettings = {
                    navController.navigate(WebAppsDestinations.SETTINGS)
                },
                onSignOut = {
                    navController.navigate(WebAppsDestinations.LOGIN) {
                        popUpTo(0)
                    }
                }
            )
        }

        composable(
            WebAppsDestinations.LOG_VIEWER,
            enterTransition = defaultEnterTransition(),
            exitTransition = defaultExitTransition(),
            popEnterTransition = defaultPopEnterTransition(),
            popExitTransition = defaultPopExitTransition()
        ) {
            onUpdateScreenActiveChanged(false)
            com.web.apps.ui.logviewer.LogViewerScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = WebAppsDestinations.BROWSER,
            arguments = listOf(navArgument("containerId") { type = NavType.LongType }),
            enterTransition = defaultEnterTransition(),
            exitTransition = defaultExitTransition(),
            popEnterTransition = defaultPopEnterTransition(),
            popExitTransition = defaultPopExitTransition()
        ) {
            onUpdateScreenActiveChanged(false)
            val context = LocalContext.current
            val containerManager = remember {
                EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    ContainerManagerEntryPoint::class.java
                ).containerManager()
            }
            BrowserScreen(
                containerManager = containerManager,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSourceInspector = { containerId ->
                    navController.navigate(WebAppsDestinations.sourceInspectorRoute(containerId))
                },
                onNavigateToPermissionManager = { containerId ->
                    navController.navigate(WebAppsDestinations.permissionManagerRoute(containerId))
                }
            )
        }

        composable(
            WebAppsDestinations.BACKUP,
            enterTransition = defaultEnterTransition(),
            exitTransition = defaultExitTransition(),
            popEnterTransition = defaultPopEnterTransition(),
            popExitTransition = defaultPopExitTransition()
        ) {
            onUpdateScreenActiveChanged(false)
            BackupScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToQrExport = { navController.navigate(WebAppsDestinations.QR_EXPORT) },
                onNavigateToQrScan = { navController.navigate(WebAppsDestinations.QR_SCAN) }
            )
        }

        composable(
            WebAppsDestinations.PLUGIN_BROWSER,
            enterTransition = defaultEnterTransition(),
            exitTransition = defaultExitTransition(),
            popEnterTransition = defaultPopEnterTransition(),
            popExitTransition = defaultPopExitTransition()
        ) {
            onUpdateScreenActiveChanged(false)
            com.web.apps.ui.plugin.PluginBrowserScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            WebAppsDestinations.STATISTICS,
            enterTransition = defaultEnterTransition(),
            exitTransition = defaultExitTransition(),
            popEnterTransition = defaultPopEnterTransition(),
            popExitTransition = defaultPopExitTransition()
        ) {
            onUpdateScreenActiveChanged(false)
            com.web.apps.ui.statistics.StatisticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = WebAppsDestinations.CONTAINER_LOCK,
            arguments = listOf(navArgument("containerId") { type = NavType.LongType }),
            enterTransition = defaultEnterTransition(),
            exitTransition = defaultExitTransition(),
            popEnterTransition = defaultPopEnterTransition(),
            popExitTransition = defaultPopExitTransition()
        ) {
            onUpdateScreenActiveChanged(false)
            ContainerLockScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = WebAppsDestinations.SOURCE_INSPECTOR,
            arguments = listOf(navArgument("containerId") { type = NavType.LongType }),
            enterTransition = defaultEnterTransition(),
            exitTransition = defaultExitTransition(),
            popEnterTransition = defaultPopEnterTransition(),
            popExitTransition = defaultPopExitTransition()
        ) {
            onUpdateScreenActiveChanged(false)
            SourceInspectorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = WebAppsDestinations.PERMISSION_MANAGER,
            arguments = listOf(navArgument("containerId") { type = NavType.LongType }),
            enterTransition = defaultEnterTransition(),
            exitTransition = defaultExitTransition(),
            popEnterTransition = defaultPopEnterTransition(),
            popExitTransition = defaultPopExitTransition()
        ) {
            onUpdateScreenActiveChanged(false)
            PermissionManagerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            WebAppsDestinations.SETTINGS,
            enterTransition = defaultEnterTransition(),
            exitTransition = defaultExitTransition(),
            popEnterTransition = defaultPopEnterTransition(),
            popExitTransition = defaultPopExitTransition()
        ) {
            onUpdateScreenActiveChanged(false)
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToUpdate = { navController.navigate(WebAppsDestinations.UPDATE_SYSTEM) },
                onNavigateToPlugins = { navController.navigate(WebAppsDestinations.PLUGIN_BROWSER) },
                onNavigateToBugReport = { navController.navigate(WebAppsDestinations.BUG_REPORT) },
                onNavigateToLogViewer = { navController.navigate(WebAppsDestinations.LOG_VIEWER) },
                onNavigateToStatistics = { navController.navigate(WebAppsDestinations.STATISTICS) }
            )
        }

        composable(
            WebAppsDestinations.UPDATE_SYSTEM,
            enterTransition = defaultEnterTransition(),
            exitTransition = defaultExitTransition(),
            popEnterTransition = defaultPopEnterTransition(),
            popExitTransition = defaultPopExitTransition()
        ) {
            onUpdateScreenActiveChanged(true)
            UpdateScreen(
                onFinished = {
                    onUpdateScreenActiveChanged(false)
                    navController.popBackStack()
                }
            )
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ContainerManagerEntryPoint {
    fun containerManager(): ContainerManager
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface OnboardingEntryPoint {
    fun onboardingPreferenceManager(): com.web.apps.core.preferences.OnboardingPreferenceManager
}
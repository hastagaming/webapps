package com.web.apps.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
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
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.EntryPointAccessors

@EntryPoint
@InstallIn(ActivityComponent::class)
interface ContainerManagerEntryPoint {
    fun containerManager(): ContainerManager
}

object WebAppsDestinations {
    const val LOGIN = "login"
    const val CONTAINER_LIST = "container_list"
    const val BROWSER = "browser/{containerId}"
    const val BACKUP = "backup"
    const val CONTAINER_LOCK = "container_lock/{containerId}"
    const val SOURCE_INSPECTOR = "source_inspector/{containerId}"
    const val PERMISSION_MANAGER = "permission_manager/{containerId}"
    const val SETTINGS = "settings"
    const val UPDATE_SYSTEM = "update_system"

    fun browserRoute(containerId: Long) = "browser/$containerId"
    fun containerLockRoute(containerId: Long) = "container_lock/$containerId"
    fun sourceInspectorRoute(containerId: Long) = "source_inspector/$containerId"
    fun permissionManagerRoute(containerId: Long) = "permission_manager/$containerId"
}

@Composable
fun WebAppsNavHost(
    initialContainerId: Long? = null,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val containerManager = EntryPoints.get(
        context,
        ContainerManagerEntryPoint::class.java
    ).containerManager()
    val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val startDestination = when {
        initialContainerId != null -> WebAppsDestinations.browserRoute(initialContainerId)
        firebaseAuth.currentUser != null -> WebAppsDestinations.CONTAINER_LIST
        else -> WebAppsDestinations.LOGIN
    }

    NavHost(
          navController = navController,
          startDestination = startDestination
    ) {
        composable(WebAppsDestinations.LOGIN) {
            com.web.apps.ui.login.LoginScreen(
                onLoginSuccess = {
                    navController.navigate(WebAppsDestinations.CONTAINER_LIST) {
                        popUpTo(WebAppsDestinations.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(WebAppsDestinations.CONTAINER_LIST) {
            ContainerListScreen(
                onContainerClick = { containerId ->
                    navController.navigate(WebAppsDestinations.browserRoute(containerId))
                },
                onNavigateToBackup = {
                    navController.navigate(WebAppsDestinations.BACKUP)
                },
                onNavigateToSettings = {
                    navController.navigate(WebAppsDestinations.SETTINGS)
                },
                onNavigateToLockSettings = { containerId ->
                    navController.navigate(WebAppsDestinations.containerLockRoute(containerId))
                },
                onSignOut = {
                    navController.navigate(WebAppsDestinations.LOGIN) {
                        popUpTo(0)
                    }
                }
            )
        }
        composable(WebAppsDestinations.SETTINGS) {
            com.web.apps.ui.settings.SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToUpdate = { navController.navigate(WebAppsDestinations.UPDATE_SYSTEM) }
            )
        }

        composable(WebAppsDestinations.UPDATE_SYSTEM) {
            com.web.apps.ui.update.UpdateScreen(
                onFinished = { navController.popBackStack() }
            )
        }
        composable(
            route = WebAppsDestinations.BROWSER,
            arguments = listOf(navArgument("containerId") { type = androidx.navigation.NavType.LongType })
        ) {
            BrowserScreen(
                containerManager = containerManager,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSourceInspector = { containerId ->
                    navController.navigate(WebAppsDestinations.sourceInspectorRoute(containerId))
                },
                onNavigateToPermissionManager = { containerId ->
                    navController.navigate(WebAppsDestinations.permissionManagerRoute(containerId))
                },
                onNavigateToSwitchedContainer = { containerId ->
                    navController.navigate(WebAppsDestinations.browserRoute(containerId)) {
                        popUpTo(WebAppsDestinations.CONTAINER_LIST)
                    }
                }
            )
        }

        composable(WebAppsDestinations.BACKUP) {
            com.web.apps.ui.backup.BackupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = WebAppsDestinations.CONTAINER_LOCK,
            arguments = listOf(androidx.navigation.navArgument("containerId") {
                type = androidx.navigation.NavType.LongType
            })
        ) {
            com.web.apps.ui.containerlock.ContainerLockScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = WebAppsDestinations.PERMISSION_MANAGER,
            arguments = listOf(androidx.navigation.navArgument("containerId") {
                type = androidx.navigation.NavType.LongType
            })
        ) {
            com.web.apps.ui.permission.PermissionManagerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = WebAppsDestinations.SOURCE_INSPECTOR,
            arguments = listOf(androidx.navigation.navArgument("containerId") {
                type = androidx.navigation.NavType.LongType
            })
        ) {
            com.web.apps.ui.inspector.SourceInspectorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
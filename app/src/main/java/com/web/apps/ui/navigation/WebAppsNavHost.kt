package com.web.apps.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
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
import dagger.hilt.android.EntryPointAccessors

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
    navController: NavHostController = rememberNavController(),
    initialContainerId: Long? = null,
    onUpdateScreenActiveChanged: (Boolean) -> Unit = {}
) {
    val firebaseAuth = FirebaseAuth.getInstance()
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
            onUpdateScreenActiveChanged(false)
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(WebAppsDestinations.CONTAINER_LIST) {
                        popUpTo(WebAppsDestinations.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(WebAppsDestinations.CONTAINER_LIST) {
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
            route = WebAppsDestinations.BROWSER,
            arguments = listOf(navArgument("containerId") { type = NavType.LongType })
        ) {
            onUpdateScreenActiveChanged(false)
            val context = androidx.compose.ui.platform.LocalContext.current
            val containerManager = remember {
                dagger.hilt.android.EntryPointAccessors.fromApplication(
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

        composable(WebAppsDestinations.BACKUP) {
            onUpdateScreenActiveChanged(false)
            BackupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = WebAppsDestinations.CONTAINER_LOCK,
            arguments = listOf(navArgument("containerId") { type = NavType.LongType })
        ) {
            onUpdateScreenActiveChanged(false)
            ContainerLockScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = WebAppsDestinations.SOURCE_INSPECTOR,
            arguments = listOf(navArgument("containerId") { type = NavType.LongType })
        ) {
            onUpdateScreenActiveChanged(false)
            SourceInspectorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = WebAppsDestinations.PERMISSION_MANAGER,
            arguments = listOf(navArgument("containerId") { type = NavType.LongType })
        ) {
            onUpdateScreenActiveChanged(false)
            PermissionManagerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(WebAppsDestinations.SETTINGS) {
            onUpdateScreenActiveChanged(false)
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToUpdate = { navController.navigate(WebAppsDestinations.UPDATE_SYSTEM) }
            )
        }

        composable(WebAppsDestinations.UPDATE_SYSTEM) {
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

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.android.components.SingletonComponent::class)
interface ContainerManagerEntryPoint {
    fun containerManager(): ContainerManager
}
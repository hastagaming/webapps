package com.web.apps.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.web.apps.core.container.ContainerManager
import com.web.apps.ui.browser.BrowserScreen
import com.web.apps.ui.containerlist.ContainerListScreen
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@EntryPoint
@InstallIn(ActivityComponent::class)
interface ContainerManagerEntryPoint {
    fun containerManager(): ContainerManager
}

object WebAppsDestinations {
    const val CONTAINER_LIST = "container_list"
    const val BROWSER = "browser/{containerId}"
    const val BACKUP = "backup"
    const val CONTAINER_LOCK = "container_lock/{containerId}"
    const val SOURCE_INSPECTOR = "source_inspector/{containerId}"
    const val PERMISSION_MANAGER = "permission_manager/{containerId}"

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

    NavHost(
        navController = navController,
        startDestination = if (initialContainerId != null) {
            WebAppsDestinations.browserRoute(initialContainerId)
        } else {
            WebAppsDestinations.CONTAINER_LIST
        }
    ) {
        composable(WebAppsDestinations.CONTAINER_LIST) {
            ContainerListScreen(
                 onContainerClick = { containerId ->
                     navController.navigate(WebAppsDestinations.browserRoute(containerId))
                 },
                 onNavigateToBackup = {
                     navController.navigate(WebAppsDestinations.BACKUP)
                 }
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
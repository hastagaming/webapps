package com.web.apps.di

import com.web.apps.core.container.ContainerManager
import com.web.apps.core.inspector.SourceInspectorManager
import com.web.apps.core.permission.ContainerPermissionManager
import com.web.apps.core.recovery.RecoveryManager
import com.web.apps.core.security.SafeBrowsingChecker
import com.web.apps.core.webview.ContainerWebViewFactory
import com.web.apps.data.repository.ContainerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ContainerModule {

    @Provides
    @Singleton
    fun provideSafeBrowsingChecker(): SafeBrowsingChecker = SafeBrowsingChecker()

    @Provides
    @Singleton
    fun provideContainerWebViewFactory(
        permissionManager: ContainerPermissionManager,
        safeBrowsingChecker: SafeBrowsingChecker,
        sourceInspectorManager: SourceInspectorManager,
        recoveryManager: RecoveryManager,
        notificationHelper: com.web.apps.core.notification.ContainerNotificationHelper,
        themePreferenceManager: com.web.apps.core.preferences.ThemePreferenceManager
    ): ContainerWebViewFactory = ContainerWebViewFactory(
        permissionManager,
        safeBrowsingChecker,
        sourceInspectorManager,
        recoveryManager,
        notificationHelper,
        themePreferenceManager
    )

    @Provides
    @Singleton
    fun provideContainerManager(
        webViewFactory: ContainerWebViewFactory,
        containerRepository: ContainerRepository
    ): ContainerManager = ContainerManager(webViewFactory, containerRepository)
}
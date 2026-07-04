package com.web.apps.di

import com.web.apps.backup.BackupManager
import com.web.apps.core.security.KeystoreManager
import com.web.apps.data.local.dao.ContainerDao
import com.web.apps.data.local.dao.GroupDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackupModule {

    @Provides
    @Singleton
    fun provideKeystoreManager(): KeystoreManager = KeystoreManager()

    @Provides
    @Singleton
    fun provideBackupManager(
        groupDao: GroupDao,
        containerDao: ContainerDao,
        keystoreManager: KeystoreManager
    ): BackupManager = BackupManager(groupDao, containerDao, keystoreManager)
}
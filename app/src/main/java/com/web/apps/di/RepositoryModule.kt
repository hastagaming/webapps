package com.web.apps.di

import com.web.apps.core.sync.SupabaseSyncManager
import com.web.apps.data.local.dao.ContainerDao
import com.web.apps.data.local.dao.DownloadDao
import com.web.apps.data.local.dao.GroupDao
import com.web.apps.data.repository.ContainerRepository
import com.web.apps.data.repository.DownloadRepository
import com.web.apps.data.repository.GroupRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideGroupRepository(
        groupDao: GroupDao,
        supabaseSyncManager: SupabaseSyncManager
    ): GroupRepository = GroupRepository(groupDao, supabaseSyncManager)

    @Provides
    @Singleton
    fun provideContainerRepository(
        containerDao: ContainerDao,
        supabaseSyncManager: SupabaseSyncManager
    ): ContainerRepository = ContainerRepository(containerDao, supabaseSyncManager)

    @Provides
    @Singleton
    fun provideDownloadRepository(downloadDao: DownloadDao): DownloadRepository =
        DownloadRepository(downloadDao)
}
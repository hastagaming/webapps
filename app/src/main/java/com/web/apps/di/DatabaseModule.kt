package com.web.apps.di

import android.content.Context
import androidx.room.Room
import com.web.apps.data.local.dao.ContainerDao
import com.web.apps.data.local.dao.DownloadDao
import com.web.apps.data.local.dao.GroupDao
import com.web.apps.data.local.database.WebAppsDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideWebAppsDatabase(@ApplicationContext context: Context): WebAppsDatabase {
        return Room.databaseBuilder(
            context,
            WebAppsDatabase::class.java,
            WebAppsDatabase.DATABASE_NAME
        )
            .addMigrations(*com.web.apps.data.local.database.DatabaseMigrations.getAllMigrations())
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideGroupDao(database: WebAppsDatabase): GroupDao = database.groupDao()

    @Provides
    fun provideContainerDao(database: WebAppsDatabase): ContainerDao = database.containerDao()

    @Provides
    fun provideDownloadDao(database: WebAppsDatabase): DownloadDao = database.downloadDao()
}
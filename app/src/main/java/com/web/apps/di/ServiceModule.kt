package com.web.apps.di

import android.content.Context
import com.web.apps.service.ContainerServiceController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideContainerServiceController(
        @ApplicationContext context: Context
    ): ContainerServiceController = ContainerServiceController(context)
}
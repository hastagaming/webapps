package com.web.apps.di

import com.web.apps.core.inspector.SourceInspectorManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InspectorModule {

    @Provides
    @Singleton
    fun provideSourceInspectorManager(): SourceInspectorManager = SourceInspectorManager()
}
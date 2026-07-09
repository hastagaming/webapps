package com.web.apps.widget

import com.web.apps.data.repository.ContainerRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun containerRepository(): ContainerRepository
}

fun getContainerRepositoryForWidget(context: android.content.Context): ContainerRepository {
    val appContext = context.applicationContext
    val entryPoint = EntryPointAccessors.fromApplication(appContext, WidgetEntryPoint::class.java)
    return entryPoint.containerRepository()
}
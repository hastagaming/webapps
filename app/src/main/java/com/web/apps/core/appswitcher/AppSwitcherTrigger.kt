package com.web.apps.core.appswitcher

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSwitcherTrigger @Inject constructor() {
    private val _triggers = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val triggers: SharedFlow<Unit> = _triggers

    fun trigger() {
        _triggers.tryEmit(Unit)
    }
}
package com.web.apps.core.auth

import android.content.Intent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleSignInResultBus @Inject constructor() {
    private val _results = MutableSharedFlow<Intent?>(extraBufferCapacity = 1)
    val results = _results.asSharedFlow()

    fun emit(intent: Intent?) {
        _results.tryEmit(intent)
    }
}
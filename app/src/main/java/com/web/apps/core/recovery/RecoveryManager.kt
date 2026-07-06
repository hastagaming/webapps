package com.web.apps.core.recovery

import dagger.Lazy
import com.web.apps.core.container.ContainerManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@Singleton
class RecoveryManager @Inject constructor(
    private val containerManagerLazy: Lazy<com.web.apps.core.container.ContainerManager>
) {
    private val containerManager: com.web.apps.core.container.ContainerManager
        get() = containerManagerLazy.get()
    private val crashCountByContainer = mutableMapOf<Long, Int>()
    private val lastCrashTimestampByContainer = mutableMapOf<Long, Long>()
    private val loadStartTimestampByContainer = mutableMapOf<Long, Long>()
    private val alreadyNotifiedTimeouts = mutableSetOf<Long>()

    private val _recoveryEvents = MutableSharedFlow<RecoveryEvent>(replay = 0, extraBufferCapacity = 8)
    val recoveryEvents: SharedFlow<RecoveryEvent> = _recoveryEvents.asSharedFlow()

    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        private const val CRASH_LOOP_THRESHOLD = 3
        private const val CRASH_LOOP_WINDOW_MILLIS = 60_000L
        private const val LOAD_TIMEOUT_MILLIS = 30_000L
        private const val MONITOR_INTERVAL_MILLIS = 5_000L
    }

    init {
        monitorScope.launch {
            while (true) {
                delay(MONITOR_INTERVAL_MILLIS)
                checkLoadTimeouts()
            }
        }
    }

    fun onRenderProcessGone(containerId: Long): Boolean {
        val now = System.currentTimeMillis()
        val lastCrash = lastCrashTimestampByContainer[containerId] ?: 0L
        val currentCount = if (now - lastCrash <= CRASH_LOOP_WINDOW_MILLIS) {
            (crashCountByContainer[containerId] ?: 0) + 1
        } else {
            1
        }

        crashCountByContainer[containerId] = currentCount
        lastCrashTimestampByContainer[containerId] = now

        return if (currentCount >= CRASH_LOOP_THRESHOLD) {
            emitEvent(
                RecoveryEvent(
                    containerId = containerId,
                    reason = RecoveryReason.CRASH_LOOP,
                    message = "This container has crashed $currentCount times in the last minute."
                )
            )
            true
        } else {
            emitEvent(
                RecoveryEvent(
                    containerId = containerId,
                    reason = RecoveryReason.RENDER_PROCESS_GONE,
                    message = "This container's render process stopped unexpectedly."
                )
            )
            containerManager.stopContainer(containerId)
            false
        }
    }

    fun onPageLoadStarted(containerId: Long) {
        loadStartTimestampByContainer[containerId] = System.currentTimeMillis()
        alreadyNotifiedTimeouts.remove(containerId)
    }

    fun onPageLoadFinished(containerId: Long) {
        loadStartTimestampByContainer.remove(containerId)
        alreadyNotifiedTimeouts.remove(containerId)
    }

    private fun checkLoadTimeouts() {
        val now = System.currentTimeMillis()
        loadStartTimestampByContainer.forEach { (containerId, startedAt) ->
            val alreadyNotified = alreadyNotifiedTimeouts.contains(containerId)
            if (!alreadyNotified && now - startedAt > LOAD_TIMEOUT_MILLIS) {
                alreadyNotifiedTimeouts.add(containerId)
                emitEvent(
                    RecoveryEvent(
                        containerId = containerId,
                        reason = RecoveryReason.LOAD_TIMEOUT,
                        message = "This container has been loading for more than 30 seconds."
                    )
                )
            }
        }
    }

    fun triggerManualRecovery(containerId: Long) {
        emitEvent(
            RecoveryEvent(
                containerId = containerId,
                reason = RecoveryReason.MANUAL_TRIGGER,
                message = "Manual recovery requested by the user."
            )
        )
    }

    fun performSoftReset(containerId: Long) {
        containerManager.stopContainer(containerId)
        crashCountByContainer.remove(containerId)
        lastCrashTimestampByContainer.remove(containerId)
        loadStartTimestampByContainer.remove(containerId)
        alreadyNotifiedTimeouts.remove(containerId)
    }

    private fun emitEvent(event: RecoveryEvent) {
        _recoveryEvents.tryEmit(event)
    }
}
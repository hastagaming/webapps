package com.web.apps.core.recovery

enum class RecoveryReason {
    CRASH_LOOP,
    LOAD_TIMEOUT,
    RENDER_PROCESS_GONE,
    MANUAL_TRIGGER
}

data class RecoveryEvent(
    val containerId: Long,
    val reason: RecoveryReason,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String
)
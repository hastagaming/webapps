package com.web.apps.ui.containerlock

data class ContainerLockUiState(
    val isLocked: Boolean = false,
    val step: LockSetupStep = LockSetupStep.IDLE,
    val firstPinEntry: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

enum class LockSetupStep {
    IDLE,
    ENTER_NEW_PIN,
    CONFIRM_NEW_PIN,
    ENTER_CURRENT_PIN_TO_REMOVE
}

sealed class ContainerLockEvent {
    object StartSetup : ContainerLockEvent()
    data class SubmitPin(val pin: String) : ContainerLockEvent()
    object CancelSetup : ContainerLockEvent()
    object StartRemoveLock : ContainerLockEvent()
    object DismissMessage : ContainerLockEvent()
}
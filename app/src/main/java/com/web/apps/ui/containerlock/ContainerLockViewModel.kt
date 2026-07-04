package com.web.apps.ui.containerlock

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.web.apps.data.repository.ContainerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MIN_PIN_LENGTH = 4
private const val MAX_PIN_LENGTH = 8

@HiltViewModel
class ContainerLockViewModel @Inject constructor(
    private val containerRepository: ContainerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val containerId: Long = checkNotNull(savedStateHandle["containerId"])

    private val _uiState = MutableStateFlow(ContainerLockUiState())
    val uiState: StateFlow<ContainerLockUiState> = _uiState.asStateFlow()

    init {
        containerRepository.observeContainerById(containerId)
            .onEach { container ->
                if (container != null) {
                    _uiState.value = _uiState.value.copy(isLocked = container.isLocked)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: ContainerLockEvent) {
        when (event) {
            is ContainerLockEvent.StartSetup -> startSetup()
            is ContainerLockEvent.SubmitPin -> submitPin(event.pin)
            is ContainerLockEvent.CancelSetup -> cancelSetup()
            is ContainerLockEvent.StartRemoveLock -> startRemoveLock()
            is ContainerLockEvent.DismissMessage -> dismissMessage()
        }
    }

    private fun startSetup() {
        _uiState.value = _uiState.value.copy(
            step = LockSetupStep.ENTER_NEW_PIN,
            firstPinEntry = "",
            errorMessage = null,
            successMessage = null
        )
    }

    private fun startRemoveLock() {
        _uiState.value = _uiState.value.copy(
            step = LockSetupStep.ENTER_CURRENT_PIN_TO_REMOVE,
            errorMessage = null,
            successMessage = null
        )
    }

    private fun cancelSetup() {
        _uiState.value = _uiState.value.copy(
            step = LockSetupStep.IDLE,
            firstPinEntry = "",
            errorMessage = null
        )
    }

    private fun submitPin(pin: String) {
        if (pin.length < MIN_PIN_LENGTH || pin.length > MAX_PIN_LENGTH) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "PIN must be between $MIN_PIN_LENGTH and $MAX_PIN_LENGTH digits."
            )
            return
        }
        if (!pin.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "PIN must contain digits only."
            )
            return
        }

        when (_uiState.value.step) {
            LockSetupStep.ENTER_NEW_PIN -> {
                _uiState.value = _uiState.value.copy(
                    step = LockSetupStep.CONFIRM_NEW_PIN,
                    firstPinEntry = pin,
                    errorMessage = null
                )
            }

            LockSetupStep.CONFIRM_NEW_PIN -> {
                if (pin == _uiState.value.firstPinEntry) {
                    viewModelScope.launch {
                        containerRepository.lockContainer(containerId, pin)
                        _uiState.value = _uiState.value.copy(
                            isLocked = true,
                            step = LockSetupStep.IDLE,
                            firstPinEntry = "",
                            errorMessage = null,
                            successMessage = "Container lock has been enabled."
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        step = LockSetupStep.ENTER_NEW_PIN,
                        firstPinEntry = "",
                        errorMessage = "PINs did not match. Please try again."
                    )
                }
            }

            LockSetupStep.ENTER_CURRENT_PIN_TO_REMOVE -> {
                viewModelScope.launch {
                    val success = containerRepository.unlockContainer(containerId, pin)
                    if (success) {
                        _uiState.value = _uiState.value.copy(
                            isLocked = false,
                            step = LockSetupStep.IDLE,
                            errorMessage = null,
                            successMessage = "Container lock has been removed."
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Incorrect PIN. Please try again."
                        )
                    }
                }
            }

            LockSetupStep.IDLE -> Unit
        }
    }

    private fun dismissMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
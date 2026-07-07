package com.web.apps.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.web.apps.core.auth.GoogleSignInResultBus
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import com.web.apps.core.auth.GoogleSignInHelper
import com.web.apps.core.auth.GoogleSignInResult
import com.web.apps.data.repository.AuthRepository
import com.web.apps.data.repository.AuthResult
import com.web.apps.data.repository.PasswordResetResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MIN_PASSWORD_LENGTH = 6

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleSignInHelper: GoogleSignInHelper,
    private val googleSignInResultBus: GoogleSignInResultBus,
    @ApplicationContext private val appContext: android.content.Context
) : ViewModel() {

    init {
        googleSignInResultBus.results
            .onEach { intent -> handleGoogleSignInResult(intent) }
            .launchIn(viewModelScope)
    }

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> {
                _uiState.value = _uiState.value.copy(email = event.value, errorMessage = null)
            }
            is LoginEvent.PasswordChanged -> {
                _uiState.value = _uiState.value.copy(password = event.value, errorMessage = null)
            }
            is LoginEvent.ToggleMode -> {
                _uiState.value = _uiState.value.copy(
                    isRegisterMode = !_uiState.value.isRegisterMode,
                    errorMessage = null,
                    infoMessage = null
                )
            }
            is LoginEvent.SubmitEmailAuth -> submitEmailAuth()
            is LoginEvent.SignInWithGoogle -> Unit
            is LoginEvent.ForgotPassword -> sendPasswordReset()
            is LoginEvent.DismissMessage -> {
                _uiState.value = _uiState.value.copy(errorMessage = null, infoMessage = null)
            }
        }
    }

    private fun submitEmailAuth() {
        com.web.apps.core.crash.DebugLogger.log(appContext, "WebAppsAuth", "submitEmailAuth called")
        val state = _uiState.value
        val validationError = validateInput(state.email, state.password)
        com.web.apps.core.crash.DebugLogger.log(appContext, "WebAppsAuth", "Validation error: $validationError")
        if (validationError != null) {
            _uiState.value = state.copy(errorMessage = validationError)
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            com.web.apps.core.crash.DebugLogger.log(appContext, "WebAppsAuth", "Coroutine launched, calling repository")
            val result = if (state.isRegisterMode) {
                authRepository.registerWithEmail(state.email.trim(), state.password)
            } else {
                authRepository.signInWithEmail(state.email.trim(), state.password)
            }
            com.web.apps.core.crash.DebugLogger.log(appContext, "WebAppsAuth", "Repository returned: $result")
            handleAuthResult(result)
        }
    }

    fun signInWithGoogle(context: Context, webClientId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        googleSignInHelper.initializeGoogleSignIn(context, webClientId)
        val signInIntent = googleSignInHelper.getSignInIntent(context)
        if (signInIntent != null) {
            (context as? androidx.activity.ComponentActivity)?.startActivityForResult(signInIntent, GOOGLE_SIGN_IN_REQUEST_CODE)
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Failed to initialize Google Sign-In."
            )
        }
    }

    companion object {
        private const val GOOGLE_SIGN_IN_REQUEST_CODE = 9001
    }

    fun handleGoogleSignInResult(data: android.content.Intent?) {
        viewModelScope.launch {
            when (val result = googleSignInHelper.handleSignInResult(data)) {
                is GoogleSignInResult.Success -> {
                    val authResult = authRepository.signInWithGoogleIdToken(result.idToken)
                    handleAuthResult(authResult)
                }
                is GoogleSignInResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                is GoogleSignInResult.Cancelled -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    private fun handleAuthResult(result: AuthResult) {
        com.web.apps.core.crash.DebugLogger.log(appContext, "WebAppsAuth", "handleAuthResult called: $result")
        when (result) {
            is AuthResult.Success -> {
                com.web.apps.core.crash.DebugLogger.log(appContext, "WebAppsAuth", "Success! User: ${result.user.email}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    errorMessage = null
                )
            }
            is AuthResult.Failure -> {
                com.web.apps.core.crash.DebugLogger.log(appContext, "WebAppsAuth", "Failure: ${result.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
            }
        }
    }

    private fun sendPasswordReset() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please enter your email address first."
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            when (val result = authRepository.sendPasswordResetEmail(email)) {
                is PasswordResetResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        infoMessage = "A password reset link has been sent to $email."
                    )
                }
                is PasswordResetResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    private fun validateInput(email: String, password: String): String? {
        if (email.isBlank()) return "Please enter your email address."
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Please enter a valid email address."
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            return "Password must be at least $MIN_PASSWORD_LENGTH characters."
        }
        return null
    }
}
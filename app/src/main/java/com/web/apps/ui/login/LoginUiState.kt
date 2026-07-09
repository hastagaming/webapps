package com.web.apps.ui.login

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isRegisterMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isAuthenticated: Boolean = false
)

sealed class LoginEvent {
    data class EmailChanged(val value: String) : LoginEvent()
    data class PasswordChanged(val value: String) : LoginEvent()
    object ToggleMode : LoginEvent()
    object SubmitEmailAuth : LoginEvent()
    object SignInWithGoogleSilent : LoginEvent()
    object SignInWithGoogleInteractive : LoginEvent()
    object ForgotPassword : LoginEvent()
    object DismissMessage : LoginEvent()
}
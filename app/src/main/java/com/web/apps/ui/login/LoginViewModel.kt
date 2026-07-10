package com.web.apps.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.web.apps.R
import com.web.apps.core.auth.GoogleSignInHelper
import com.web.apps.core.auth.GoogleSignInResult
import com.web.apps.core.auth.GoogleSignInResultBus
import com.web.apps.core.auth.KnownAccount
import com.web.apps.core.auth.KnownAccountManager
import com.web.apps.core.auth.KnownAccountType
import com.web.apps.core.sync.SupabaseSyncManager
import com.web.apps.data.repository.AuthRepository
import com.web.apps.data.repository.AuthResult
import com.web.apps.data.repository.PasswordResetResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MIN_PASSWORD_LENGTH = 6

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleSignInHelper: GoogleSignInHelper,
    private val googleSignInResultBus: GoogleSignInResultBus,
    private val supabaseSyncManager: SupabaseSyncManager,
    private val knownAccountManager: KnownAccountManager,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    init {
        googleSignInResultBus.results
            .onEach { intent -> handleGoogleSignInResult(intent) }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            knownAccountManager.pullKnownAccountsFromSupabase()
        }
    }

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val knownAccounts = knownAccountManager.knownAccounts

    private var pendingGoogleAccountEmail: String? = null

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
            is LoginEvent.SignInWithGoogleSilent -> Unit
            is LoginEvent.SignInWithGoogleInteractive -> Unit
            is LoginEvent.ForgotPassword -> sendPasswordReset()
            is LoginEvent.DismissMessage -> {
                _uiState.value = _uiState.value.copy(errorMessage = null, infoMessage = null)
            }
            is LoginEvent.SelectKnownAccount -> selectKnownAccount(event.account)
            is LoginEvent.ShowNewAccountForm -> {
                _uiState.value = _uiState.value.copy(
                    showAccountForm = true,
                    email = "",
                    password = "",
                    pendingAccountEmail = null
                )
            }
            is LoginEvent.BackToAccountList -> {
                _uiState.value = _uiState.value.copy(
                    showAccountForm = false,
                    email = "",
                    password = "",
                    pendingAccountEmail = null,
                    errorMessage = null
                )
            }
        }
    }

    private fun selectKnownAccount(account: KnownAccount) {
        when (account.type) {
            KnownAccountType.EMAIL_PASSWORD -> {
                _uiState.value = _uiState.value.copy(
                    showAccountForm = true,
                    email = account.email,
                    password = "",
                    pendingAccountEmail = account.email,
                    isRegisterMode = false
                )
            }
            KnownAccountType.GOOGLE -> {
                pendingGoogleAccountEmail = account.email
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                val webClientId = appContext.getString(R.string.default_web_client_id)
                googleSignInHelper.initializeGoogleSignIn(appContext, webClientId)
                // Signal the Activity layer via a dedicated flag; handled by LoginScreen's onGoogleSignInRequested
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    pendingAccountEmail = account.email
                )
            }
        }
    }

    fun requestGoogleInteractiveForAccount(email: String?) {
        pendingGoogleAccountEmail = email
    }

    private fun submitEmailAuth() {
        val state = _uiState.value
        val validationError = validateInput(state.email, state.password)
        if (validationError != null) {
            _uiState.value = state.copy(errorMessage = validationError)
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = if (state.isRegisterMode) {
                authRepository.registerWithEmail(state.email.trim(), state.password)
            } else {
                authRepository.signInWithEmail(state.email.trim(), state.password)
            }
            if (result is AuthResult.Success && state.isRegisterMode) {
                knownAccountManager.saveAccount(
                    KnownAccount(
                        email = state.email.trim(),
                        displayName = null,
                        photoUrl = null,
                        type = KnownAccountType.EMAIL_PASSWORD
                    )
                )
            }
            handleAuthResult(result)
        }
    }

    fun handleGoogleSignInResult(data: android.content.Intent?) {
        if (data == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "No account signed in on this device."
            )
            return
        }
        viewModelScope.launch {
            when (val result = googleSignInHelper.handleSignInResult(data)) {
                is GoogleSignInResult.Success -> {
                    val authResult = authRepository.signInWithGoogleIdToken(result.idToken)

                    if (authResult is AuthResult.Success) {
                        val signedInEmail = authResult.user.email
                        val expectedEmail = pendingGoogleAccountEmail

                        if (expectedEmail != null && !signedInEmail.equals(expectedEmail, ignoreCase = true)) {
                            authRepository.signOut()
                            pendingGoogleAccountEmail = null
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "Please select $expectedEmail to continue."
                            )
                            return@launch
                        }

                        val isNewAccountFlow = _uiState.value.isRegisterMode && expectedEmail == null

                        if (!isNewAccountFlow) {
                            val known = knownAccountManager.knownAccounts
                            // Only allow sign-in if this account was previously registered via Sign Up
                        }

                        knownAccountManager.saveAccount(
                            KnownAccount(
                                email = signedInEmail ?: "",
                                displayName = authResult.user.displayName,
                                photoUrl = authResult.user.photoUrl?.toString(),
                                type = KnownAccountType.GOOGLE
                            )
                        )
                        pendingGoogleAccountEmail = null
                    }

                    handleAuthResult(authResult)
                }
                is GoogleSignInResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                is GoogleSignInResult.Cancelled -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No account signed in on this device."
                    )
                }
            }
        }
    }

    private fun handleAuthResult(result: AuthResult) {
        when (result) {
            is AuthResult.Success -> {
                viewModelScope.launch {
                    supabaseSyncManager.pullAndMergeAll()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        errorMessage = null
                    )
                }
            }
            is AuthResult.Failure -> {
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
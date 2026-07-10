package com.web.apps.ui.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.web.apps.R
import com.web.apps.core.auth.KnownAccount
import com.web.apps.core.auth.KnownAccountType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoogleSignInRequested: (String) -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val knownAccounts by viewModel.knownAccounts.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var passwordVisible by remember { mutableStateOf(false) }

    val webClientId = context.getString(R.string.default_web_client_id)

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onLoginSuccess()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onEvent(LoginEvent.DismissMessage)
        }
    }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onEvent(LoginEvent.DismissMessage)
        }
    }

    LaunchedEffect(uiState.pendingAccountEmail) {
        val email = uiState.pendingAccountEmail
        if (email != null && !uiState.showAccountForm) {
            // Google account tapped from list -> trigger the real Google popup
            onGoogleSignInRequested(webClientId)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        if (!uiState.showAccountForm && knownAccounts.isNotEmpty()) {
            AccountPickerContent(
                accounts = knownAccounts,
                isLoading = uiState.isLoading,
                onAccountSelected = { account ->
                    viewModel.onEvent(LoginEvent.SelectKnownAccount(account))
                },
                onUseDifferentAccount = {
                    viewModel.onEvent(LoginEvent.ShowNewAccountForm)
                },
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LoginFormContent(
                uiState = uiState,
                showBackButton = knownAccounts.isNotEmpty(),
                passwordVisible = passwordVisible,
                onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                onEvent = { viewModel.onEvent(it) },
                onGoogleSignInClick = {
                    viewModel.requestGoogleInteractiveForAccount(null)
                    onGoogleSignInRequested(webClientId)
                },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun AccountPickerContent(
    accounts: List<KnownAccount>,
    isLoading: Boolean,
    onAccountSelected: (KnownAccount) -> Unit,
    onUseDifferentAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "WebApps",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "Choose an account",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        accounts.forEach { account ->
            ListItem(
                headlineContent = { Text(account.displayName ?: account.email) },
                supportingContent = { Text(account.email) },
                leadingContent = {
                    if (account.photoUrl != null) {
                        AsyncImage(
                            model = account.photoUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                },
                modifier = Modifier.clickable(enabled = !isLoading) { onAccountSelected(account) }
            )
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onUseDifferentAccount,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Use a different account")
        }
    }
}

@Composable
private fun LoginFormContent(
    uiState: LoginUiState,
    showBackButton: Boolean,
    passwordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    onEvent: (LoginEvent) -> Unit,
    onGoogleSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "WebApps",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = if (uiState.isRegisterMode) "Create a new account" else "Sign in to continue",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
        )

        OutlinedTextField(
            value = uiState.email,
            onValueChange = { onEvent(LoginEvent.EmailChanged(it)) },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
            singleLine = true,
            readOnly = uiState.pendingAccountEmail != null,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.password,
            onValueChange = { onEvent(LoginEvent.PasswordChanged(it)) },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = onPasswordVisibilityToggle) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )

        if (!uiState.isRegisterMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { onEvent(LoginEvent.ForgotPassword) }) {
                    Text("Forgot Password?")
                }
            }
        }

        Button(
            onClick = { onEvent(LoginEvent.SubmitEmailAuth) },
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(top = 8.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(if (uiState.isRegisterMode) "Create Account" else "Sign In")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(text = "  OR  ", style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        OutlinedButton(
            onClick = onGoogleSignInClick,
            enabled = !uiState.isLoading,
            colors = ButtonDefaults.outlinedButtonColors(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(if (uiState.isRegisterMode) "Sign Up with Google" else "Continue with Google")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (uiState.isRegisterMode) "Already have an account?" else "Don't have an account?",
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = { onEvent(LoginEvent.ToggleMode) }) {
                Text(if (uiState.isRegisterMode) "Sign In" else "Sign Up")
            }
        }

        if (showBackButton) {
            TextButton(onClick = { onEvent(LoginEvent.BackToAccountList) }) {
                Text("Back to account list")
            }
        }
    }
}
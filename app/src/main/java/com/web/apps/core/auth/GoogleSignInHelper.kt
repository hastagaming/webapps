package com.web.apps.core.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import javax.inject.Inject
import javax.inject.Singleton

sealed class GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult()
    data class Failure(val message: String) : GoogleSignInResult()
    object Cancelled : GoogleSignInResult()
}

@Singleton
class GoogleSignInHelper @Inject constructor() {

    suspend fun requestGoogleIdToken(
        context: Context,
        webClientId: String
    ): GoogleSignInResult {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val response = credentialManager.getCredential(context, request)
            val credential = response.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleSignInResult.Success(googleIdTokenCredential.idToken)
            } else {
                GoogleSignInResult.Failure("Unexpected credential type received.")
            }
        } catch (e: GetCredentialException) {
            if (e.type == "android.credentials.GetCredentialException.TYPE_USER_CANCELED") {
                GoogleSignInResult.Cancelled
            } else {
                GoogleSignInResult.Failure(e.message ?: "Google sign-in was cancelled or failed.")
            }
        } catch (e: GoogleIdTokenParsingException) {
            GoogleSignInResult.Failure("Failed to parse Google ID token.")
        }
    }
}
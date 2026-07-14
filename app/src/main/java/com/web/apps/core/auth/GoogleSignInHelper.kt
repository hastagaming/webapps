package com.web.apps.core.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
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

    suspend fun signIn(context: Context, webClientId: String, filtered: Boolean): GoogleSignInResult {
        return try {
            val credentialManager = CredentialManager.create(context)

            val signInOption = GetSignInWithGoogleOption.Builder(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val response = credentialManager.getCredential(context, request)
            val credential = response.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleSignInResult.Success(googleIdTokenCredential.idToken)
            } else {
                GoogleSignInResult.Failure("Unexpected credential type.")
            }
        } catch (e: NoCredentialException) {
            GoogleSignInResult.Failure("No account signed in on this device.")
        } catch (e: GetCredentialException) {
            if (e.type == "androidx.credentials.TYPE_USER_CANCELED") {
                GoogleSignInResult.Cancelled
            } else {
                GoogleSignInResult.Failure(e.message ?: "Google Sign-In failed.")
            }
        } catch (e: GoogleIdTokenParsingException) {
            GoogleSignInResult.Failure("Failed to parse Google ID token.")
        } catch (e: Exception) {
            GoogleSignInResult.Failure(e.message ?: "Google Sign-In failed.")
        }
    }
}
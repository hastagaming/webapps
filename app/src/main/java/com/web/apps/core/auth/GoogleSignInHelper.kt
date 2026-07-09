package com.web.apps.core.auth

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.web.apps.R
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult()
    data class Failure(val message: String) : GoogleSignInResult()
    object Cancelled : GoogleSignInResult()
}

@Singleton
class GoogleSignInHelper @Inject constructor() {

    private var googleSignInClient: GoogleSignInClient? = null

    fun initializeGoogleSignIn(context: Context, webClientId: String) {
        if (googleSignInClient == null) {
            val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(context, gsoBuilder)
        }
    }

    fun signOut(context: Context) {
        googleSignInClient?.signOut()
    }

    fun getSignInIntent(context: Context): android.content.Intent? {
        return googleSignInClient?.signInIntent
    }

    suspend fun silentSignIn(context: Context, webClientId: String): GoogleSignInResult {
        return try {
            initializeGoogleSignIn(context, webClientId)
            val client = googleSignInClient
                ?: return GoogleSignInResult.Failure("No account signed in on this device.")

            val account = client.silentSignIn().await()
            val idToken = account.idToken
            if (idToken != null) {
                GoogleSignInResult.Success(idToken)
            } else {
                GoogleSignInResult.Failure("No account signed in on this device.")
            }
        } catch (e: Exception) {
            GoogleSignInResult.Failure("No account signed in on this device.")
        }
    }

    fun handleSignInResult(data: android.content.Intent?): GoogleSignInResult {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                GoogleSignInResult.Success(idToken)
            } else {
                GoogleSignInResult.Failure("ID token is null. Please try again.")
            }
        } catch (e: ApiException) {
            when (e.statusCode) {
                12501 -> GoogleSignInResult.Cancelled
                else -> GoogleSignInResult.Failure(e.message ?: "Google Sign-In failed.")
            }
        }
    }
}
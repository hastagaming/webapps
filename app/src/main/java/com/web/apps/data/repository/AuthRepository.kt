package com.web.apps.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult {
    data class Success(val user: FirebaseUser, val isNewUser: Boolean = false) : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

sealed class PasswordResetResult {
    object Success : PasswordResetResult()
    data class Failure(val message: String) : PasswordResetResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return AuthResult.Failure("Sign-in failed. Please try again.")
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Failure(mapAuthErrorMessage(e))
        }
    }

    suspend fun registerWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return AuthResult.Failure("Account creation failed. Please try again.")
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Failure(mapAuthErrorMessage(e))
        }
    }

    suspend fun signInWithGoogleIdToken(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user ?: return AuthResult.Failure("Google sign-in failed. Please try again.")
            val isNewUser = result.additionalUserInfo?.isNewUser ?: false
            AuthResult.Success(user, isNewUser)
        } catch (e: Exception) {
            AuthResult.Failure(mapAuthErrorMessage(e))
        }
    }

    suspend fun sendPasswordResetEmail(email: String): PasswordResetResult {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            PasswordResetResult.Success
        } catch (e: Exception) {
            PasswordResetResult.Failure(mapAuthErrorMessage(e))
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    private fun mapAuthErrorMessage(e: Exception): String {
        return when {
            e.message?.contains("password is invalid", ignoreCase = true) == true ->
                "Incorrect password. Please try again."
            e.message?.contains("no user record", ignoreCase = true) == true ->
                "Cannot enter until the account sign up."
            e.message?.contains("email address is already in use", ignoreCase = true) == true ->
                "This email address is already registered."
            e.message?.contains("badly formatted", ignoreCase = true) == true ->
                "Please enter a valid email address."
            e.message?.contains("network error", ignoreCase = true) == true ->
                "Network error. Please check your internet connection."
            e.message?.contains("weak password", ignoreCase = true) == true ->
                "Password is too weak. Use at least 6 characters."
            else -> e.message ?: "Something went wrong. Please try again."
        }
    }
}
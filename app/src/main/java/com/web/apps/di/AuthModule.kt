package com.web.apps.di

import com.google.firebase.auth.FirebaseAuth
import com.web.apps.data.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(firebaseAuth: FirebaseAuth): AuthRepository = AuthRepository(firebaseAuth)

    @Provides
    @Singleton
    fun provideGoogleSignInHelper(): GoogleSignInHelper = GoogleSignInHelper()
}
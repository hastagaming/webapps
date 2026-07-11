package com.web.apps.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://ouaxmljxepgvgwgxrcil.supabase.co",
        supabaseKey = "nEbxCD7wstWIJz2Q"
    ) {
        defaultSerializer = io.github.jan.supabase.serializer.KotlinXSerializer(
            kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
        )
        install(Postgrest)
    }
}
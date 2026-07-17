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
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im91YXhtbGp4ZXBndmd3Z3hyY2lsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODM1MDMxNDQsImV4cCI6MjA5OTA3OTE0NH0.KtLMnEIIQiwq2t73ctRpQmXB9BSNBkYKY7VbIqmYcuo"
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
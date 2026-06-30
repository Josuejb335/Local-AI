package com.localai.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // TODO: Add application-scoped dependencies here.
    // Examples:
    // - DataStore preferences for model configuration
    // - Model download / cache manager
    // - Application context providers
}

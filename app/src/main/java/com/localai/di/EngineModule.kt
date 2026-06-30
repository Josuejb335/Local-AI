package com.localai.di

import com.localai.data.engine.LlamaCppEngineImpl
import com.localai.data.repository.AIModelRepositoryImpl
import com.localai.domain.engine.AIModelEngine
import com.localai.domain.repository.AIModelRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EngineModule {

    @Binds
    @Singleton
    abstract fun bindAiModelEngine(
        impl: LlamaCppEngineImpl
    ): AIModelEngine

    @Binds
    @Singleton
    abstract fun bindAiModelRepository(
        impl: AIModelRepositoryImpl
    ): AIModelRepository

    // TODO: Uncomment when SpeechToTextEngine implementation is ready:
    // @Binds
    // @Singleton
    // abstract fun bindSpeechToTextEngine(
    //     impl: YourSttEngineImpl
    // ): SpeechToTextEngine

    // TODO: Uncomment when TextToSpeechEngine implementation is ready:
    // @Binds
    // @Singleton
    // abstract fun bindTextToSpeechEngine(
    //     impl: YourTtsEngineImpl
    // ): TextToSpeechEngine
}

package com.localai.di;

import com.localai.data.engine.LlamaCppEngineImpl;
import com.localai.data.repository.AIModelRepositoryImpl;
import com.localai.domain.engine.AIModelEngine;
import com.localai.domain.repository.AIModelRepository;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@dagger.Module()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b\'\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\'J\u0010\u0010\u0007\u001a\u00020\b2\u0006\u0010\u0005\u001a\u00020\tH\'\u00a8\u0006\n"}, d2 = {"Lcom/localai/di/EngineModule;", "", "()V", "bindAiModelEngine", "Lcom/localai/domain/engine/AIModelEngine;", "impl", "Lcom/localai/data/engine/LlamaCppEngineImpl;", "bindAiModelRepository", "Lcom/localai/domain/repository/AIModelRepository;", "Lcom/localai/data/repository/AIModelRepositoryImpl;", "app_debug"})
@dagger.hilt.InstallIn(value = {dagger.hilt.components.SingletonComponent.class})
public abstract class EngineModule {
    
    public EngineModule() {
        super();
    }
    
    @dagger.Binds()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public abstract com.localai.domain.engine.AIModelEngine bindAiModelEngine(@org.jetbrains.annotations.NotNull()
    com.localai.data.engine.LlamaCppEngineImpl impl);
    
    @dagger.Binds()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public abstract com.localai.domain.repository.AIModelRepository bindAiModelRepository(@org.jetbrains.annotations.NotNull()
    com.localai.data.repository.AIModelRepositoryImpl impl);
}
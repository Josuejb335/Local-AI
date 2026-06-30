package com.localai.domain.repository;

import com.localai.domain.model.DownloadState;
import com.localai.domain.model.ModelInfo;
import kotlinx.coroutines.flow.Flow;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\b\u0004\bf\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a6@\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\b2\u0006\u0010\n\u001a\u00020\u000bH&J\u0014\u0010\f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000b0\r0\bH&J\u0014\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000b0\rH\u00a6@\u00a2\u0006\u0002\u0010\u000fJ\u0018\u0010\u0010\u001a\u0004\u0018\u00010\u00052\u0006\u0010\u0004\u001a\u00020\u0005H\u00a6@\u00a2\u0006\u0002\u0010\u0006\u00a8\u0006\u0011"}, d2 = {"Lcom/localai/domain/repository/AIModelRepository;", "", "deleteModel", "", "modelId", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "downloadModel", "Lkotlinx/coroutines/flow/Flow;", "Lcom/localai/domain/model/DownloadState;", "modelInfo", "Lcom/localai/domain/model/ModelInfo;", "fetchRemoteModels", "", "getLocalModels", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getLocalPath", "app_debug"})
public abstract interface AIModelRepository {
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.localai.domain.model.ModelInfo>> fetchRemoteModels();
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<com.localai.domain.model.DownloadState> downloadModel(@org.jetbrains.annotations.NotNull()
    com.localai.domain.model.ModelInfo modelInfo);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getLocalModels(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.localai.domain.model.ModelInfo>> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getLocalPath(@org.jetbrains.annotations.NotNull()
    java.lang.String modelId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteModel(@org.jetbrains.annotations.NotNull()
    java.lang.String modelId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}
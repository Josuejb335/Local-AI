package com.localai.data.repository;

import android.content.Context;
import android.util.Log;
import com.localai.domain.model.DownloadState;
import com.localai.domain.model.ModelInfo;
import com.localai.domain.repository.AIModelRepository;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.Flow;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000X\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\t\n\u0002\b\n\b\u0007\u0018\u0000 %2\u00020\u0001:\u0001%B\u0019\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eH\u0096@\u00a2\u0006\u0002\u0010\u000fJ\u0016\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00120\u00112\u0006\u0010\u0013\u001a\u00020\u0014H\u0016J\u0012\u0010\u0015\u001a\u0004\u0018\u00010\u00162\u0006\u0010\r\u001a\u00020\u000eH\u0002J\u000e\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00140\u0018H\u0002J\u0014\u0010\u0019\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00140\u00180\u0011H\u0016J\u001e\u0010\u001a\u001a\u0010\u0012\u0004\u0012\u00020\u000e\u0012\u0004\u0012\u00020\u001c\u0018\u00010\u001b2\u0006\u0010\u001d\u001a\u00020\u0016H\u0002J\u0018\u0010\u001e\u001a\u00020\u000e2\u0006\u0010\u001f\u001a\u00020\u001c2\u0006\u0010 \u001a\u00020\u001cH\u0002J\u000e\u0010!\u001a\b\u0012\u0004\u0012\u00020\u00140\u0018H\u0002J\u0014\u0010\"\u001a\b\u0012\u0004\u0012\u00020\u00140\u0018H\u0096@\u00a2\u0006\u0002\u0010#J\u0018\u0010$\u001a\u0004\u0018\u00010\u000e2\u0006\u0010\r\u001a\u00020\u000eH\u0096@\u00a2\u0006\u0002\u0010\u000fR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0007\u001a\u00020\b8BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\t\u0010\nR\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006&"}, d2 = {"Lcom/localai/data/repository/AIModelRepositoryImpl;", "Lcom/localai/domain/repository/AIModelRepository;", "context", "Landroid/content/Context;", "okHttpClient", "Lokhttp3/OkHttpClient;", "(Landroid/content/Context;Lokhttp3/OkHttpClient;)V", "modelsDir", "Ljava/io/File;", "getModelsDir", "()Ljava/io/File;", "deleteModel", "", "modelId", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "downloadModel", "Lkotlinx/coroutines/flow/Flow;", "Lcom/localai/domain/model/DownloadState;", "modelInfo", "Lcom/localai/domain/model/ModelInfo;", "fetchModelDetail", "Lorg/json/JSONObject;", "fetchModelsFromHuggingFace", "", "fetchRemoteModels", "findBestGgufFile", "Lkotlin/Pair;", "", "modelDetail", "formatDescription", "fileSize", "downloads", "getFallbackModels", "getLocalModels", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getLocalPath", "Companion", "app_debug"})
public final class AIModelRepositoryImpl implements com.localai.domain.repository.AIModelRepository {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final okhttp3.OkHttpClient okHttpClient = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "AIModelRepository";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String HF_API_URL = "https://huggingface.co/api/models?filter=gguf&sort=downloads&direction=-1&limit=50&expand[]=siblings";
    private static final long MAX_FILE_SIZE_BYTES = 2000000000L;
    @org.jetbrains.annotations.NotNull()
    private static final java.util.List<java.lang.String> PREFERRED_QUANT_PATTERNS = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.localai.data.repository.AIModelRepositoryImpl.Companion Companion = null;
    
    @javax.inject.Inject()
    public AIModelRepositoryImpl(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    okhttp3.OkHttpClient okHttpClient) {
        super();
    }
    
    private final java.io.File getModelsDir() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<java.util.List<com.localai.domain.model.ModelInfo>> fetchRemoteModels() {
        return null;
    }
    
    private final java.util.List<com.localai.domain.model.ModelInfo> fetchModelsFromHuggingFace() {
        return null;
    }
    
    private final org.json.JSONObject fetchModelDetail(java.lang.String modelId) {
        return null;
    }
    
    private final kotlin.Pair<java.lang.String, java.lang.Long> findBestGgufFile(org.json.JSONObject modelDetail) {
        return null;
    }
    
    private final java.lang.String formatDescription(long fileSize, long downloads) {
        return null;
    }
    
    private final java.util.List<com.localai.domain.model.ModelInfo> getFallbackModels() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<com.localai.domain.model.DownloadState> downloadModel(@org.jetbrains.annotations.NotNull()
    com.localai.domain.model.ModelInfo modelInfo) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getLocalModels(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.localai.domain.model.ModelInfo>> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getLocalPath(@org.jetbrains.annotations.NotNull()
    java.lang.String modelId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object deleteModel(@org.jetbrains.annotations.NotNull()
    java.lang.String modelId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010 \n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00040\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\n"}, d2 = {"Lcom/localai/data/repository/AIModelRepositoryImpl$Companion;", "", "()V", "HF_API_URL", "", "MAX_FILE_SIZE_BYTES", "", "PREFERRED_QUANT_PATTERNS", "", "TAG", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}
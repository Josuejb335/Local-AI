package com.localai.presentation.model;

import androidx.lifecycle.ViewModel;
import com.localai.domain.model.DownloadState;
import com.localai.domain.model.ModelInfo;
import com.localai.domain.repository.AIModelRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010$\n\u0002\u0018\u0002\n\u0002\b\u0013\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001BM\u0012\u000e\b\u0002\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u0012\b\b\u0002\u0010\u0005\u001a\u00020\u0006\u0012\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\b\u0012\u0014\b\u0002\u0010\t\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\u000b0\n\u0012\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\b\u00a2\u0006\u0002\u0010\rJ\u000f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00c6\u0003J\t\u0010\u0017\u001a\u00020\u0006H\u00c6\u0003J\u000b\u0010\u0018\u001a\u0004\u0018\u00010\bH\u00c6\u0003J\u0015\u0010\u0019\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\u000b0\nH\u00c6\u0003J\u000b\u0010\u001a\u001a\u0004\u0018\u00010\bH\u00c6\u0003JQ\u0010\u001b\u001a\u00020\u00002\u000e\b\u0002\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\b2\u0014\b\u0002\u0010\t\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\u000b0\n2\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\bH\u00c6\u0001J\u0013\u0010\u001c\u001a\u00020\u00062\b\u0010\u001d\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u001e\u001a\u00020\u001fH\u00d6\u0001J\t\u0010 \u001a\u00020\bH\u00d6\u0001R\u001d\u0010\t\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\u000b0\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0010R\u0013\u0010\u0007\u001a\u0004\u0018\u00010\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0017\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014R\u0013\u0010\f\u001a\u0004\u0018\u00010\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0012\u00a8\u0006!"}, d2 = {"Lcom/localai/presentation/model/ModelManagerUiState;", "", "remoteModels", "", "Lcom/localai/domain/model/ModelInfo;", "isLoadingRemote", "", "remoteError", "", "downloadStates", "", "Lcom/localai/domain/model/DownloadState;", "selectedModelPath", "(Ljava/util/List;ZLjava/lang/String;Ljava/util/Map;Ljava/lang/String;)V", "getDownloadStates", "()Ljava/util/Map;", "()Z", "getRemoteError", "()Ljava/lang/String;", "getRemoteModels", "()Ljava/util/List;", "getSelectedModelPath", "component1", "component2", "component3", "component4", "component5", "copy", "equals", "other", "hashCode", "", "toString", "app_debug"})
public final class ModelManagerUiState {
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.localai.domain.model.ModelInfo> remoteModels = null;
    private final boolean isLoadingRemote = false;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String remoteError = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, com.localai.domain.model.DownloadState> downloadStates = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String selectedModelPath = null;
    
    public ModelManagerUiState(@org.jetbrains.annotations.NotNull()
    java.util.List<com.localai.domain.model.ModelInfo> remoteModels, boolean isLoadingRemote, @org.jetbrains.annotations.Nullable()
    java.lang.String remoteError, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, ? extends com.localai.domain.model.DownloadState> downloadStates, @org.jetbrains.annotations.Nullable()
    java.lang.String selectedModelPath) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.localai.domain.model.ModelInfo> getRemoteModels() {
        return null;
    }
    
    public final boolean isLoadingRemote() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getRemoteError() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Map<java.lang.String, com.localai.domain.model.DownloadState> getDownloadStates() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getSelectedModelPath() {
        return null;
    }
    
    public ModelManagerUiState() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.localai.domain.model.ModelInfo> component1() {
        return null;
    }
    
    public final boolean component2() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Map<java.lang.String, com.localai.domain.model.DownloadState> component4() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component5() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.localai.presentation.model.ModelManagerUiState copy(@org.jetbrains.annotations.NotNull()
    java.util.List<com.localai.domain.model.ModelInfo> remoteModels, boolean isLoadingRemote, @org.jetbrains.annotations.Nullable()
    java.lang.String remoteError, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, ? extends com.localai.domain.model.DownloadState> downloadStates, @org.jetbrains.annotations.Nullable()
    java.lang.String selectedModelPath) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
}
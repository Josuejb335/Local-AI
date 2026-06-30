package com.localai.presentation.model;

import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.material3.CardDefaults;
import androidx.compose.material3.ExperimentalMaterial3Api;
import androidx.compose.material3.TopAppBarDefaults;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextOverflow;
import com.localai.domain.model.DownloadState;
import com.localai.domain.model.ModelInfo;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u00004\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\u001a<\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\b\u0010\u0004\u001a\u0004\u0018\u00010\u00052\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\u0012\u0010\b\u001a\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u00010\tH\u0003\u001a5\u0010\u000b\u001a\u00020\u00012!\u0010\f\u001a\u001d\u0012\u0013\u0012\u00110\n\u00a2\u0006\f\b\r\u0012\b\b\u000e\u0012\u0004\b\b(\u000f\u0012\u0004\u0012\u00020\u00010\t2\b\b\u0002\u0010\u0010\u001a\u00020\u0011H\u0007\u00a8\u0006\u0012"}, d2 = {"ModelCard", "", "modelInfo", "Lcom/localai/domain/model/ModelInfo;", "downloadState", "Lcom/localai/domain/model/DownloadState;", "onDownload", "Lkotlin/Function0;", "onUse", "Lkotlin/Function1;", "", "ModelDownloaderScreen", "onNavigateToChat", "Lkotlin/ParameterName;", "name", "modelPath", "viewModel", "Lcom/localai/presentation/model/ModelManagerViewModel;", "app_debug"})
public final class ModelDownloaderScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void ModelDownloaderScreen(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onNavigateToChat, @org.jetbrains.annotations.NotNull()
    com.localai.presentation.model.ModelManagerViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable()
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    private static final void ModelCard(com.localai.domain.model.ModelInfo modelInfo, com.localai.domain.model.DownloadState downloadState, kotlin.jvm.functions.Function0<kotlin.Unit> onDownload, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onUse) {
    }
}
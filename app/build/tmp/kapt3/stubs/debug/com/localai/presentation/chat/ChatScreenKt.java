package com.localai.presentation.chat;

import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.foundation.text.KeyboardOptions;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.ExperimentalMaterial3Api;
import androidx.compose.material3.IconButtonDefaults;
import androidx.compose.material3.SnackbarHostState;
import androidx.compose.material3.TopAppBarDefaults;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.input.ImeAction;
import com.localai.domain.model.ChatMessage;
import com.localai.domain.model.MessageRole;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u00004\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\u001a\u0010\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u0003H\u0003\u001a(\u0010\u0004\u001a\u00020\u00012\u0006\u0010\u0005\u001a\u00020\u00062\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\b\b\u0002\u0010\t\u001a\u00020\nH\u0007\u001a:\u0010\u000b\u001a\u00020\u00012\u0006\u0010\f\u001a\u00020\u00062\u0012\u0010\r\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u000e2\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\u0006\u0010\u0010\u001a\u00020\u0011H\u0003\u001a\b\u0010\u0012\u001a\u00020\u0001H\u0003\u00a8\u0006\u0013"}, d2 = {"ChatBubble", "", "message", "Lcom/localai/domain/model/ChatMessage;", "ChatScreen", "modelPath", "", "onNavigateBack", "Lkotlin/Function0;", "viewModel", "Lcom/localai/presentation/chat/ChatViewModel;", "InputBar", "text", "onTextChange", "Lkotlin/Function1;", "onSend", "enabled", "", "TypingIndicator", "app_debug"})
public final class ChatScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void ChatScreen(@org.jetbrains.annotations.NotNull()
    java.lang.String modelPath, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onNavigateBack, @org.jetbrains.annotations.NotNull()
    com.localai.presentation.chat.ChatViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ChatBubble(com.localai.domain.model.ChatMessage message) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void InputBar(java.lang.String text, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onTextChange, kotlin.jvm.functions.Function0<kotlin.Unit> onSend, boolean enabled) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void TypingIndicator() {
    }
}
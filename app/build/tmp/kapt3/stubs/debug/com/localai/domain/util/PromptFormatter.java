package com.localai.domain.util;

import com.localai.domain.model.ChatMessage;
import com.localai.domain.model.MessageRole;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0004\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\b\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\t\u001a\u00020\u00062\u0006\u0010\n\u001a\u00020\u0004J2\u0010\u000b\u001a\u00020\u00042\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000e0\r2\b\b\u0002\u0010\u000f\u001a\u00020\u00102\b\b\u0002\u0010\u0011\u001a\u00020\u00042\b\b\u0002\u0010\u0012\u001a\u00020\u0006J\u001e\u0010\u0013\u001a\u00020\u00042\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000e0\r2\u0006\u0010\u0011\u001a\u00020\u0004H\u0002J\u001e\u0010\u0014\u001a\u00020\u00042\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000e0\r2\u0006\u0010\u0011\u001a\u00020\u0004H\u0002J\u0016\u0010\u0015\u001a\u00020\u00042\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000e0\rH\u0002J*\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u000e0\r2\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000e0\r2\u0006\u0010\u0011\u001a\u00020\u00042\u0006\u0010\u0017\u001a\u00020\u0006R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0018"}, d2 = {"Lcom/localai/domain/util/PromptFormatter;", "", "()V", "DEFAULT_SYSTEM_PROMPT", "", "SAFETY_MARGIN_TOKENS", "", "TOKEN_ESTIMATE_PER_CHAR", "", "estimateTokenCount", "text", "format", "messages", "", "Lcom/localai/domain/model/ChatMessage;", "template", "Lcom/localai/domain/util/PromptTemplate;", "systemPrompt", "maxContextTokens", "formatChatML", "formatLlama3", "formatRaw", "trimToMaxTokens", "maxTokens", "app_debug"})
public final class PromptFormatter {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String DEFAULT_SYSTEM_PROMPT = "You are a helpful AI assistant running entirely on-device. Answer concisely and accurately.";
    private static final float TOKEN_ESTIMATE_PER_CHAR = 0.3F;
    private static final int SAFETY_MARGIN_TOKENS = 256;
    @org.jetbrains.annotations.NotNull()
    public static final com.localai.domain.util.PromptFormatter INSTANCE = null;
    
    private PromptFormatter() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String format(@org.jetbrains.annotations.NotNull()
    java.util.List<com.localai.domain.model.ChatMessage> messages, @org.jetbrains.annotations.NotNull()
    com.localai.domain.util.PromptTemplate template, @org.jetbrains.annotations.NotNull()
    java.lang.String systemPrompt, int maxContextTokens) {
        return null;
    }
    
    public final int estimateTokenCount(@org.jetbrains.annotations.NotNull()
    java.lang.String text) {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.localai.domain.model.ChatMessage> trimToMaxTokens(@org.jetbrains.annotations.NotNull()
    java.util.List<com.localai.domain.model.ChatMessage> messages, @org.jetbrains.annotations.NotNull()
    java.lang.String systemPrompt, int maxTokens) {
        return null;
    }
    
    private final java.lang.String formatChatML(java.util.List<com.localai.domain.model.ChatMessage> messages, java.lang.String systemPrompt) {
        return null;
    }
    
    private final java.lang.String formatLlama3(java.util.List<com.localai.domain.model.ChatMessage> messages, java.lang.String systemPrompt) {
        return null;
    }
    
    private final java.lang.String formatRaw(java.util.List<com.localai.domain.model.ChatMessage> messages) {
        return null;
    }
}
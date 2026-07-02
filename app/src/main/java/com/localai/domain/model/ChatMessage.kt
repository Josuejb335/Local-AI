package com.localai.domain.model

data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val thinkingContent: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

package com.localai.presentation.chat

import com.localai.domain.model.ChatMessage

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentInput: String = "",
    val isGenerating: Boolean = false,
    val isModelLoaded: Boolean = false,
    val error: String? = null
)

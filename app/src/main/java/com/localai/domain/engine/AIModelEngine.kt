package com.localai.domain.engine

import com.localai.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface AIModelEngine {
    suspend fun initialize(localPath: String): Result<Unit>
    suspend fun generate(messages: List<ChatMessage>): Flow<String>
    suspend fun release()
}

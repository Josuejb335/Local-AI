package com.localai.domain.engine

import com.localai.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface AIModelEngine {
    suspend fun initialize(localPath: String): Result<Unit>

    /**
     * Generate a response as a flow of [GenerationEvent]s.
     *
     * Events include visible content tokens, thinking block indicators,
     * and thinking content. The consumer can decide how to display each type.
     */
    suspend fun generate(messages: List<ChatMessage>): Flow<GenerationEvent>

    suspend fun release()
}

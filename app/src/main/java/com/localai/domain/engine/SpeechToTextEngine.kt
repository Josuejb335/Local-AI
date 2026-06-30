package com.localai.domain.engine

import kotlinx.coroutines.flow.Flow

interface SpeechToTextEngine {
    suspend fun startListening(): Flow<String>
    suspend fun stopListening()
    suspend fun release()
}

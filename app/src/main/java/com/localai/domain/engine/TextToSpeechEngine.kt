package com.localai.domain.engine

interface TextToSpeechEngine {
    suspend fun speak(text: String)
    suspend fun stop()
    suspend fun release()
}

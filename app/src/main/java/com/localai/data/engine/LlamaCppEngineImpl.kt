package com.localai.data.engine

import android.util.Log
import com.localai.data.network.LlamaCppJniBridge
import com.localai.domain.engine.AIModelEngine
import com.localai.domain.model.ChatMessage
import com.localai.domain.util.PromptFormatter
import com.localai.domain.util.PromptTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlamaCppEngineImpl @Inject constructor() : AIModelEngine {

    private var nativeContextPtr: Long = 0L
    private var loadedLocalPath: String = ""
    private var isInitialized = false
    private var useNative = false

    override suspend fun initialize(localPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        loadedLocalPath = localPath
        if (!LlamaCppJniBridge.nativeLoaded) {
            Log.w(TAG, "Native libraries not loaded, falling back to mock")
            return@withContext fallbackInit()
        }
        try {
            val ptr = LlamaCppJniBridge.loadModelNative(localPath)
            if (ptr == 0L) {
                return@withContext Result.failure(
                    Exception("Failed to load model — corrupted or incompatible GGUF file: $localPath")
                )
            }
            nativeContextPtr = ptr
            useNative = true
            isInitialized = true
            Log.i(TAG, "Native model loaded: $localPath")
            Result.success(Unit)
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native library not found, falling back to mock", e)
            fallbackInit()
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OOM loading model", e)
            return@withContext Result.failure(
                Exception("Device ran out of memory. Try a smaller quantisation.")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize model", e)
            return@withContext Result.failure(e)
        }
    }

    override suspend fun generate(messages: List<ChatMessage>): Flow<String> {
        if (!isInitialized) {
            throw IllegalStateException("Model not initialized.")
        }
        return if (useNative) {
            nativeGenerate(messages)
        } else {
            mockGenerate(messages)
        }
    }

    private fun nativeGenerate(messages: List<ChatMessage>): Flow<String> = flow {
        val template = if (loadedLocalPath.contains("Llama", ignoreCase = true) ||
            loadedLocalPath.contains("llama", ignoreCase = true))
            PromptTemplate.LLAMA_3 else PromptTemplate.CHATML

        val prompt = PromptFormatter.format(
            messages = messages,
            template = template,
            maxContextTokens = 2048
        )

        Log.d(TAG, "Prompt: ${prompt.length} chars, ~${PromptFormatter.estimateTokenCount(prompt)} tokens")

        // First token: start generation with the prompt
        var token = withContext(Dispatchers.IO) {
            LlamaCppJniBridge.generateStreamingTokenNative(nativeContextPtr, prompt)
        }

        // Subsequent tokens: continue generation (null prompt)
        while (token != null) {
            emit(token)
            if (!currentCoroutineContext().isActive) break
            token = withContext(Dispatchers.IO) {
                LlamaCppJniBridge.generateStreamingTokenNative(nativeContextPtr, null)
            }
        }
    }

    override suspend fun release(): Unit = withContext(Dispatchers.IO) {
        if (useNative && nativeContextPtr != 0L) {
            try {
                LlamaCppJniBridge.freeModelNative(nativeContextPtr)
                Log.i(TAG, "Native model released")
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing native model", e)
            }
        }
        nativeContextPtr = 0L
        loadedLocalPath = ""
        isInitialized = false
        useNative = false
    }

    // ------------------------------------------------------------------
    // Mock fallback
    // ------------------------------------------------------------------

    private suspend fun fallbackInit(): Result<Unit> {
        delay(500)
        isInitialized = true
        useNative = false
        Log.i(TAG, "Mock engine initialized")
        return Result.success(Unit)
    }

    private fun mockGenerate(messages: List<ChatMessage>): Flow<String> = flow {
        val lastMessage = messages.lastOrNull { it.role == com.localai.domain.model.MessageRole.USER }
        val input = lastMessage?.content ?: ""
        val preamble = "\u2592 Mock inference: $loadedLocalPath\n\n"
        for (ch in preamble) {
            delay(12)
            emit(ch.toString())
        }
        val words = input.split("\\s+".toRegex()).filter { it.length > 3 }
        emit("Processing")
        delay(30)
        emit(" \"$input\"")
        delay(30)
        if (words.isNotEmpty()) {
            emit(".\n\nContext: ${messages.size} messages in history")
        }
        for (w in words) {
            delay(40)
            emit(" $w")
        }
    }

    companion object {
        private const val TAG = "LlamaCpp"
    }
}

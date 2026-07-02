package com.localai.data.engine

import android.util.Log
import com.localai.data.network.LlamaCppJniBridge
import com.localai.domain.engine.AIModelEngine
import com.localai.domain.engine.GenerationEvent
import com.localai.domain.model.ChatMessage
import com.localai.domain.util.PromptFormatter
import com.localai.domain.util.PromptTemplate
import com.localai.domain.util.SpecialTokenFilter
import kotlinx.coroutines.Dispatchers
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
    private var contextSize: Int = DEFAULT_CONTEXT_TOKENS

    override suspend fun initialize(localPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (isInitialized && nativeContextPtr != 0L) {
            release()
        }
        loadedLocalPath = localPath
        if (!LlamaCppJniBridge.nativeLoaded) {
            Log.e(TAG, "Native libraries not loaded for this device architecture")
            val detail = LlamaCppJniBridge.lastLoadError?.let { " Details: $it" } ?: ""
            return@withContext Result.failure(
                Exception(
                    "Native inference engine not available. The required native libraries " +
                    "(libllama.so) could not be loaded for this device architecture. " +
                    "Please ensure the app was built with native libraries for your device." + detail
                )
            )
        }
        try {
            val ptr = LlamaCppJniBridge.loadModelNative(localPath)
            if (ptr == 0L) {
                return@withContext Result.failure(
                    Exception("Failed to load model - corrupted or incompatible GGUF file: $localPath")
                )
            }
            nativeContextPtr = ptr
            isInitialized = true


            // Query the actual context size from the native layer
            contextSize = LlamaCppJniBridge.getContextSizeNative(ptr)
            if (contextSize <= 0) contextSize = DEFAULT_CONTEXT_TOKENS
            Log.i(TAG, "Native model loaded: $localPath (context=$contextSize tokens)")

            Result.success(Unit)
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native library link error", e)
            return@withContext Result.failure(
                Exception(
                    "Native inference library failed to link. The native libraries may be " +
                    "incompatible with this device. Error: ${e.message}"
                )
            )
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

    override suspend fun generate(messages: List<ChatMessage>): Flow<GenerationEvent> {
        if (!isInitialized) {
            throw IllegalStateException("Model not initialized. Call initialize() first.")
        }
        return nativeGenerate(messages)
    }

    /**
     * Get the actual token count for text using the model's tokenizer.
     * Falls back to the character-based estimate if native call fails.
     */
    private fun getActualTokenCount(text: String): Int {
        if (nativeContextPtr == 0L) return PromptFormatter.estimateTokenCount(text)
        val count = LlamaCppJniBridge.tokenizeCountNative(nativeContextPtr, text)
        return if (count >= 0) count else PromptFormatter.estimateTokenCount(text)
    }

    private fun nativeGenerate(messages: List<ChatMessage>): Flow<GenerationEvent> = flow {
        val templates = templateCandidatesForModel(loadedLocalPath)

        // Reserve tokens for generation output (at least 256, up to 1/4 of context)
        val reserveForGeneration = (contextSize / 4).coerceIn(MIN_GENERATION_TOKENS, contextSize / 2)
        val maxPromptTokens = contextSize - reserveForGeneration

        for (template in templates) {
            val prompt = PromptFormatter.format(
                messages = messages,
                template = template,
                maxContextTokens = maxPromptTokens
            )

            // Use actual tokenizer to verify the prompt fits
            val actualPromptTokens = withContext(Dispatchers.IO) {
                getActualTokenCount(prompt)
            }

            Log.d(
                TAG,
                "Trying template=${template.label} prompt=${prompt.length} chars " +
                "(actual=$actualPromptTokens tokens, limit=$maxPromptTokens, ctx=$contextSize)"
            )


            // If prompt is still too large after formatting, skip this template
            if (actualPromptTokens > contextSize - MIN_GENERATION_TOKENS) {
                Log.w(TAG, "Prompt ($actualPromptTokens tokens) exceeds safe limit, trying next template")
                continue
            }

            // CRITICAL: Reset context before each generation attempt.
            // This clears the KV cache and prevents crashes from stale state.
            withContext(Dispatchers.IO) {
                LlamaCppJniBridge.resetContextNative(nativeContextPtr)
            }

            val filter = SpecialTokenFilter.StreamingFilter()
            var hasVisibleOutput = false
            var hasAnyOutput = false

            // First token: start generation with the prompt
            var token: String? = try {
                withContext(Dispatchers.IO) {
                    LlamaCppJniBridge.generateStreamingTokenNative(nativeContextPtr, prompt)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Native generation failed on prompt submission", e)
                null
            }

            // Subsequent tokens: continue generation (null prompt)
            while (token != null) {
                if (token.isNotEmpty()) {
                    val results = filter.process(token)
                    for (result in results) {
                        when (result) {
                            is SpecialTokenFilter.FilterResult.Content -> {
                                hasVisibleOutput = true
                                hasAnyOutput = true
                                emit(GenerationEvent.Content(result.text))
                            }
                            is SpecialTokenFilter.FilterResult.ThinkingStart -> {
                                hasAnyOutput = true
                                emit(GenerationEvent.ThinkingStarted)
                            }
                            is SpecialTokenFilter.FilterResult.ThinkingContent -> {
                                hasAnyOutput = true
                                emit(GenerationEvent.ThinkingContent(result.text))
                            }
                            is SpecialTokenFilter.FilterResult.ThinkingEnd -> {
                                hasAnyOutput = true
                                emit(GenerationEvent.ThinkingEnded)
                            }
                            is SpecialTokenFilter.FilterResult.Stop -> {
                                val flushed = filter.flush()
                                for (f in flushed) {
                                    when (f) {
                                        is SpecialTokenFilter.FilterResult.Content -> {
                                            hasVisibleOutput = true
                                            emit(GenerationEvent.Content(f.text))
                                        }
                                        is SpecialTokenFilter.FilterResult.ThinkingContent -> {
                                            emit(GenerationEvent.ThinkingContent(f.text))
                                        }
                                        else -> { /* ignore */ }
                                    }
                                }
                                if (hasVisibleOutput || hasAnyOutput) return@flow
                                break
                            }
                            is SpecialTokenFilter.FilterResult.Consumed -> {
                                hasAnyOutput = true
                            }
                            is SpecialTokenFilter.FilterResult.Buffered -> {
                                // Token is being buffered for partial match
                            }
                        }
                    }
                }
                if (!currentCoroutineContext().isActive) break
                token = try {
                    withContext(Dispatchers.IO) {
                        LlamaCppJniBridge.generateStreamingTokenNative(nativeContextPtr, null)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Native generation failed during token streaming", e)
                    null
                }
            }


            // Flush any remaining buffered content
            val flushed = filter.flush()
            for (result in flushed) {
                when (result) {
                    is SpecialTokenFilter.FilterResult.Content -> {
                        hasVisibleOutput = true
                        emit(GenerationEvent.Content(result.text))
                    }
                    is SpecialTokenFilter.FilterResult.ThinkingContent -> {
                        emit(GenerationEvent.ThinkingContent(result.text))
                    }
                    else -> { /* ignore */ }
                }
            }

            if (hasVisibleOutput || hasAnyOutput) {
                return@flow
            }

            Log.w(TAG, "Template ${template.label} produced empty output, trying fallback")
        }

        throw IllegalStateException(
            "Model produced no visible output. This usually means the chat template does not match the model."
        )
    }

    private fun templateCandidatesForModel(modelPath: String): List<PromptTemplate> {
        val name = modelPath.substringAfterLast('/').lowercase()
        val first = when {
            "llama-3" in name || "llama3" in name -> PromptTemplate.LLAMA_3
            "qwen" in name || "smol" in name || "mistral" in name || "zephyr" in name || "chatml" in name -> PromptTemplate.CHATML
            else -> PromptTemplate.CHATML
        }
        return listOf(first, PromptTemplate.CHATML, PromptTemplate.LLAMA_3, PromptTemplate.RAW).distinct()
    }

    override suspend fun release(): Unit = withContext(Dispatchers.IO) {
        if (nativeContextPtr != 0L) {
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
        contextSize = DEFAULT_CONTEXT_TOKENS
    }

    companion object {
        private const val TAG = "LlamaCpp"
        private const val DEFAULT_CONTEXT_TOKENS = 2048
        private const val MIN_GENERATION_TOKENS = 256
    }
}

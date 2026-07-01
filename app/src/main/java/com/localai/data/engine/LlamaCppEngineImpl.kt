package com.localai.data.engine

import android.util.Log
import com.localai.data.network.LlamaCppJniBridge
import com.localai.domain.engine.AIModelEngine
import com.localai.domain.model.ChatMessage
import com.localai.domain.util.PromptFormatter
import com.localai.domain.util.PromptTemplate
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
            Log.i(TAG, "Native model loaded: $localPath")
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

    override suspend fun generate(messages: List<ChatMessage>): Flow<String> {
        if (!isInitialized) {
            throw IllegalStateException("Model not initialized. Call initialize() first.")
        }
        return nativeGenerate(messages)
    }

    private fun nativeGenerate(messages: List<ChatMessage>): Flow<String> = flow {
        val templates = templateCandidatesForModel(loadedLocalPath)
        for (template in templates) {
            val prompt = PromptFormatter.format(
                messages = messages,
                template = template,
                maxContextTokens = MAX_CONTEXT_TOKENS
            )

            Log.d(
                TAG,
                "Trying template=${template.label} prompt=${prompt.length} chars (~${PromptFormatter.estimateTokenCount(prompt)} tokens)"
            )

            var hasVisibleOutput = false
            val pendingOutput = StringBuilder()

            // First token: start generation with the prompt
            var token = withContext(Dispatchers.IO) {
                LlamaCppJniBridge.generateStreamingTokenNative(nativeContextPtr, prompt)
            }

            // Subsequent tokens: continue generation (null prompt)
            while (token != null) {
                if (token.isNotEmpty()) {
                    if (!hasVisibleOutput) {
                        pendingOutput.append(token)
                    } else {
                        emit(token)
                    }
                    if (!hasVisibleOutput && pendingOutput.any { !it.isWhitespace() }) {
                        hasVisibleOutput = true
                        emit(pendingOutput.toString())
                        pendingOutput.clear()
                    }
                }
                if (!currentCoroutineContext().isActive) break
                token = withContext(Dispatchers.IO) {
                    LlamaCppJniBridge.generateStreamingTokenNative(nativeContextPtr, null)
                }
            }

            if (hasVisibleOutput) {
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
    }

    companion object {
        private const val TAG = "LlamaCpp"
        private const val MAX_CONTEXT_TOKENS = 2048
    }
}

package com.localai.data.network

import android.os.Build
import android.util.Log

object LlamaCppJniBridge {

    private const val TAG = "JniBridge"
    internal var nativeLoaded = false
        private set
    internal var lastLoadError: String? = null
        private set

    init {
        // Load in dependency order: ggml-base → ggml-cpu → ggml → llama
        var allLoaded = true
        lastLoadError = null
        for (lib in listOf("ggml-base", "ggml-cpu", "ggml", "llama")) {
            try {
                System.loadLibrary(lib)
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "Failed to load $lib", e)
                lastLoadError = "loadLibrary($lib) failed on ABIs ${Build.SUPPORTED_ABIS.joinToString()}: ${e.message}"
                allLoaded = false
                break
            }
        }
        nativeLoaded = allLoaded
    }

    external fun loadModelNative(modelPath: String): Long

    external fun freeModelNative(contextPtr: Long)

    /**
     * Reset the native context: clears the KV cache and resets all internal state.
     * MUST be called before starting a new generation to prevent context overflow.
     */
    external fun resetContextNative(contextPtr: Long)

    /**
     * Returns the configured context window size (n_ctx) for the loaded model.
     */
    external fun getContextSizeNative(contextPtr: Long): Int

    /**
     * Returns the actual token count for the given text using the model's tokenizer.
     * Much more accurate than character-based estimates.
     * Returns -1 on error.
     */
    external fun tokenizeCountNative(contextPtr: Long, text: String): Int

    external fun generateStreamingTokenNative(
        contextPtr: Long,
        prompt: String?
    ): String?
}

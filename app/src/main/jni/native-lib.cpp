#include <jni.h>
#include "llama.h"
#include <algorithm>
#include <cstring>
#include <new>
#include <string>
#include <thread>
#include <vector>

struct NativeContext {
    llama_model      *model      = nullptr;
    llama_context    *ctx        = nullptr;
    const llama_vocab *vocab     = nullptr;
    llama_sampler    *smpl       = nullptr;
    bool              generating = false;
    int               n_past     = 0;
    int               step       = 0;
    int               n_ctx      = 0;   // context window size for overflow checks

    // Cached special token IDs for fast stop detection
    std::vector<llama_token> stop_token_ids;
};

// Known stop sequences to detect at the native level.
// These are checked by text representation in case the model's vocabulary
// uses different token IDs.
static const char * const STOP_SEQUENCES[] = {
    "<|im_end|>",
    "<|eot_id|>",
    "<|end_of_text|>",
    "<|endoftext|>",
    "</s>",
    "<end_of_turn>",
    "<|end|>",
    "<|END_OF_TURN_TOKEN|>",
    nullptr // sentinel
};

static void cache_stop_tokens(NativeContext *nc) {
    if (!nc || !nc->vocab) return;

    nc->stop_token_ids.clear();

    // Always include the standard EOS token
    llama_token eos = llama_vocab_eos(nc->vocab);
    if (eos >= 0) {
        nc->stop_token_ids.push_back(eos);
    }

    // Try to tokenize each known stop sequence and find its single-token ID.
    // Many models encode special tokens as a single token.
    for (int i = 0; STOP_SEQUENCES[i] != nullptr; i++) {
        const char *seq = STOP_SEQUENCES[i];
        int seq_len = (int) std::strlen(seq);

        // Try to tokenize with special token parsing enabled
        int n = llama_tokenize(nc->vocab, seq, seq_len, nullptr, 0, false, true);
        if (n == 1) {
            // It's a single token - cache its ID
            llama_token tok;
            llama_tokenize(nc->vocab, seq, seq_len, &tok, 1, false, true);
            // Avoid duplicates
            bool found = false;
            for (auto &existing : nc->stop_token_ids) {
                if (existing == tok) { found = true; break; }
            }
            if (!found) {
                nc->stop_token_ids.push_back(tok);
            }
        }
    }
}

static bool is_stop_token(const NativeContext *nc, llama_token token) {
    for (auto &stop : nc->stop_token_ids) {
        if (token == stop) return true;
    }
    return false;
}

static llama_sampler * make_default_sampler() {
    auto sparams = llama_sampler_chain_default_params();
    llama_sampler * s = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(s, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(s, llama_sampler_init_top_p(0.9f, 1));
    llama_sampler_chain_add(s, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(s, llama_sampler_init_dist(0));
    return s;
}

// ---------------------------------------------------------------
// loadModelNative
// ---------------------------------------------------------------

extern "C" JNIEXPORT jlong JNICALL
Java_com_localai_data_network_LlamaCppJniBridge_loadModelNative(
    JNIEnv *env, jobject /* thiz */, jstring model_path)
{
    const char *path = env->GetStringUTFChars(model_path, nullptr);
    if (!path) return 0;

    llama_model_params m_params = llama_model_default_params();
    llama_model *model = llama_model_load_from_file(path, m_params);
    env->ReleaseStringUTFChars(model_path, path);
    if (!model) return 0;

    const int ctx_size = 2048;

    llama_context_params c_params = llama_context_default_params();
    c_params.n_ctx = ctx_size;
    const unsigned int hw_threads = std::thread::hardware_concurrency();
    c_params.n_threads = (int32_t) std::clamp(hw_threads > 0 ? hw_threads - 1 : 2u, 1u, 6u);
    c_params.n_batch = 512;  // Larger batch for faster prompt processing

    llama_context *ctx = llama_init_from_model(model, c_params);
    if (!ctx) {
        llama_model_free(model);
        return 0;
    }

    NativeContext *nc = new (std::nothrow) NativeContext{model, ctx};
    if (!nc) {
        llama_free(ctx);
        llama_model_free(model);
        return 0;
    }
    nc->vocab = llama_model_get_vocab(model);
    nc->smpl  = make_default_sampler();
    nc->n_ctx = ctx_size;

    // Pre-cache stop token IDs for fast detection during generation
    cache_stop_tokens(nc);

    return reinterpret_cast<jlong>(nc);
}

// ---------------------------------------------------------------
// freeModelNative
// ---------------------------------------------------------------

extern "C" JNIEXPORT void JNICALL
Java_com_localai_data_network_LlamaCppJniBridge_freeModelNative(
    JNIEnv * /* env */, jobject /* thiz */, jlong context_ptr)
{
    NativeContext *nc = reinterpret_cast<NativeContext *>(context_ptr);
    if (!nc) return;

    if (nc->smpl)  llama_sampler_free(nc->smpl);
    if (nc->ctx)   llama_free(nc->ctx);
    if (nc->model) llama_model_free(nc->model);
    delete nc;
}

// ---------------------------------------------------------------
// resetContextNative
//
//   Clears the KV cache and resets internal state.
//   MUST be called before starting a new conversation turn to
//   prevent KV cache overflow/corruption.
// ---------------------------------------------------------------

extern "C" JNIEXPORT void JNICALL
Java_com_localai_data_network_LlamaCppJniBridge_resetContextNative(
    JNIEnv * /* env */, jobject /* thiz */, jlong context_ptr)
{
    NativeContext *nc = reinterpret_cast<NativeContext *>(context_ptr);
    if (!nc || !nc->ctx) return;

    llama_kv_cache_clear(nc->ctx);
    nc->n_past     = 0;
    nc->step       = 0;
    nc->generating = false;

    // Reset the sampler state as well
    if (nc->smpl) {
        llama_sampler_reset(nc->smpl);
    }
}

// ---------------------------------------------------------------
// getContextSizeNative
//
//   Returns the configured context window size (n_ctx).
// ---------------------------------------------------------------

extern "C" JNIEXPORT jint JNICALL
Java_com_localai_data_network_LlamaCppJniBridge_getContextSizeNative(
    JNIEnv * /* env */, jobject /* thiz */, jlong context_ptr)
{
    NativeContext *nc = reinterpret_cast<NativeContext *>(context_ptr);
    if (!nc) return 0;
    return (jint) nc->n_ctx;
}

// ---------------------------------------------------------------
// tokenizeCountNative
//
//   Returns the actual number of tokens for a given text using
//   the model's real tokenizer. This is much more accurate than
//   character-based estimates.
// ---------------------------------------------------------------

extern "C" JNIEXPORT jint JNICALL
Java_com_localai_data_network_LlamaCppJniBridge_tokenizeCountNative(
    JNIEnv *env, jobject /* thiz */, jlong context_ptr, jstring text)
{
    NativeContext *nc = reinterpret_cast<NativeContext *>(context_ptr);
    if (!nc || !nc->vocab) return -1;

    const char *text_str = env->GetStringUTFChars(text, nullptr);
    if (!text_str) return -1;

    int n_tokens = llama_tokenize(
        nc->vocab, text_str, std::strlen(text_str),
        nullptr, 0, true, true);

    env->ReleaseStringUTFChars(text, text_str);
    return (jint)(n_tokens > 0 ? n_tokens : 0);
}

// ---------------------------------------------------------------
// generateStreamingTokenNative
//
//   prompt != null  -> start new generation, return first token
//   prompt == null  -> continue generation, return next token
//   returns null on EOS / stop token / error / context overflow
//
//   IMPORTANT: Call resetContextNative() before passing a new prompt
//   to ensure the KV cache is clean. If not called, this function
//   will clear it automatically when a prompt is provided.
//
//   Special tokens (thinking markers, role markers, etc.) are passed
//   through as text so the Kotlin layer can properly handle them
//   (e.g., showing thinking indicators). Only true stop/end tokens
//   cause generation to terminate.
// ---------------------------------------------------------------

extern "C" JNIEXPORT jstring JNICALL
Java_com_localai_data_network_LlamaCppJniBridge_generateStreamingTokenNative(
    JNIEnv *env, jobject /* thiz */, jlong context_ptr, jstring prompt)
{
    NativeContext *nc = reinterpret_cast<NativeContext *>(context_ptr);
    if (!nc || !nc->ctx) return nullptr;

    // ---------- new generation ----------------------------------------------
    if (prompt != nullptr) {
        // CRITICAL: Clear KV cache before starting new generation.
        // Without this, stale KV entries from prior generations corrupt
        // the attention computation and eventually cause a crash when
        // n_past exceeds n_ctx.
        llama_kv_cache_clear(nc->ctx);
        nc->generating = false;
        nc->n_past     = 0;
        nc->step       = 0;

        // Reset sampler state for fresh generation
        if (nc->smpl) {
            llama_sampler_reset(nc->smpl);
        }

        const char *prompt_str = env->GetStringUTFChars(prompt, nullptr);
        if (!prompt_str) return nullptr;

        int n_tokens = llama_tokenize(
            nc->vocab, prompt_str, std::strlen(prompt_str),
            nullptr, 0, true, true);
        if (n_tokens <= 0) {
            env->ReleaseStringUTFChars(prompt, prompt_str);
            return nullptr;
        }

        // Safety check: ensure prompt fits within context window
        // Leave room for at least some generation tokens
        const int max_prompt_tokens = nc->n_ctx - 64;  // Reserve 64 for generation
        if (n_tokens > max_prompt_tokens) {
            // Truncate from the beginning (keep the most recent context)
            // We'll re-tokenize with a truncated string
            // For safety, just cap at max and proceed
            n_tokens = max_prompt_tokens;
        }

        std::vector<llama_token> tokens(static_cast<size_t>(n_tokens));
        int actual = llama_tokenize(
            nc->vocab, prompt_str, std::strlen(prompt_str),
            tokens.data(), tokens.size(), true, true);

        env->ReleaseStringUTFChars(prompt, prompt_str);

        if (actual <= 0) return nullptr;

        // If tokenization gave more tokens than our buffer (shouldn't happen
        // but be safe), cap it
        if (actual > (int) tokens.size()) {
            actual = (int) tokens.size();
        }

        // Process prompt in batches to avoid exceeding n_batch
        const int n_batch = 512;
        for (int i = 0; i < actual; i += n_batch) {
            int batch_size = std::min(n_batch, actual - i);

            llama_batch batch = llama_batch_init(batch_size, 0, 1);
            for (int j = 0; j < batch_size; j++) {
                batch.token[j]     = tokens[i + j];
                batch.pos[j]       = nc->n_past++;
                batch.n_seq_id[j]  = 1;
                batch.seq_id[j][0] = 0;
                // Only compute logits for the very last token of the entire prompt
                batch.logits[j]    = (i + j == actual - 1);
            }
            batch.n_tokens = batch_size;

            int ret = llama_decode(nc->ctx, batch);
            llama_batch_free(batch);
            if (ret != 0) {
                // Decode failed - context might be corrupted, reset everything
                llama_kv_cache_clear(nc->ctx);
                nc->n_past = 0;
                nc->generating = false;
                return nullptr;
            }
        }

        nc->generating = true;
        // fall through to produce the first output token
    }

    // ---------- continue generation -----------------------------------------
    if (!nc->generating) return nullptr;

    // Check if we've exceeded the context window
    // Leave a small margin to be safe
    if (nc->n_past >= nc->n_ctx - 1) {
        nc->generating = false;
        return nullptr;
    }

    // Also cap total generation steps to prevent runaway generation
    if (nc->step >= (nc->n_ctx - 64)) {
        nc->generating = false;
        return nullptr;
    }

    // sample next token
    llama_token new_token_id = llama_sampler_sample(nc->smpl, nc->ctx, -1);

    // Check against all cached stop tokens (EOS + model-specific stops)
    if (new_token_id < 0 || is_stop_token(nc, new_token_id)) {
        nc->generating = false;
        return nullptr;
    }

    // Detokenize with a growable buffer.
    // We pass special=true so special tokens are rendered as their text
    // representation (e.g., "<think>") rather than being silently dropped.
    // The Kotlin SpecialTokenFilter handles the semantic processing.
    std::vector<char> piece_buf(64);
    int len = llama_token_to_piece(nc->vocab, new_token_id, piece_buf.data(), (int32_t) piece_buf.size(), 0, true);
    if (len < 0) {
        piece_buf.resize((size_t) -len);
        len = llama_token_to_piece(nc->vocab, new_token_id, piece_buf.data(), (int32_t) piece_buf.size(), 0, true);
    }
    std::string piece;
    if (len > 0) {
        piece.assign(piece_buf.data(), (size_t) len);
    }

    // Check if the detokenized text itself is a known stop sequence.
    // This handles cases where the model's tokenizer splits stop sequences
    // differently than expected.
    if (len > 0) {
        for (int i = 0; STOP_SEQUENCES[i] != nullptr; i++) {
            if (piece == STOP_SEQUENCES[i]) {
                nc->generating = false;
                return nullptr;
            }
        }
    }

    // feed token back for next iteration
    {
        llama_batch batch = llama_batch_init(1, 0, 1);
        batch.token[0]     = new_token_id;
        batch.pos[0]       = nc->n_past++;
        batch.n_seq_id[0]  = 1;
        batch.seq_id[0][0] = 0;
        batch.logits[0]    = true;
        batch.n_tokens     = 1;

        int ret = llama_decode(nc->ctx, batch);
        llama_batch_free(batch);
        if (ret != 0) {
            // If decode fails during generation, stop gracefully
            nc->generating = false;
            return nullptr;
        }
    }
    nc->step++;

    return env->NewStringUTF(piece.c_str());
}

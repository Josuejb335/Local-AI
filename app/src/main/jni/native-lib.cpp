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

    llama_context_params c_params = llama_context_default_params();
    c_params.n_ctx = 2048;
    const unsigned int hw_threads = std::thread::hardware_concurrency();
    c_params.n_threads = (int32_t) std::clamp(hw_threads > 0 ? hw_threads - 1 : 2u, 1u, 6u);
    c_params.n_batch = 128;

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
// generateStreamingTokenNative
//
//   prompt != null  -> start new generation, return first token
//   prompt == null  -> continue generation, return next token
//   returns null on EOS / stop token / error
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
        nc->generating = false;
        nc->n_past     = 0;
        nc->step       = 0;

        const char *prompt_str = env->GetStringUTFChars(prompt, nullptr);
        if (!prompt_str) return nullptr;

        int n_tokens = llama_tokenize(
            nc->vocab, prompt_str, std::strlen(prompt_str),
            nullptr, 0, true, true);
        if (n_tokens <= 0) {
            env->ReleaseStringUTFChars(prompt, prompt_str);
            return nullptr;
        }

        std::vector<llama_token> tokens(static_cast<size_t>(n_tokens));
        llama_tokenize(
            nc->vocab, prompt_str, std::strlen(prompt_str),
            tokens.data(), tokens.size(), true, true);

        env->ReleaseStringUTFChars(prompt, prompt_str);

        // eval the full prompt - explicit positions, seq 0
        {
            llama_batch batch = llama_batch_init(n_tokens, 0, 1);
            for (int i = 0; i < n_tokens; i++) {
                batch.token[i]     = tokens[i];
                batch.pos[i]       = nc->n_past++;
                batch.n_seq_id[i]  = 1;
                batch.seq_id[i][0] = 0;
                batch.logits[i]    = (i == n_tokens - 1);
            }
            batch.n_tokens = n_tokens;

            int ret = llama_decode(nc->ctx, batch);
            llama_batch_free(batch);
            if (ret != 0) return nullptr;
        }

        nc->generating = true;
        // fall through to produce the first output token
    }

    // ---------- continue generation -----------------------------------------
    if (!nc->generating) return nullptr;

    if (nc->step >= 2048) {
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
            nc->generating = false;
            return nullptr;
        }
    }
    nc->step++;

    return env->NewStringUTF(piece.c_str());
}

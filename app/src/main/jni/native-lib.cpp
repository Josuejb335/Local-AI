#include <jni.h>
#include "llama.h"
#include <cstring>
#include <new>
#include <vector>

struct NativeContext {
    llama_model      *model      = nullptr;
    llama_context    *ctx        = nullptr;
    const llama_vocab *vocab     = nullptr;
    llama_sampler    *smpl       = nullptr;
    bool              generating = false;
    int               n_past     = 0;
    int               step       = 0;
};

static llama_sampler * make_greedy_sampler() {
    auto sparams = llama_sampler_chain_default_params();
    llama_sampler * s = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(s, llama_sampler_init_greedy());
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
    c_params.n_ctx     = 2048;
    c_params.n_threads = 4;
    c_params.n_batch   = 512;

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
    nc->smpl  = make_greedy_sampler();

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
//   prompt != null  → start new generation, return first token
//   prompt == null  → continue generation, return next token
//   returns null on EOS / error
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
            nullptr, 0, true, false);
        if (n_tokens <= 0) {
            env->ReleaseStringUTFChars(prompt, prompt_str);
            return nullptr;
        }

        std::vector<llama_token> tokens(static_cast<size_t>(n_tokens));
        llama_tokenize(
            nc->vocab, prompt_str, std::strlen(prompt_str),
            tokens.data(), tokens.size(), true, false);

        env->ReleaseStringUTFChars(prompt, prompt_str);

        // eval the full prompt — explicit positions, seq 0
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

    if (new_token_id < 0 || new_token_id == llama_vocab_eos(nc->vocab)) {
        nc->generating = false;
        return nullptr;
    }

    // detokenize
    char piece[16];
    int len = llama_token_to_piece(nc->vocab, new_token_id, piece, (int)sizeof(piece), 0, true);
    if (len < 0) len = 0;
    piece[len] = '\0';

    // feed token back
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

    return env->NewStringUTF(piece);
}

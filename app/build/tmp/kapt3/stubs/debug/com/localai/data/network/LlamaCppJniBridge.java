package com.localai.data.network;

import android.util.Log;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0005\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0011\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\rH\u0086 J\u001d\u0010\u000e\u001a\u0004\u0018\u00010\u00042\u0006\u0010\f\u001a\u00020\r2\b\u0010\u000f\u001a\u0004\u0018\u00010\u0004H\u0086 J\u0011\u0010\u0010\u001a\u00020\r2\u0006\u0010\u0011\u001a\u00020\u0004H\u0086 R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u001e\u0010\u0007\u001a\u00020\u00062\u0006\u0010\u0005\u001a\u00020\u0006@BX\u0080\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\t\u00a8\u0006\u0012"}, d2 = {"Lcom/localai/data/network/LlamaCppJniBridge;", "", "()V", "TAG", "", "<set-?>", "", "nativeLoaded", "getNativeLoaded$app_debug", "()Z", "freeModelNative", "", "contextPtr", "", "generateStreamingTokenNative", "prompt", "loadModelNative", "modelPath", "app_debug"})
public final class LlamaCppJniBridge {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "JniBridge";
    private static boolean nativeLoaded = false;
    @org.jetbrains.annotations.NotNull()
    public static final com.localai.data.network.LlamaCppJniBridge INSTANCE = null;
    
    private LlamaCppJniBridge() {
        super();
    }
    
    public final boolean getNativeLoaded$app_debug() {
        return false;
    }
    
    public final native long loadModelNative(@org.jetbrains.annotations.NotNull()
    java.lang.String modelPath) {
        return 0L;
    }
    
    public final native void freeModelNative(long contextPtr) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final native java.lang.String generateStreamingTokenNative(long contextPtr, @org.jetbrains.annotations.Nullable()
    java.lang.String prompt) {
        return null;
    }
}
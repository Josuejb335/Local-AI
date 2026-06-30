#!/usr/bin/env bash
# =============================================================================
# Build libllama.so with JNI wrapper for Android
#
# Prerequisites:
#   - Android NDK (set ANDROID_NDK_HOME or edit NDK_PATH below)
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JNI_DIR="$SCRIPT_DIR/app/src/main/jni"
LLAMA_CPP_DIR="${LLAMA_CPP_DIR:-$SCRIPT_DIR/../llama.cpp}"
OUTPUT_DIR="$SCRIPT_DIR/app/src/main/jniLibs"
NDK_PATH="${ANDROID_NDK_HOME:-$HOME/Android/Sdk/ndk/25.1.8937393}"
API_LEVEL=26
CMAKE_TOOLCHAIN="$NDK_PATH/build/cmake/android.toolchain.cmake"

ABIS=("x86_64" "arm64-v8a")

# ---------------------------------------------------------------------------
# checks
# ---------------------------------------------------------------------------
if [ ! -d "$LLAMA_CPP_DIR" ]; then
    echo "Cloning llama.cpp into $LLAMA_CPP_DIR ..."
    git clone --depth 1 https://github.com/ggerganov/llama.cpp "$LLAMA_CPP_DIR"
fi

if [ ! -f "$CMAKE_TOOLCHAIN" ]; then
    echo "ERROR: NDK toolchain not found at $CMAKE_TOOLCHAIN"
    echo "Set ANDROID_NDK_HOME to your NDK path or install via SDK Manager."
    exit 1
fi

# ---------------------------------------------------------------------------
# build for each target ABI
# ---------------------------------------------------------------------------
for ABI in "${ABIS[@]}"; do
    echo "━━━ Building libllama.so for $ABI ━━━"

    BUILD_DIR="$SCRIPT_DIR/build_llama_jni/$ABI"
    mkdir -p "$BUILD_DIR"

    cmake -S "$JNI_DIR" \
          -B "$BUILD_DIR" \
          -DCMAKE_TOOLCHAIN_FILE="$CMAKE_TOOLCHAIN" \
          -DANDROID_ABI="$ABI" \
          -DANDROID_PLATFORM="$API_LEVEL" \
          -DCMAKE_C_FLAGS="-O2 -DNDEBUG" \
          -DCMAKE_CXX_FLAGS="-O2 -DNDEBUG" \
          -DBUILD_SHARED_LIBS=ON \
          -DLLAMA_STATIC=OFF \
          -DLLAMA_CUDA=OFF \
          -DLLAMA_METAL=OFF \
          -DLLAMA_VULKAN=OFF \
          -DLLAMA_CPP_DIR="$LLAMA_CPP_DIR" \
          -G "Unix Makefiles"

    cmake --build "$BUILD_DIR" --target llama -j "$(nproc)"

    OUT_ABI="$OUTPUT_DIR/$ABI"
    mkdir -p "$OUT_ABI"
    cp "$BUILD_DIR/bin/libllama.so" "$OUT_ABI/libllama.so"
    for dep in libggml-base.so libggml-cpu.so libggml.so; do
        if [ -f "$BUILD_DIR/bin/$dep" ]; then
            cp "$BUILD_DIR/bin/$dep" "$OUT_ABI/$dep"
        fi
    done

    # Copy libomp.so from the NDK (required by libggml-base.so and libggml-cpu.so)
    # The NDK ships OpenMP at: toolchains/llvm/prebuilt/<host>/lib/clang/<ver>/lib/linux/<arch>/libomp.so
    case "$ABI" in
        arm64-v8a) OMP_ARCH="aarch64" ;;
        armeabi-v7a) OMP_ARCH="arm" ;;
        x86_64) OMP_ARCH="x86_64" ;;
        x86) OMP_ARCH="i386" ;;
        *) OMP_ARCH="" ;;
    esac
    if [ -n "$OMP_ARCH" ]; then
        OMP_SO=$(find "$NDK_PATH/toolchains/llvm/prebuilt" -path "*/lib/linux/$OMP_ARCH/libomp.so" 2>/dev/null | head -1)
        if [ -n "$OMP_SO" ] && [ -f "$OMP_SO" ]; then
            cp "$OMP_SO" "$OUT_ABI/libomp.so"
            echo "  ✓ Copied libomp.so from NDK for $ABI"
        else
            echo "  ⚠ WARNING: libomp.so not found in NDK for $OMP_ARCH."
            echo "    libggml-base.so and libggml-cpu.so depend on OpenMP."
            echo "    Searched: $NDK_PATH/toolchains/llvm/prebuilt/*/lib/clang/*/lib/linux/$OMP_ARCH/libomp.so"
        fi
    fi

    echo "✓ $OUT_ABI/libllama.so"
done

rm -rf "$SCRIPT_DIR/build_llama_jni"

echo ""
echo "═══════════════════════════════════════════════════════════════════"
echo "  Done. Run ./gradlew installDebug to install on emulator."
echo "═══════════════════════════════════════════════════════════════════"

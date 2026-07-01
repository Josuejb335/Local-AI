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

expected_machine() {
    case "$1" in
        arm64-v8a) echo "ARM aarch64" ;;
        armeabi-v7a) echo "ARM" ;;
        x86_64) echo "x86-64" ;;
        x86) echo "Intel 80386" ;;
        *) echo "" ;;
    esac
}

verify_android_abi_binary() {
    local so_path="$1"
    local abi="$2"
    local expected
    expected="$(expected_machine "$abi")"
    if [ ! -f "$so_path" ]; then
        echo "ERROR: Missing expected binary: $so_path"
        exit 1
    fi
    local info
    info="$(file "$so_path")"
    if ! echo "$info" | grep -q "for Android"; then
        echo "ERROR: $so_path is not an Android binary."
        echo "  file output: $info"
        exit 1
    fi
    if [ -n "$expected" ] && ! echo "$info" | grep -q "$expected"; then
        echo "ERROR: $so_path does not match ABI $abi."
        echo "  file output: $info"
        exit 1
    fi
}

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
          -DGGML_OPENMP=OFF \
          -DLLAMA_CPP_DIR="$LLAMA_CPP_DIR" \
          -G "Unix Makefiles"

    cmake --build "$BUILD_DIR" --target llama -j "$(nproc)"

    OUT_ABI="$OUTPUT_DIR/$ABI"
    rm -rf "$OUT_ABI"
    mkdir -p "$OUT_ABI"
    verify_android_abi_binary "$BUILD_DIR/bin/libllama.so" "$ABI"
    verify_android_abi_binary "$BUILD_DIR/bin/libggml-base.so" "$ABI"
    verify_android_abi_binary "$BUILD_DIR/bin/libggml-cpu.so" "$ABI"
    verify_android_abi_binary "$BUILD_DIR/bin/libggml.so" "$ABI"
    cp "$BUILD_DIR/bin/libllama.so" "$OUT_ABI/libllama.so"
    for dep in libggml-base.so libggml-cpu.so libggml.so; do
        if [ -f "$BUILD_DIR/bin/$dep" ]; then
            cp "$BUILD_DIR/bin/$dep" "$OUT_ABI/$dep"
        fi
    done

    if readelf -d "$OUT_ABI/libggml-base.so" | grep -q "Shared library: \\[libomp.so\\]"; then
        echo "ERROR: libggml-base.so still links to libomp.so for $ABI (GGML_OPENMP should be OFF)."
        exit 1
    fi

    echo "✓ $OUT_ABI/libllama.so"
done

rm -rf "$SCRIPT_DIR/build_llama_jni"

echo ""
echo "═══════════════════════════════════════════════════════════════════"
echo "  Done. Run ./gradlew installDebug to install on emulator."
echo "═══════════════════════════════════════════════════════════════════"

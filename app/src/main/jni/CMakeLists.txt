cmake_minimum_required(VERSION 3.22)
project(llama_jni C CXX)

# llama.cpp directory — override with -DLLAMA_CPP_DIR=/path on command line
if(NOT DEFINED LLAMA_CPP_DIR)
    set(LLAMA_CPP_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../../llama.cpp")
endif()

# Build llama.cpp as a library target
add_subdirectory("${LLAMA_CPP_DIR}" build_llama)

# Add our JNI wrapper source to the existing llama target
target_sources(llama PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}/native-lib.cpp")

# JNI headers are provided by the NDK sysroot automatically.
# Link required Android libraries.
target_link_libraries(llama PRIVATE log android)

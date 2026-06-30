package com.localai.data.repository

import android.content.Context
import com.localai.domain.model.DownloadState
import com.localai.domain.model.ModelInfo
import com.localai.domain.repository.AIModelRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) : AIModelRepository {

    private val modelsDir: File
        get() = File(context.filesDir, "models").also { it.mkdirs() }

    override fun fetchRemoteModels(): Flow<List<ModelInfo>> = flow {
        // TODO: Replace with real Hugging Face API call:
        // val response = hfApiClient.searchGgufModels(query = "GGUF", limit = 20)
        emit(
            listOf(
                ModelInfo(
                    id = "Qwen/Qwen2.5-1.5B-Instruct-GGUF",
                    author = "Qwen",
                    name = "Qwen2.5-1.5B-Instruct",
                    description = "Lightweight instruction-tuned model, ~900 MB Q4_K_M",
                    downloads = 185_000,
                    fileSizeBytes = 987_000_000,
                    ggufFileName = "qwen2.5-1.5b-instruct-q4_k_m.gguf"
                ),
                ModelInfo(
                    id = "google/gemma-2-2b-it-GGUF",
                    author = "Google",
                    name = "Gemma 2 2B IT",
                    description = "Google's Gemma 2 instruction-tuned, ~1.3 GB Q4_K_M",
                    downloads = 320_000,
                    fileSizeBytes = 1_350_000_000,
                    ggufFileName = "gemma-2-2b-it-q4_k_m.gguf"
                ),
                ModelInfo(
                    id = "QuantFactory/Meta-Llama-3.2-1B-Instruct-GGUF",
                    author = "Meta",
                    name = "Llama 3.2 1B Instruct",
                    description = "Meta's latest small instruct model, ~700 MB Q4_K_M",
                    downloads = 590_000,
                    fileSizeBytes = 711_000_000,
                    ggufFileName = "Meta-Llama-3.2-1B-Instruct.Q4_K_M.gguf"
                )
            )
        )
    }

    override fun downloadModel(modelInfo: ModelInfo): Flow<DownloadState> = flow {
        val targetFile = File(modelsDir, modelInfo.ggufFileName)

        // If a real GGUF file already exists (e.g. adb-pushed), skip download
        if (targetFile.exists() && targetFile.length() > 4096) {
            emit(DownloadState.Completed(targetFile.absolutePath))
            return@flow
        }

        emit(DownloadState.InProgress(0f))

        val tmpFile = File(modelsDir, "${modelInfo.ggufFileName}.tmp")

        try {
            val url = "https://huggingface.co/${modelInfo.id}/resolve/main/${modelInfo.ggufFileName}"

            val request = Request.Builder()
                .url(url)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                tmpFile.delete()
                emit(DownloadState.Failed("Download failed: HTTP ${response.code}"))
                return@flow
            }

            val body = response.body ?: run {
                tmpFile.delete()
                emit(DownloadState.Failed("Download failed: empty response body"))
                return@flow
            }

            val contentLength = body.contentLength()
            var bytesRead: Long = 0

            body.byteStream().use { inputStream ->
                tmpFile.outputStream().use { outputStream ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        bytesRead += read
                        if (contentLength > 0) {
                            val progress = bytesRead.toFloat() / contentLength.toFloat()
                            emit(DownloadState.InProgress(progress.coerceIn(0f, 1f)))
                        }
                    }
                }
            }

            // Rename tmp file to final file on success
            if (tmpFile.renameTo(targetFile)) {
                emit(DownloadState.Completed(targetFile.absolutePath))
            } else {
                tmpFile.delete()
                emit(DownloadState.Failed("Failed to rename downloaded file"))
            }
        } catch (e: IOException) {
            tmpFile.delete()
            emit(DownloadState.Failed("Download failed: ${e.message ?: "Unknown IO error"}"))
        } catch (e: Exception) {
            tmpFile.delete()
            emit(DownloadState.Failed("Download failed: ${e.message ?: "Unknown error"}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getLocalModels(): List<ModelInfo> {
        val files = modelsDir.listFiles() ?: return emptyList()
        return files.filter { it.extension == "gguf" }.map { file ->
            ModelInfo(
                id = file.nameWithoutExtension,
                author = "Local",
                name = file.nameWithoutExtension,
                description = "Downloaded model",
                downloads = 0,
                fileSizeBytes = file.length(),
                ggufFileName = file.name,
                localPath = file.absolutePath
            )
        }
    }

    override suspend fun getLocalPath(modelId: String): String? {
        val fileName = modelId.substringAfterLast("/")
        val file = File(modelsDir, fileName)
        return if (file.exists()) file.absolutePath else null
    }

    override suspend fun deleteModel(modelId: String) {
        val fileName = modelId.substringAfterLast("/")
        File(modelsDir, fileName).delete()
    }
}

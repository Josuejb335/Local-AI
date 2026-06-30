package com.localai.data.repository

import android.content.Context
import android.util.Log
import com.localai.domain.model.DownloadState
import com.localai.domain.model.ModelInfo
import com.localai.domain.repository.AIModelRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.job
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) : AIModelRepository {

    companion object {
        private const val TAG = "AIModelRepository"
        // Use expand[]=siblings to inline file metadata in search results (avoids N+1 API calls)
        private const val HF_API_URL =
            "https://huggingface.co/api/models?filter=gguf&sort=downloads&direction=-1&limit=50&expand[]=siblings"
        private const val MAX_FILE_SIZE_BYTES = 2_000_000_000L // 2GB cap for < 3B models
        private val PREFERRED_QUANT_PATTERNS = listOf(
            "q4_k_m", "q4_k_s", "q4_0", "q4_1", "q5_k_m", "q5_k_s"
        )
    }

    private val modelsDir: File
        get() = File(context.filesDir, "models").also { it.mkdirs() }

    override fun fetchRemoteModels(): Flow<List<ModelInfo>> = flow {
        val models = try {
            fetchModelsFromHuggingFace()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch models from Hugging Face API, using fallback list", e)
            getFallbackModels()
        }
        emit(models)
    }.flowOn(Dispatchers.IO)

    private fun fetchModelsFromHuggingFace(): List<ModelInfo> {
        val request = Request.Builder()
            .url(HF_API_URL)
            .header("Accept", "application/json")
            .build()

        val response = okHttpClient.newCall(request).execute()

        response.use { resp ->
            if (!resp.isSuccessful) {
                throw IOException("HF API returned HTTP ${resp.code}")
            }

            val body = resp.body?.string()
                ?: throw IOException("HF API returned empty response")

            val modelsArray = JSONArray(body)
            val result = mutableListOf<ModelInfo>()

            for (i in 0 until modelsArray.length()) {
                // Wrap per-model processing in try/catch so one bad model
                // does not discard already-parsed partial results
                try {
                    val modelObj = modelsArray.getJSONObject(i)
                    val modelId = modelObj.optString("id", "")
                    if (modelId.isEmpty()) continue
                    val author = modelObj.optString("author", modelId.substringBefore("/"))
                    val downloads = modelObj.optLong("downloads", 0)

                    // The search response includes siblings via expand[]=siblings.
                    // Use it directly; fall back to detail endpoint only if missing.
                    val modelData = if (modelObj.has("siblings")) {
                        modelObj
                    } else {
                        fetchModelDetail(modelId) ?: continue
                    }

                    val ggufFile = findBestGgufFile(modelData) ?: continue

                    val fileName = ggufFile.first
                    val fileSize = ggufFile.second

                    // Skip if file has a known size that exceeds 2GB (likely > 3B params)
                    if (fileSize > MAX_FILE_SIZE_BYTES) continue

                    val modelName = modelId.substringAfter("/")
                        .replace("-GGUF", "")
                        .replace("-gguf", "")

                    result.add(
                        ModelInfo(
                            id = modelId,
                            author = author,
                            name = modelName,
                            description = formatDescription(fileSize, downloads),
                            downloads = downloads,
                            fileSizeBytes = fileSize,
                            ggufFileName = fileName
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse model at index $i, skipping", e)
                    continue
                }
            }

            // Sort by downloads descending and return
            return result.sortedByDescending { it.downloads }
        }
    }

    private fun fetchModelDetail(modelId: String): JSONObject? {
        return try {
            val url = "https://huggingface.co/api/models/$modelId"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                JSONObject(body)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch model detail for $modelId", e)
            null
        }
    }

    private fun findBestGgufFile(modelDetail: JSONObject): Pair<String, Long>? {
        val siblings = modelDetail.optJSONArray("siblings") ?: return null

        // Collect all .gguf files with their sizes
        // Files with known size <= MAX are preferred; files with unknown size (0) are kept
        // as lower-priority candidates
        val ggufFilesWithKnownSize = mutableListOf<Pair<String, Long>>()
        val ggufFilesWithUnknownSize = mutableListOf<Pair<String, Long>>()

        for (i in 0 until siblings.length()) {
            val sibling = siblings.getJSONObject(i)
            val filename = sibling.optString("rfilename", "")
            if (filename.endsWith(".gguf", ignoreCase = true)) {
                val size = sibling.optLong("size", 0)
                when {
                    size > MAX_FILE_SIZE_BYTES -> { /* skip files known to be too large */ }
                    size > 0 -> ggufFilesWithKnownSize.add(filename to size)
                    else -> ggufFilesWithUnknownSize.add(filename to 0L)
                }
            }
        }

        // Prefer files with known size first, then fall back to unknown-size files
        val candidates = if (ggufFilesWithKnownSize.isNotEmpty()) {
            ggufFilesWithKnownSize
        } else if (ggufFilesWithUnknownSize.isNotEmpty()) {
            ggufFilesWithUnknownSize
        } else {
            return null
        }

        // Prefer Q4_K_M quantization, then Q4_K_S, then Q4_0, etc.
        for (quantPattern in PREFERRED_QUANT_PATTERNS) {
            val match = candidates.find { (name, _) ->
                name.lowercase().contains(quantPattern)
            }
            if (match != null) return match
        }

        // If no preferred quantization found, return the smallest known-size file
        // or the first unknown-size file
        return if (ggufFilesWithKnownSize.isNotEmpty()) {
            ggufFilesWithKnownSize.minByOrNull { it.second }
        } else {
            ggufFilesWithUnknownSize.firstOrNull()
        }
    }

    private fun formatDescription(fileSize: Long, downloads: Long): String {
        val sizeStr = when {
            fileSize <= 0 -> "Unknown size"
            fileSize >= 1_000_000_000 -> "%.1f GB".format(fileSize / 1_000_000_000.0)
            fileSize >= 1_000_000 -> "%.0f MB".format(fileSize / 1_000_000.0)
            else -> "%.0f KB".format(fileSize / 1_000.0)
        }
        val dlStr = when {
            downloads >= 1_000_000 -> "%.1fM downloads".format(downloads / 1_000_000.0)
            downloads >= 1_000 -> "%.1fK downloads".format(downloads / 1_000.0)
            else -> "$downloads downloads"
        }
        return "$sizeStr, $dlStr"
    }

    private fun getFallbackModels(): List<ModelInfo> = listOf(
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
            id = "bartowski/Llama-3.2-1B-Instruct-GGUF",
            author = "bartowski",
            name = "Llama-3.2-1B-Instruct",
            description = "Meta Llama 3.2 1B quantized by bartowski, ~700 MB Q4_K_M",
            downloads = 500_000,
            fileSizeBytes = 750_000_000,
            ggufFileName = "Llama-3.2-1B-Instruct-Q4_K_M.gguf"
        ),
        ModelInfo(
            id = "bartowski/SmolLM2-1.7B-Instruct-GGUF",
            author = "bartowski",
            name = "SmolLM2-1.7B-Instruct",
            description = "HuggingFace SmolLM2 1.7B, ~1.0 GB Q4_K_M",
            downloads = 120_000,
            fileSizeBytes = 1_020_000_000,
            ggufFileName = "SmolLM2-1.7B-Instruct-Q4_K_M.gguf"
        ),
        ModelInfo(
            id = "bartowski/gemma-2-2b-it-GGUF",
            author = "bartowski",
            name = "gemma-2-2b-it",
            description = "Google Gemma 2 2B IT quantized by bartowski, ~1.6 GB Q4_K_M",
            downloads = 200_000,
            fileSizeBytes = 1_600_000_000,
            ggufFileName = "gemma-2-2b-it-Q4_K_M.gguf"
        )
    )

    override fun downloadModel(modelInfo: ModelInfo): Flow<DownloadState> = flow {
        val targetFile = File(modelsDir, modelInfo.ggufFileName)

        // Validate existing file against expected size to prevent false completion
        if (targetFile.exists()) {
            val expectedSize = modelInfo.fileSizeBytes
            val actualSize = targetFile.length()

            if (expectedSize > 0) {
                // File must be at least 90% of expected size to be considered complete
                if (actualSize >= expectedSize * 0.9) {
                    emit(DownloadState.Completed(targetFile.absolutePath))
                    return@flow
                } else {
                    // File exists but is too small - likely a failed/partial download
                    Log.w(TAG, "Existing file ${targetFile.name} is ${actualSize} bytes " +
                            "but expected ~${expectedSize} bytes. Deleting and re-downloading.")
                    targetFile.delete()
                }
            } else {
                // No expected size known - use a more conservative minimum (1MB)
                if (actualSize > 1_000_000) {
                    emit(DownloadState.Completed(targetFile.absolutePath))
                    return@flow
                } else {
                    targetFile.delete()
                }
            }
        }

        emit(DownloadState.InProgress(0f))

        val tmpFile = File(modelsDir, "${modelInfo.ggufFileName}.tmp")

        try {
            val url = "https://huggingface.co/${modelInfo.id}/resolve/main/${modelInfo.ggufFileName}"

            val request = Request.Builder()
                .url(url)
                .build()

            val call = okHttpClient.newCall(request)
            val cancelHandle = coroutineContext.job.invokeOnCompletion { call.cancel() }

            try {
                val response = call.execute()

                response.use { resp ->
                    if (!resp.isSuccessful) {
                        tmpFile.delete()
                        val errorMessage = when (resp.code) {
                            401 -> "This model requires authentication. Please use a public model."
                            403 -> "Access to this model is forbidden. It may require accepting terms of use."
                            404 -> "Model file not found at this URL. The file may have been renamed or removed."
                            429 -> "Too many requests. Please try again later."
                            500, 502, 503 -> "Hugging Face server error (${resp.code}). Please try again later."
                            else -> "Download failed: HTTP ${resp.code}"
                        }
                        emit(DownloadState.Failed(errorMessage))
                        return@flow
                    }

                    val body = resp.body ?: run {
                        tmpFile.delete()
                        emit(DownloadState.Failed("Download failed: empty response body"))
                        return@flow
                    }

                    val contentLength = body.contentLength()
                    var bytesRead: Long = 0
                    var lastEmittedPercent = -1

                    body.byteStream().use { inputStream ->
                        tmpFile.outputStream().use { outputStream ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (inputStream.read(buffer).also { read = it } != -1) {
                                coroutineContext.ensureActive()
                                outputStream.write(buffer, 0, read)
                                bytesRead += read
                                if (contentLength > 0) {
                                    val progress = bytesRead.toFloat() / contentLength.toFloat()
                                    val percent = (progress * 100).toInt()
                                    if (percent != lastEmittedPercent) {
                                        lastEmittedPercent = percent
                                        emit(DownloadState.InProgress(progress.coerceIn(0f, 1f)))
                                    }
                                }
                            }
                        }
                    }
                }
            } finally {
                cancelHandle.dispose()
            }

            // Validate downloaded file size before finalizing
            val downloadedSize = tmpFile.length()
            if (modelInfo.fileSizeBytes > 0 && downloadedSize < modelInfo.fileSizeBytes * 0.9) {
                tmpFile.delete()
                emit(DownloadState.Failed(
                    "Download incomplete: got ${downloadedSize / 1_000_000}MB " +
                            "but expected ~${modelInfo.fileSizeBytes / 1_000_000}MB"
                ))
                return@flow
            }

            // Rename tmp file to final file on success
            if (tmpFile.renameTo(targetFile)) {
                emit(DownloadState.Completed(targetFile.absolutePath))
            } else {
                tmpFile.delete()
                emit(DownloadState.Failed("Failed to rename downloaded file"))
            }
        } catch (e: CancellationException) {
            tmpFile.delete()
            throw e
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

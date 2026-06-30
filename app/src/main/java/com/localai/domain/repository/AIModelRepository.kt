package com.localai.domain.repository

import com.localai.domain.model.DownloadState
import com.localai.domain.model.ModelInfo
import kotlinx.coroutines.flow.Flow

interface AIModelRepository {
    fun fetchRemoteModels(): Flow<List<ModelInfo>>
    fun downloadModel(modelInfo: ModelInfo): Flow<DownloadState>
    suspend fun getLocalModels(): List<ModelInfo>
    suspend fun getLocalPath(modelId: String): String?
    suspend fun deleteModel(modelId: String)
}

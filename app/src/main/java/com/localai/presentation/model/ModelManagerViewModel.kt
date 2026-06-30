package com.localai.presentation.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localai.domain.model.DownloadState
import com.localai.domain.model.ModelInfo
import com.localai.domain.repository.AIModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModelManagerUiState(
    val remoteModels: List<ModelInfo> = emptyList(),
    val isLoadingRemote: Boolean = false,
    val remoteError: String? = null,
    val downloadStates: Map<String, DownloadState> = emptyMap(),
    val selectedModelPath: String? = null
)

@HiltViewModel
class ModelManagerViewModel @Inject constructor(
    private val repository: AIModelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelManagerUiState())
    val uiState: StateFlow<ModelManagerUiState> = _uiState.asStateFlow()

    init {
        fetchModels()
    }

    fun fetchModels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRemote = true, remoteError = null) }
            repository.fetchRemoteModels()
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoadingRemote = false, remoteError = e.message)
                    }
                }
                .collect { models ->
                    _uiState.update {
                        it.copy(remoteModels = models, isLoadingRemote = false)
                    }
                }
        }
    }

    fun downloadModel(modelInfo: ModelInfo) {
        val currentState = _uiState.value.downloadStates[modelInfo.id]
        if (currentState is DownloadState.InProgress) return

        viewModelScope.launch {
            repository.downloadModel(modelInfo)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            downloadStates = it.downloadStates + (modelInfo.id to
                                    DownloadState.Failed(e.message ?: "Download failed"))
                        )
                    }
                }
                .collect { state ->
                    _uiState.update {
                        it.copy(
                            downloadStates = it.downloadStates + (modelInfo.id to state)
                        )
                    }
                }
        }
    }

    fun selectModel(localPath: String) {
        _uiState.update { it.copy(selectedModelPath = localPath) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedModelPath = null) }
    }
}

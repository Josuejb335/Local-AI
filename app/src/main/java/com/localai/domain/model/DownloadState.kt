package com.localai.domain.model

sealed class DownloadState {
    data object Idle : DownloadState()
    data class InProgress(val progress: Float) : DownloadState()
    data class Completed(val localPath: String) : DownloadState()
    data class Failed(val message: String) : DownloadState()
}

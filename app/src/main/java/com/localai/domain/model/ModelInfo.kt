package com.localai.domain.model

data class ModelInfo(
    val id: String,
    val author: String,
    val name: String,
    val description: String,
    val downloads: Long,
    val fileSizeBytes: Long,
    val ggufFileName: String,
    val localPath: String? = null
) {
    val fileSizeFormatted: String
        get() = when {
            fileSizeBytes >= 1_000_000_000 -> "%.1f GB".format(fileSizeBytes / 1_000_000_000.0)
            fileSizeBytes >= 1_000_000 -> "%.0f MB".format(fileSizeBytes / 1_000_000.0)
            fileSizeBytes >= 1_000 -> "%.0f KB".format(fileSizeBytes / 1_000.0)
            else -> "$fileSizeBytes B"
        }

    val downloadsFormatted: String
        get() = when {
            downloads >= 1_000_000 -> "%.1fM".format(downloads / 1_000_000.0)
            downloads >= 1_000 -> "%.1fK".format(downloads / 1_000.0)
            else -> "$downloads"
        }
}

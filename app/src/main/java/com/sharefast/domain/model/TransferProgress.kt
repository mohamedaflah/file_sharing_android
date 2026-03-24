package com.sharefast.domain.model

data class TransferProgress(
    val currentFileIndex: Int,
    val totalFiles: Int,
    val currentFileName: String,
    val bytesTransferred: Long,
    val totalBytes: Long,
    val speedBytesPerSecond: Double,
    val isPaused: Boolean,
)

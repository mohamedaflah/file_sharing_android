package com.sharefast.domain.model

data class TransferHistoryEntry(
    val id: Long = 0,
    val peerName: String,
    val fileName: String,
    val sizeBytes: Long,
    val direction: TransferDirection,
    val timestampEpochMs: Long,
    val success: Boolean,
    /** content:// or FileProvider URI for opening this item from history. */
    val storageUri: String? = null,
)

enum class TransferDirection { SENT, RECEIVED }

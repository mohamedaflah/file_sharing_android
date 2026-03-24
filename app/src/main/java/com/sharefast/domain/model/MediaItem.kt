package com.sharefast.domain.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val displayName: String,
    val uri: Uri,
    val sizeBytes: Long,
    val durationMs: Long? = null,
)

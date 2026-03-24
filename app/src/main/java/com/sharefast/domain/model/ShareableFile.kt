package com.sharefast.domain.model

import android.net.Uri

data class ShareableFile(
    val id: String,
    val displayName: String,
    val sizeBytes: Long,
    val uri: Uri,
    val mimeType: String? = null,
    /** When set, sender reads this path directly (e.g. APK sourceDir). */
    val localPath: String? = null,
)

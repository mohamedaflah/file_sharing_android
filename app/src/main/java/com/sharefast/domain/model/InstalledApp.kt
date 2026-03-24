package com.sharefast.domain.model

import android.graphics.Bitmap

data class InstalledApp(
    val packageName: String,
    val label: String,
    val apkPath: String,
    val sizeBytes: Long,
    val launcherIcon: Bitmap?,
)

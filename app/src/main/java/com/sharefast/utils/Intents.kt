package com.sharefast.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )
}

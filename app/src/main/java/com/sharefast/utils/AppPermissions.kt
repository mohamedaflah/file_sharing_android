package com.sharefast.utils

import android.Manifest
import android.os.Build

object AppPermissions {
    /**
     * Permissions to request on launch (camera is requested when opening QR).
     */
    fun coreLaunchPermissions(): Array<String> = buildList {
        if (Build.VERSION.SDK_INT >= 33) {
            add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
            add(Manifest.permission.READ_MEDIA_IMAGES)
            add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    fun cameraPermission(): Array<String> = arrayOf(Manifest.permission.CAMERA)
}

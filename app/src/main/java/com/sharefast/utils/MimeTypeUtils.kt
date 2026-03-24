package com.sharefast.utils

import android.webkit.MimeTypeMap

private val extToMime = mapOf(
    "jpg" to "image/jpeg",
    "jpeg" to "image/jpeg",
    "png" to "image/png",
    "gif" to "image/gif",
    "webp" to "image/webp",
    "heic" to "image/heic",
    "heif" to "image/heif",
    "mp4" to "video/mp4",
    "mkv" to "video/x-matroska",
    "webm" to "video/webm",
    "3gp" to "video/3gpp",
    "mov" to "video/quicktime",
    "pdf" to "application/pdf",
    "apk" to "application/vnd.android.package-archive",
    "zip" to "application/zip",
)

fun mimeForFileName(fileName: String): String {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    extToMime[ext]?.let { return it }
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        ?: java.net.URLConnection.guessContentTypeFromName(fileName)
        ?: "application/octet-stream"
}

fun fileExtension(fileName: String): String =
    fileName.substringAfterLast('.', "").lowercase()

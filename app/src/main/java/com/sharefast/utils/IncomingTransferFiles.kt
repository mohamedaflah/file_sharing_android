package com.sharefast.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.sharefast.domain.model.ShareableFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "heif")
private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "webm", "3gp", "mov")

/**
 * Publishes a received file so it can be opened from history and (for media) appears in the gallery.
 */
@Singleton
class IncomingTransferFiles @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val authority: String get() = "${context.packageName}.fileprovider"

    /** Content or FileProvider URI string for opening the file. */
    fun publishReceivedFile(privateFile: File, displayName: String): String {
        require(privateFile.exists()) { "Missing $displayName" }
        val ext = fileExtension(displayName)
        val mime = mimeForFileName(displayName)
        return when {
            ext in IMAGE_EXTENSIONS -> publishImage(privateFile, displayName, mime)
            ext in VIDEO_EXTENSIONS -> publishVideo(privateFile, displayName, mime)
            else -> fileProviderUriForPrivate(privateFile).toString()
        }
    }

    /** Best URI string to reopen something we sent (content URI or FileProvider). */
    fun uriStringForSentFile(file: ShareableFile): String? {
        if (file.uri.scheme == "content") return file.uri.toString()
        val path = file.localPath ?: return file.uri.toString().takeIf { it.isNotEmpty() }
        val f = File(path)
        if (!f.exists() || !f.canRead()) return null
        return runCatching { FileProvider.getUriForFile(context, authority, f).toString() }.getOrNull()
    }

    private fun fileProviderUriForPrivate(file: File): Uri {
        val safe = runCatching { file.canonicalFile }.getOrElse { file }
        return FileProvider.getUriForFile(context, authority, safe)
    }

    private fun publishImage(privateFile: File, displayName: String, mime: String): String {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            insertMediaStoreImage(displayName, mime, privateFile)
        } else {
            copyToPublicAndScan(
                privateFile,
                displayName,
                mime,
                Environment.DIRECTORY_PICTURES,
            )
        }
        runCatching { privateFile.delete() }
        return uri.toString()
    }

    private fun publishVideo(privateFile: File, displayName: String, mime: String): String {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            insertMediaStoreVideo(displayName, mime, privateFile)
        } else {
            copyToPublicAndScan(
                privateFile,
                displayName,
                mime,
                Environment.DIRECTORY_MOVIES,
            )
        }
        runCatching { privateFile.delete() }
        return uri.toString()
    }

    private fun insertMediaStoreImage(displayName: String, mime: String, source: File): Uri {
        val resolver = context.contentResolver
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/ShareFast")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(collection, values))
        resolver.openOutputStream(uri)?.use { out ->
            FileInputStream(source).use { it.copyTo(out) }
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    private fun insertMediaStoreVideo(displayName: String, mime: String, source: File): Uri {
        val resolver = context.contentResolver
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, mime)
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/ShareFast")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(collection, values))
        resolver.openOutputStream(uri)?.use { out ->
            FileInputStream(source).use { it.copyTo(out) }
        }
        values.clear()
        values.put(MediaStore.Video.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    private fun copyToPublicAndScan(
        source: File,
        displayName: String,
        mime: String,
        publicDir: String,
    ): Uri {
        @Suppress("DEPRECATION")
        val base = Environment.getExternalStoragePublicDirectory(publicDir)
        val dir = File(base, "ShareFast").apply { mkdirs() }
        var destName = displayName
        if (File(dir, destName).exists()) {
            val stem = displayName.substringBeforeLast('.', displayName)
            val ext = displayName.substringAfterLast('.', "")
            destName = if (ext.isNotEmpty() && ext != displayName) {
                "${stem}_${System.currentTimeMillis()}.$ext"
            } else {
                "${displayName}_${System.currentTimeMillis()}"
            }
        }
        val dest = File(dir, destName)
        source.copyTo(dest, overwrite = true)
        MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf(mime), null)
        val published = runCatching { dest.canonicalFile }.getOrElse { dest }
        return FileProvider.getUriForFile(context, authority, published)
    }
}

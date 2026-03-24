package com.sharefast.data.repository

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.sharefast.domain.model.MediaItem
import com.sharefast.domain.repository.DocumentsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : DocumentsRepository {

    override suspend fun loadDocuments(): List<MediaItem> = withContext(Dispatchers.IO) {
        val mimeTypes = listOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "text/csv",
        )
        val placeholders = mimeTypes.joinToString(",") { "?" }
        val selection = "${MediaStore.MediaColumns.MIME_TYPE} IN ($placeholders)"
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
        )
        val sort = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

        val collections = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL))
            } else {
                @Suppress("DEPRECATION")
                add(MediaStore.Files.getContentUri("external"))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Downloads.EXTERNAL_CONTENT_URI)
            }
        }

        val merged = LinkedHashMap<Long, MediaItem>()
        for (collection in collections) {
            runCatching {
                context.contentResolver.query(
                    collection,
                    projection,
                    selection,
                    mimeTypes.toTypedArray(),
                    sort,
                )?.use { cursor ->
                    val idIdx = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                    val nameIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                    if (idIdx < 0) return@use
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIdx)
                        val name = (if (nameIdx >= 0) cursor.getString(nameIdx) else null) ?: "doc_$id"
                        val size = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0L
                        val uri = ContentUris.withAppendedId(collection, id)
                        merged[id] = MediaItem(id = id, displayName = name, uri = uri, sizeBytes = size)
                    }
                }
            }
        }
        merged.values.toList()
    }
}

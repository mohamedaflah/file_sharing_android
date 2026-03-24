package com.sharefast.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.sharefast.domain.model.MediaItem
import com.sharefast.domain.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : MediaRepository {

    override suspend fun loadImages(): List<MediaItem> = withContext(Dispatchers.IO) {
        runCatching {
            queryMedia(
                collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                idColumn = MediaStore.Images.Media._ID,
                nameColumn = MediaStore.Images.Media.DISPLAY_NAME,
                sizeColumn = MediaStore.Images.Media.SIZE,
                durationColumn = null,
                sortColumn = MediaStore.Images.Media.DATE_ADDED,
                mimeLike = null,
            )
        }.getOrElse { emptyList() }
    }

    override suspend fun loadVideos(): List<MediaItem> = withContext(Dispatchers.IO) {
        runCatching {
            queryMedia(
                collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                idColumn = MediaStore.Video.Media._ID,
                nameColumn = MediaStore.Video.Media.DISPLAY_NAME,
                sizeColumn = MediaStore.Video.Media.SIZE,
                durationColumn = MediaStore.Video.Media.DURATION,
                sortColumn = MediaStore.Video.Media.DATE_ADDED,
                mimeLike = null,
            )
        }.getOrElse { emptyList() }
    }

    private fun queryMedia(
        collection: android.net.Uri,
        idColumn: String,
        nameColumn: String,
        sizeColumn: String,
        durationColumn: String?,
        sortColumn: String,
        mimeLike: String?,
    ): List<MediaItem> {
        val resolver = context.contentResolver
        val projection = buildList {
            add(idColumn)
            add(nameColumn)
            add(sizeColumn)
            if (durationColumn != null) add(durationColumn)
        }.toTypedArray()

        val selection = if (mimeLike != null) "${MediaStore.MediaColumns.MIME_TYPE} LIKE ?" else null
        val args = if (mimeLike != null) arrayOf(mimeLike) else null
        val sort = "$sortColumn DESC"
        val list = mutableListOf<MediaItem>()
        try {
            resolver.query(collection, projection, selection, args, sort)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(idColumn)
                if (idIdx < 0) return@use
                val nameIdx = cursor.getColumnIndex(nameColumn).takeIf { it >= 0 }
                    ?: cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(sizeColumn)
                val durIdx = durationColumn?.let { cursor.getColumnIndex(it) }?.takeIf { it >= 0 } ?: -1
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val name = when {
                        nameIdx >= 0 -> cursor.getString(nameIdx)
                        else -> null
                    } ?: "image_$id"
                    val size = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0L
                    val dur = if (durIdx >= 0) cursor.getLong(durIdx) else null
                    val uri = ContentUris.withAppendedId(collection, id)
                    list.add(MediaItem(id = id, displayName = name, uri = uri, sizeBytes = size, durationMs = dur))
                }
            }
        } catch (_: SecurityException) {
            return emptyList()
        }
        return list
    }
}

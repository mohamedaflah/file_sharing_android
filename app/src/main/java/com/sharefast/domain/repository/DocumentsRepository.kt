package com.sharefast.domain.repository

import com.sharefast.domain.model.MediaItem

interface DocumentsRepository {
    suspend fun loadDocuments(): List<MediaItem>
}

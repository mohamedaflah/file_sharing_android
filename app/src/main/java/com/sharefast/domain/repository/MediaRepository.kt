package com.sharefast.domain.repository

import com.sharefast.domain.model.MediaItem

interface MediaRepository {
    suspend fun loadImages(): List<MediaItem>
    suspend fun loadVideos(): List<MediaItem>
}

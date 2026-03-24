package com.sharefast.presentation.tabs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharefast.data.repository.SendQueueStore
import com.sharefast.domain.model.MediaItem
import com.sharefast.domain.model.ShareableFile
import com.sharefast.domain.repository.MediaRepository
import com.sharefast.utils.Feedback
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val mediaRepository: MediaRepository,
    private val sendQueueStore: SendQueueStore,
) : ViewModel() {

    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    val items: StateFlow<List<MediaItem>> = _items.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    val selected: StateFlow<Set<Long>> = _selected.asStateFlow()

    fun load(kind: MediaKind) {
        viewModelScope.launch {
            _loading.value = true
            try {
                _items.value = runCatching {
                    when (kind) {
                        MediaKind.IMAGES -> mediaRepository.loadImages()
                        MediaKind.VIDEOS -> mediaRepository.loadVideos()
                    }
                }.getOrElse { emptyList() }
            } finally {
                _loading.value = false
            }
        }
    }

    fun toggle(id: Long) {
        val cur = _selected.value.toMutableSet()
        if (!cur.add(id)) cur.remove(id)
        _selected.value = cur
    }

    fun clearSelection() {
        _selected.value = emptySet()
    }

    fun addSelectedToQueue(kind: MediaKind) {
        val map = _items.value.associateBy { it.id }
        val files = _selected.value.mapNotNull { map[it]?.toShareable() }
        sendQueueStore.addAll(files)
        if (files.isNotEmpty()) Feedback.vibrateQueueAdd(appContext)
        clearSelection()
    }

    private fun MediaItem.toShareable() = ShareableFile(
        id = id.toString(),
        displayName = displayName,
        sizeBytes = sizeBytes,
        uri = uri,
    )
}

package com.sharefast.presentation.tabs

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharefast.data.repository.SendQueueStore
import com.sharefast.domain.model.MediaItem
import com.sharefast.domain.model.ShareableFile
import com.sharefast.domain.repository.DocumentsRepository
import com.sharefast.utils.Feedback
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val documentsRepository: DocumentsRepository,
    private val sendQueueStore: SendQueueStore,
) : ViewModel() {

    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    val items: StateFlow<List<MediaItem>> = _items.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    val selected: StateFlow<Set<Long>> = _selected.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            try {
                _items.value = runCatching { documentsRepository.loadDocuments() }.getOrElse { emptyList() }
            } finally {
                _loading.value = false
            }
        }
    }

    fun importUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val resolver = context.contentResolver
        val files = uris.map { uri ->
            var name: String? = null
            var size = 0L
            runCatching {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                    ?.use { c ->
                        if (c.moveToFirst()) {
                            val nIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sIdx = c.getColumnIndex(OpenableColumns.SIZE)
                            if (nIdx >= 0) name = c.getString(nIdx)
                            if (sIdx >= 0) size = c.getLong(sIdx)
                        }
                    }
            }
            ShareableFile(
                id = uri.toString(),
                displayName = name ?: uri.lastPathSegment ?: "document",
                sizeBytes = size,
                uri = uri,
            )
        }
        sendQueueStore.addAll(files)
        if (files.isNotEmpty()) Feedback.vibrateQueueAdd(context)
    }

    fun toggle(id: Long) {
        val cur = _selected.value.toMutableSet()
        if (!cur.add(id)) cur.remove(id)
        _selected.value = cur
    }

    fun clearSelection() {
        _selected.value = emptySet()
    }

    fun addSelectedToQueue() {
        val map = _items.value.associateBy { it.id }
        val files = _selected.value.mapNotNull { map[it]?.toShareable() }
        sendQueueStore.addAll(files)
        if (files.isNotEmpty()) Feedback.vibrateQueueAdd(context)
        clearSelection()
    }

    private fun MediaItem.toShareable() = ShareableFile(
        id = id.toString(),
        displayName = displayName,
        sizeBytes = sizeBytes,
        uri = uri,
    )
}

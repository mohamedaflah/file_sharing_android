package com.sharefast.data.repository

import com.sharefast.domain.model.ShareableFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SendQueueStore @Inject constructor() {
    private val _items = MutableStateFlow<List<ShareableFile>>(emptyList())
    val items: StateFlow<List<ShareableFile>> = _items.asStateFlow()

    fun addAll(files: List<ShareableFile>) {
        if (files.isEmpty()) return
        _items.value = (_items.value + files).distinctBy { it.id }
    }

    fun remove(file: ShareableFile) {
        _items.value = _items.value.filter { it.id != file.id && it.uri != file.uri }
    }

    fun removeAt(index: Int) {
        val list = _items.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _items.value = list
        }
    }

    fun move(fromIndex: Int, toIndex: Int) {
        val list = _items.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _items.value = list
    }

    fun clear() {
        _items.value = emptyList()
    }
}

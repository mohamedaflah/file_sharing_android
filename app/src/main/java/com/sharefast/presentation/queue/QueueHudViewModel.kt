package com.sharefast.presentation.queue

import androidx.lifecycle.ViewModel
import com.sharefast.data.repository.SendQueueStore
import com.sharefast.domain.model.ShareableFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class QueueHudViewModel @Inject constructor(
    private val sendQueueStore: SendQueueStore,
) : ViewModel() {
    val items: StateFlow<List<ShareableFile>> = sendQueueStore.items

    fun removeAt(index: Int) = sendQueueStore.removeAt(index)

    fun moveUp(index: Int) {
        if (index > 0) sendQueueStore.move(index, index - 1)
    }

    fun moveDown(index: Int) {
        val n = items.value.size
        if (index < n - 1) sendQueueStore.move(index, index + 1)
    }
}

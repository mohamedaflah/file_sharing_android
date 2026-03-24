package com.sharefast.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharefast.domain.model.TransferDirection
import com.sharefast.domain.model.TransferHistoryEntry
import com.sharefast.domain.repository.TransferHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransferToastUi(
    val id: Long,
    val direction: TransferDirection,
    val peerName: String,
    val fileCount: Int,
    val files: List<TransferHistoryEntry>,
)

@HiltViewModel
class TransferToastViewModel @Inject constructor(
    transferHistoryRepository: TransferHistoryRepository,
) : ViewModel() {
    private val _toast = MutableStateFlow<TransferToastUi?>(null)
    val toast: StateFlow<TransferToastUi?> = _toast.asStateFlow()

    private var latestSeenTimestamp = System.currentTimeMillis()

    init {
        viewModelScope.launch {
            transferHistoryRepository.history().collectLatest { history ->
                val newItems = history
                    .filter { it.success && it.timestampEpochMs > latestSeenTimestamp }
                    .sortedBy { it.timestampEpochMs }
                if (newItems.isEmpty()) return@collectLatest
                latestSeenTimestamp = newItems.maxOf { it.timestampEpochMs }
                val latest = newItems.last()
                val batch = newItems.filter {
                    it.direction == latest.direction &&
                        it.peerName == latest.peerName
                }
                _toast.value = TransferToastUi(
                    id = latest.timestampEpochMs,
                    direction = latest.direction,
                    peerName = latest.peerName,
                    fileCount = batch.size,
                    files = batch,
                )
            }
        }
    }

    fun dismiss() {
        _toast.value = null
    }
}


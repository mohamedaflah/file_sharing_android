package com.sharefast.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharefast.data.repository.ChatMessage
import com.sharefast.data.repository.ChatRepository
import com.sharefast.domain.model.DiscoverySource
import com.sharefast.domain.model.PeerDevice
import com.sharefast.services.transfer.TcpTransferEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val tcpTransferEngine: TcpTransferEngine,
) : ViewModel() {
    val peerKey: String = checkNotNull(savedStateHandle.get<String>("peerKey"))
    val peerName: String = savedStateHandle.get<String>("peerName") ?: "Device"
    private val host: String = savedStateHandle.get<String>("host") ?: ""
    private val port: Int = savedStateHandle.get<String>("port")?.toIntOrNull() ?: 0

    val messages: StateFlow<List<ChatMessage>> =
        chatRepository.thread(peerKey).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun clearError() {
        _error.value = null
    }

    fun send(text: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        val peer = PeerDevice(
            id = peerKey,
            displayName = peerName,
            hostAddress = host,
            port = port,
            source = DiscoverySource.UDP,
        )
        viewModelScope.launch {
            tcpTransferEngine.sendChatMessage(peer, body).onFailure {
                _error.value = it.message ?: "Message failed"
            }
        }
    }
}


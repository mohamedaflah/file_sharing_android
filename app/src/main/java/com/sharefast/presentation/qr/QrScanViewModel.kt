package com.sharefast.presentation.qr

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharefast.data.repository.LastConnectedPeerStore
import com.sharefast.data.repository.SendQueueStore
import com.sharefast.domain.model.DiscoverySource
import com.sharefast.domain.model.PeerDevice
import com.sharefast.services.transfer.TcpTransferEngine
import com.sharefast.utils.Feedback
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface QrScanPhase {
    data object Scanning : QrScanPhase
    data object Connecting : QrScanPhase
}

@HiltViewModel
class QrScanViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val sendQueueStore: SendQueueStore,
    private val tcpTransferEngine: TcpTransferEngine,
    private val lastConnectedPeerStore: LastConnectedPeerStore,
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _phase = MutableStateFlow<QrScanPhase>(QrScanPhase.Scanning)
    val phase: StateFlow<QrScanPhase> = _phase.asStateFlow()

    private val _closeUi = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val closeUi = _closeUi.asSharedFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun notifyInvalidQr() {
        _message.value = "This QR is not a ShareFast connection code."
    }

    fun onQrDecoded(host: String, port: Int) {
        viewModelScope.launch {
            val files = sendQueueStore.items.value
            if (files.isEmpty()) {
                _message.value = "Add files to the send queue from the tabs first."
                return@launch
            }
            _phase.value = QrScanPhase.Connecting
            Feedback.vibrateSuccess(appContext)
            Feedback.playQrScannedChime()
            delay(420)
            val peer = PeerDevice(
                id = "qr-$host-$port",
                displayName = "QR device",
                hostAddress = host,
                port = port,
                source = DiscoverySource.UDP,
            )
            runCatching {
                tcpTransferEngine.sendFiles(peer, files, peer.displayName)
            }.onSuccess {
                lastConnectedPeerStore.save(host, port, peer.displayName)
                Feedback.playSuccessTone()
                sendQueueStore.clear()
                delay(280)
                _closeUi.emit(Unit)
            }.onFailure {
                _message.value = it.message ?: "Transfer failed"
            }
            _phase.value = QrScanPhase.Scanning
        }
    }
}

package com.sharefast.presentation.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharefast.core.ShareConstants
import com.sharefast.domain.model.DiscoverySource
import com.sharefast.domain.model.PeerDevice
import com.sharefast.domain.repository.DeviceRepository
import com.sharefast.domain.repository.DiscoveryRepository
import com.sharefast.domain.model.ShareableFile
import com.sharefast.domain.model.TransferDirection
import com.sharefast.domain.model.TransferHistoryEntry
import com.sharefast.domain.repository.TransferHistoryRepository
import com.sharefast.data.repository.LastConnectedPeerStore
import com.sharefast.data.repository.SavedPeer
import com.sharefast.data.repository.SendQueueStore
import com.sharefast.services.transfer.TcpTransferEngine
import com.sharefast.services.transfer.TransferForegroundService
import com.sharefast.utils.Feedback
import com.sharefast.utils.NetworkUtils
import com.sharefast.utils.OpenUriHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

data class HomeUiState(
    val deviceName: String = "",
    val ipAddress: String? = null,
    val connectionLabel: String = "",
    val peers: List<PeerDevice> = emptyList(),
    val discoveryActive: Boolean = false,
    val queueCount: Int = 0,
    val wifiBars: Int = 0,
    val error: String? = null,
)

private data class HomeNetSlice(
    val ip: String?,
    val conn: String,
    val disc: Boolean,
    val err: String?,
    val wifiBars: Int,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val deviceRepository: DeviceRepository,
    private val discoveryRepository: DiscoveryRepository,
    private val transferHistoryRepository: TransferHistoryRepository,
    private val sendQueueStore: SendQueueStore,
    private val tcpTransferEngine: TcpTransferEngine,
    private val lastConnectedPeerStore: LastConnectedPeerStore,
) : ViewModel() {

    private val _ip = MutableStateFlow<String?>(null)
    private val _conn = MutableStateFlow("")
    private val _wifiBars = MutableStateFlow(0)
    private val _discoveryOn = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val lanStarted = AtomicBoolean(false)

    val recentTransfers: StateFlow<List<TransferHistoryEntry>> =
        transferHistoryRepository.history()
            .map { it.take(12) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<HomeUiState> = combine(
        combine(
            deviceRepository.deviceDisplayName,
            discoveryRepository.peers,
            sendQueueStore.items,
        ) { name, peers, queue -> Triple(name, peers, queue) },
        combine(_ip, _conn, _discoveryOn, _error, _wifiBars) { ip, conn, disc, err, wifi ->
            HomeNetSlice(ip, conn, disc, err, wifi)
        },
    ) { main, net ->
        HomeUiState(
            deviceName = main.first,
            peers = main.second,
            queueCount = main.third.size,
            ipAddress = net.ip,
            connectionLabel = net.conn,
            discoveryActive = net.disc,
            wifiBars = net.wifiBars,
            error = net.err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    val lastPeer: StateFlow<SavedPeer?> =
        lastConnectedPeerStore.savedPeer.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null,
        )

    val transferProgress = tcpTransferEngine.progress

    init {
        viewModelScope.launch {
            while (true) {
                _ip.value = NetworkUtils.localIpv4Address(appContext)
                _conn.value = NetworkUtils.connectionLabel(appContext)
                _wifiBars.value = NetworkUtils.wifiSignalBars(appContext)
                delay(2_000)
            }
        }
    }

    /**
     * Starts TCP listen, optional foreground notification, and LAN discovery after runtime permissions
     * have been requested (avoids crashes from starting FG service before the activity is ready).
     */
    fun startLanFeaturesIfNeeded() {
        if (!lanStarted.compareAndSet(false, true)) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { tcpTransferEngine.ensureServerRunning() }
            runCatching { TransferForegroundService.start(appContext) }
            runCatching {
                discoveryRepository.startDiscovery(ShareConstants.TCP_PORT)
                _discoveryOn.value = true
            }.onFailure {
                _discoveryOn.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun renameDevice(name: String) {
        viewModelScope.launch {
            deviceRepository.setDeviceDisplayName(name)
        }
    }

    fun sendTo(peer: PeerDevice) {
        viewModelScope.launch {
            val files = sendQueueStore.items.value
            if (files.isEmpty()) {
                _error.value = "Select files from tabs and add them to the send queue first."
                return@launch
            }
            try {
                tcpTransferEngine.sendFiles(peer, files, peer.displayName)
                Feedback.playSuccessTone()
                sendQueueStore.clear()
                lastConnectedPeerStore.saveFromPeer(peer)
            } catch (e: Throwable) {
                _error.value = e.message ?: "Transfer failed"
            }
        }
    }

    fun onPeerTapped(peer: PeerDevice) {
        Feedback.vibrateConnect(appContext)
        sendTo(peer)
    }

    fun sendTextTo(peer: PeerDevice, message: String) {
        val text = message.trim()
        if (text.isEmpty()) {
            _error.value = "Message can't be empty."
            return
        }
        viewModelScope.launch {
            val result = tcpTransferEngine.sendTextRequest(peer, text)
            result.onFailure {
                _error.value = it.message ?: "Message request failed"
            }
        }
    }

    fun reconnectLastPeer() {
        val s = lastPeer.value ?: return
        val peer = PeerDevice(
            id = "saved-${s.hostAddress}-${s.port}",
            displayName = s.displayName,
            hostAddress = s.hostAddress,
            port = s.port,
            source = DiscoverySource.UDP,
        )
        onPeerTapped(peer)
    }

    fun resendToQueue(entry: TransferHistoryEntry) {
        if (entry.direction != TransferDirection.SENT || entry.storageUri.isNullOrBlank()) {
            _error.value = "Only sent files with a saved copy can be added again."
            return
        }
        val file = ShareableFile(
            id = "resend-${entry.id}-${System.currentTimeMillis()}",
            displayName = entry.fileName,
            sizeBytes = entry.sizeBytes,
            uri = Uri.parse(entry.storageUri.trim()),
        )
        sendQueueStore.addAll(listOf(file))
        Feedback.vibrateQueueAdd(appContext)
    }

    fun pauseTransfer() = tcpTransferEngine.pause()

    fun resumeTransfer() = tcpTransferEngine.resume()

    fun cancelTransfer() = tcpTransferEngine.cancel()

    fun openTransferEntry(entry: TransferHistoryEntry) {
        val uriStr = entry.storageUri
        if (uriStr.isNullOrBlank()) {
            _error.value = "This transfer has no openable file on this device."
            return
        }
        openUriForFile(uriStr, entry.fileName, onFinished = null)
    }

    fun openUriForFile(
        uriStr: String,
        fileNameHint: String,
        onFinished: (() -> Unit)? = null,
    ) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            try {
                val uri = Uri.parse(uriStr.trim())
                val ok = OpenUriHelper.tryOpenWithView(appContext, uri, fileNameHint)
                if (!ok) {
                    _error.value =
                        "No app can open this file. Install a viewer (Photos, Files, or a PDF app) and try again."
                }
            } catch (_: Throwable) {
                _error.value = "Invalid file link. Re-receive the file if this keeps happening."
            } finally {
                onFinished?.invoke()
            }
        }
    }
}

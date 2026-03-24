package com.sharefast.data.repository

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.sharefast.core.ShareConstants
import com.sharefast.di.ApplicationScope
import com.sharefast.domain.model.DiscoverySource
import com.sharefast.domain.model.PeerDevice
import com.sharefast.domain.repository.DeviceRepository
import com.sharefast.domain.repository.DiscoveryRepository
import com.sharefast.services.discovery.UdpAnnouncePacket
import com.sharefast.services.discovery.decodeUdpPacket
import com.sharefast.services.discovery.toUdpBytes
import com.sharefast.utils.NetworkUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscoveryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRepository: DeviceRepository,
    @ApplicationScope private val scope: CoroutineScope,
) : DiscoveryRepository {

    private val _peers = MutableStateFlow<List<PeerDevice>>(emptyList())
    override val peers: Flow<List<PeerDevice>> = _peers.asStateFlow()

    private val peerMap = LinkedHashMap<String, PeerDevice>()
    private var udpSocket: DatagramSocket? = null
    private var listenJob: Job? = null
    private var broadcastJob: Job? = null
    private var advertisePort: Int = 0

    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var multicastLock: android.net.wifi.WifiManager.MulticastLock? = null

    override suspend fun startDiscovery(advertisePort: Int) {
        this.advertisePort = advertisePort
        stopDiscoveryInternal()
        multicastLock = NetworkUtils.wifiManager(context).createMulticastLock("sharefast-mdns").apply {
            setReferenceCounted(false)
            acquire()
        }
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        startUdp()
        startNsd(advertisePort)
    }

    override suspend fun stopDiscovery() {
        stopDiscoveryInternal()
    }

    private suspend fun stopDiscoveryInternal() = withContext(Dispatchers.IO) {
        listenJob?.cancel()
        broadcastJob?.cancel()
        listenJob = null
        broadcastJob = null
        runCatching { udpSocket?.close() }
        udpSocket = null
        runCatching {
            registrationListener?.let { nsdManager?.unregisterService(it) }
        }
        runCatching {
            discoveryListener?.let { nsdManager?.stopServiceDiscovery(it) }
        }
        registrationListener = null
        discoveryListener = null
        nsdManager = null
        runCatching { multicastLock?.release() }
        multicastLock = null
        peerMap.clear()
        _peers.value = emptyList()
    }

    private fun startUdp() {
        val socket = runCatching {
            DatagramSocket(ShareConstants.UDP_DISCOVERY_PORT).apply {
                reuseAddress = true
                broadcast = true
            }
        }.getOrNull() ?: return
        udpSocket = socket
        listenJob = scope.launch(Dispatchers.IO) {
            val myId = deviceRepository.localDeviceId()
            val buf = ByteArray(4096)
            while (isActive) {
                try {
                    val packet = DatagramPacket(buf, buf.size)
                    socket.receive(packet)
                    val parsed = decodeUdpPacket(buf, packet.length) ?: continue
                    if (parsed.type != "SF_ANN" || parsed.port <= 0) continue
                    val host = packet.address.hostAddress ?: continue
                    if (parsed.id == myId) continue
                    upsert(
                        PeerDevice(
                            id = parsed.id,
                            displayName = parsed.name,
                            hostAddress = host,
                            port = parsed.port,
                            source = DiscoverySource.UDP,
                        ),
                    )
                } catch (_: Exception) {
                    if (!isActive) break
                }
            }
        }

        broadcastJob = scope.launch(Dispatchers.IO) {
            val socket = udpSocket ?: return@launch
            while (isActive) {
                try {
                    if (advertisePort > 0) {
                        val id = deviceRepository.localDeviceId()
                        val name = deviceRepository.deviceDisplayName.first()
                        val payload = UdpAnnouncePacket(
                            id = id,
                            name = name,
                            port = advertisePort,
                        ).toUdpBytes()
                        val addresses = broadcastTargets()
                        for (addr in addresses) {
                            val pkt = DatagramPacket(payload, payload.size, addr, ShareConstants.UDP_DISCOVERY_PORT)
                            runCatching { socket.send(pkt) }
                        }
                    }
                } catch (_: Exception) {
                    if (!isActive) break
                }
                delay(2_500)
            }
        }
    }

    private fun broadcastTargets(): List<InetAddress> =
        listOf(InetAddress.getByName("255.255.255.255"))

    private fun upsert(peer: PeerDevice) {
        peerMap[peer.id] = peer.copy(lastSeenEpochMs = System.currentTimeMillis())
        _peers.value = peerMap.values.sortedByDescending { it.lastSeenEpochMs }
    }

    private fun startNsd(port: Int) {
        val mgr = nsdManager ?: return
        if (port > 0) {
            val serviceInfo = NsdServiceInfo().apply {
                serviceType = ShareConstants.NSD_SERVICE_TYPE
                serviceName = "ShareFast"
                setPort(port)
            }
            registrationListener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(info: NsdServiceInfo) {}
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}
                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            }
            runCatching {
                mgr.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
            }
        }

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                mgr.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                    override fun onServiceResolved(resolved: NsdServiceInfo) {
                        val host = resolved.host?.hostAddress ?: return
                        val myIp = NetworkUtils.localIpv4Address(context)
                        if (myIp != null && host == myIp) return
                        val id = "nsd-${resolved.serviceName}-${host}"
                        upsert(
                            PeerDevice(
                                id = id,
                                displayName = resolved.serviceName.ifBlank { "Device" },
                                hostAddress = host,
                                port = resolved.port,
                                source = DiscoverySource.NSD,
                            ),
                        )
                    }
                })
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                // Optional: remove by name — skipped for simplicity
            }
        }
        runCatching {
            mgr.discoverServices(ShareConstants.NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        }
    }
}

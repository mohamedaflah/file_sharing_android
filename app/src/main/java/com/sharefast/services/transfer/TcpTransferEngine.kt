package com.sharefast.services.transfer

import android.content.Context
import com.sharefast.core.ShareConstants
import com.sharefast.core.network.FileDescriptorWire
import com.sharefast.core.network.WireMessage
import com.sharefast.core.network.parseWireMessage
import com.sharefast.core.network.readJsonPayload
import com.sharefast.core.network.toWireJson
import com.sharefast.core.network.writeJsonPayload
import com.sharefast.data.repository.ChatRepository
import com.sharefast.di.ApplicationScope
import com.sharefast.domain.model.PeerDevice
import com.sharefast.domain.model.ShareableFile
import com.sharefast.domain.model.TransferDirection
import com.sharefast.domain.model.TransferHistoryEntry
import com.sharefast.domain.model.TransferProgress
import com.sharefast.domain.repository.DeviceRepository
import com.sharefast.domain.repository.TransferHistoryRepository
import com.sharefast.utils.Feedback
import com.sharefast.utils.IncomingTransferFiles
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TcpTransferEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRepository: DeviceRepository,
    private val transferHistoryRepository: TransferHistoryRepository,
    private val incomingTransferFiles: IncomingTransferFiles,
    private val transferNotifications: TransferNotifications,
    private val approvalCoordinator: IncomingTransferApprovalCoordinator,
    private val chatRepository: ChatRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val paused = AtomicBoolean(false)
    /** Only for the active send/receive stream — never stops the TCP accept loop. */
    private val transferCancelled = AtomicBoolean(false)

    private val _progress = MutableStateFlow<TransferProgress?>(null)
    val progress: StateFlow<TransferProgress?> = _progress.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var activeSocket: Socket? = null

    fun pause() {
        paused.set(true)
        updatePausedFlag()
    }

    fun resume() {
        paused.set(false)
        updatePausedFlag()
    }

    fun cancel() {
        transferCancelled.set(true)
        runCatching { activeSocket?.close() }
    }

    private fun updatePausedFlag() {
        val cur = _progress.value ?: return
        _progress.value = cur.copy(isPaused = paused.get())
    }

    fun ensureServerRunning() {
        if (serverJob?.isActive == true) return
        serverJob = scope.launch(Dispatchers.IO) {
            var ss: ServerSocket? = null
            try {
                ss = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(ShareConstants.TCP_PORT))
                }
                serverSocket = ss
                while (isActive) {
                    val client = runCatching { ss.accept() }.getOrNull() ?: break
                    scope.launch(Dispatchers.IO) {
                        handleIncoming(client)
                    }
                }
            } finally {
                runCatching { ss?.close() }
                if (serverSocket === ss) serverSocket = null
            }
        }
    }

    fun stopServer() {
        runCatching { serverSocket?.close() }
        serverJob?.cancel()
        serverJob = null
        serverSocket = null
    }

    suspend fun sendFiles(peer: PeerDevice, files: List<ShareableFile>, peerDisplayName: String) =
        withContext(Dispatchers.IO) {
            transferCancelled.set(false)
            paused.set(false)
            if (files.isEmpty()) return@withContext
            val totalBytes = files.sumOf { it.sizeBytes }
            var transferred = 0L
            val speedEwma = SpeedMeter()

            repeat(3) { attempt ->
                if (transferCancelled.get()) return@withContext
                try {
                    val socket = Socket()
                    activeSocket = socket
                    socket.connect(InetSocketAddress(peer.hostAddress, peer.port), 12_000)
                    val dis = DataInputStream(socket.getInputStream())
                    val dos = DataOutputStream(socket.getOutputStream())
                    val myId = deviceRepository.localDeviceId()
                    val myName = deviceRepository.deviceDisplayName.first()
                    dos.writeJsonPayload(
                        WireMessage(command = "HELLO", deviceId = myId, deviceName = myName).toWireJson(),
                    )
                    val helloAckRaw = dis.readJsonPayload()
                    val helloAck = parseWireMessage(helloAckRaw)
                    if (helloAck.command != "HELLO_ACK") error("Handshake failed")

                    val manifest = WireMessage(
                        command = "MANIFEST",
                        files = files.map { FileDescriptorWire(name = it.displayName, size = it.sizeBytes) },
                    )
                    dos.writeJsonPayload(manifest.toWireJson())
                    val manAckRaw = dis.readJsonPayload()
                    val manAck = parseWireMessage(manAckRaw)
                    when (manAck.command) {
                        "MANIFEST_OK" -> Unit
                        "MANIFEST_REJECT" -> throw ReceiverDeclinedException(
                            manAck.error ?: "Request declined by receiver",
                        )
                        else -> error("Manifest rejected")
                    }

                    for ((index, file) in files.withIndex()) {
                        if (transferCancelled.get()) return@withContext
                        waitWhilePaused()
                        streamFileOut(dos, file) { chunk ->
                            transferred += chunk
                            val speed = speedEwma.update(chunk)
                            _progress.value = TransferProgress(
                                currentFileIndex = index + 1,
                                totalFiles = files.size,
                                currentFileName = file.displayName,
                                bytesTransferred = transferred,
                                totalBytes = totalBytes,
                                speedBytesPerSecond = speed,
                                isPaused = paused.get(),
                            )
                        }
                        val sentUri = incomingTransferFiles.uriStringForSentFile(file)
                        transferHistoryRepository.insert(
                            TransferHistoryEntry(
                                peerName = peerDisplayName,
                                fileName = file.displayName,
                                sizeBytes = file.sizeBytes,
                                direction = TransferDirection.SENT,
                                timestampEpochMs = System.currentTimeMillis(),
                                success = true,
                                storageUri = sentUri,
                            ),
                        )
                    }
                    activeSocket = null
                    runCatching { socket.close() }
                    _progress.value = null
                    transferNotifications.notifySendComplete(peerDisplayName, files.size)
                    withContext(Dispatchers.Main) {
                        Feedback.vibrateSuccess(context)
                        Feedback.playSuccessTone()
                    }
                    return@withContext
                } catch (e: Exception) {
                    if (transferCancelled.get()) {
                        _progress.value = null
                        throw IllegalStateException("Transfer canceled")
                    }
                    if (e is ReceiverDeclinedException) {
                        _progress.value = null
                        throw e
                    }
                    if (attempt == 2 || transferCancelled.get()) {
                        _progress.value = null
                        throw IllegalStateException("Could not connect after retries")
                    }
                    delay(800L * (attempt + 1))
                }
            }
        }

    suspend fun sendTextRequest(peer: PeerDevice, message: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val socket = Socket()
            try {
                socket.connect(InetSocketAddress(peer.hostAddress, peer.port), 10_000)
                val dis = DataInputStream(socket.getInputStream())
                val dos = DataOutputStream(socket.getOutputStream())
                val myId = deviceRepository.localDeviceId()
                val myName = deviceRepository.deviceDisplayName.first()
                dos.writeJsonPayload(
                    WireMessage(command = "HELLO", deviceId = myId, deviceName = myName).toWireJson(),
                )
                val helloAck = parseWireMessage(dis.readJsonPayload())
                if (helloAck.command != "HELLO_ACK") error("Handshake failed")
                dos.writeJsonPayload(WireMessage(command = "TEXT_REQUEST", error = message.take(280)).toWireJson())
                val ack = parseWireMessage(dis.readJsonPayload())
                when (ack.command) {
                    "TEXT_ACCEPT" -> Unit
                    "TEXT_DECLINE" -> throw ReceiverDeclinedException(ack.error ?: "Request declined by receiver")
                    else -> error("Invalid response")
                }
            } finally {
                runCatching { socket.close() }
            }
        }
    }

    suspend fun sendChatMessage(peer: PeerDevice, message: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = message.trim().take(1000)
            require(body.isNotEmpty()) { "Empty message" }
            val socket = Socket()
            try {
                socket.connect(InetSocketAddress(peer.hostAddress, peer.port), 10_000)
                val dis = DataInputStream(socket.getInputStream())
                val dos = DataOutputStream(socket.getOutputStream())
                val myId = deviceRepository.localDeviceId()
                val myName = deviceRepository.deviceDisplayName.first()
                dos.writeJsonPayload(
                    WireMessage(command = "HELLO", deviceId = myId, deviceName = myName).toWireJson(),
                )
                val helloAck = parseWireMessage(dis.readJsonPayload())
                if (helloAck.command != "HELLO_ACK") error("Handshake failed")
                dos.writeJsonPayload(WireMessage(command = "CHAT_MESSAGE", error = body).toWireJson())
                val ack = parseWireMessage(dis.readJsonPayload())
                if (ack.command != "CHAT_ACK") error(ack.error ?: "Message failed")
                chatRepository.insertOutgoing(
                    peerKey = peer.id,
                    peerName = peer.displayName,
                    body = body,
                )
            } finally {
                runCatching { socket.close() }
            }
        }
    }

    private suspend fun waitWhilePaused() {
        while (paused.get() && !transferCancelled.get()) {
            delay(120)
            updatePausedFlag()
        }
    }

    private fun streamFileOut(
        dos: DataOutputStream,
        file: ShareableFile,
        onChunk: (Int) -> Unit,
    ) {
        val nameBytes = file.displayName.toByteArray(Charsets.UTF_8)
        dos.writeInt(nameBytes.size)
        dos.write(nameBytes)
        dos.writeLong(file.sizeBytes)
        val stream: InputStream? = file.localPath?.let { FileInputStream(File(it)) }
            ?: context.contentResolver.openInputStream(file.uri)
        stream.use { ins ->
            requireNotNull(ins) { "Cannot read ${file.displayName}" }
            copyStream(ins, dos, onChunk)
        }
        dos.flush()
    }

    private fun copyStream(input: InputStream, dos: DataOutputStream, onChunk: (Int) -> Unit) {
        val buf = ByteArray(ShareConstants.STREAM_BUFFER)
        while (true) {
            if (transferCancelled.get()) return
            while (paused.get() && !transferCancelled.get()) {
                Thread.sleep(50)
            }
            val r = input.read(buf)
            if (r <= 0) break
            dos.write(buf, 0, r)
            onChunk(r)
        }
    }

    private suspend fun handleIncoming(socket: Socket) = withContext(Dispatchers.IO) {
        transferCancelled.set(false)
        paused.set(false)
        activeSocket = socket
        try {
            val dis = DataInputStream(socket.getInputStream())
            val dos = DataOutputStream(socket.getOutputStream())
            val helloRaw = dis.readJsonPayload()
            val hello = parseWireMessage(helloRaw)
            if (hello.command != "HELLO") error("Expected HELLO")
            dos.writeJsonPayload(WireMessage(command = "HELLO_ACK").toWireJson())

            val manRaw = dis.readJsonPayload()
            val manifest = parseWireMessage(manRaw)
            if (manifest.command == "TEXT_REQUEST") {
                val peerName = hello.deviceName ?: "Peer"
                approvalCoordinator.showTextToast(
                    deviceName = peerName,
                    message = manifest.error.orEmpty(),
                    peerKey = hello.deviceId ?: "${socket.inetAddress.hostAddress}:${socket.port}",
                    peerHost = socket.inetAddress.hostAddress ?: "",
                    peerPort = ShareConstants.TCP_PORT,
                )
                dos.writeJsonPayload(WireMessage(command = "TEXT_ACCEPT").toWireJson())
                return@withContext
            }
            if (manifest.command == "CHAT_MESSAGE") {
                val peerName = hello.deviceName ?: "Peer"
                val peerKey = hello.deviceId ?: "${socket.inetAddress.hostAddress}:${socket.port}"
                val peerHost = socket.inetAddress.hostAddress ?: ""
                val peerPort = ShareConstants.TCP_PORT
                chatRepository.insertIncoming(
                    peerKey = peerKey,
                    peerName = peerName,
                    body = manifest.error.orEmpty(),
                )
                approvalCoordinator.showTextToast(
                    deviceName = peerName,
                    message = manifest.error.orEmpty(),
                    peerKey = peerKey,
                    peerHost = peerHost,
                    peerPort = peerPort,
                )
                dos.writeJsonPayload(WireMessage(command = "CHAT_ACK").toWireJson())
                return@withContext
            }
            if (manifest.command != "MANIFEST" || manifest.files.isNullOrEmpty()) {
                dos.writeJsonPayload(WireMessage(command = "MANIFEST_REJECT", error = "Empty").toWireJson())
                return@withContext
            }
            val peerName = hello.deviceName ?: "Peer"
            val approved = approvalCoordinator.awaitDecision(
                deviceName = peerName,
                files = manifest.files,
            )
            if (!approved) {
                dos.writeJsonPayload(
                    WireMessage(command = "MANIFEST_REJECT", error = "Receiver declined").toWireJson(),
                )
                return@withContext
            }
            dos.writeJsonPayload(WireMessage(command = "MANIFEST_OK").toWireJson())
            val total = manifest.files.sumOf { it.size }
            var done = 0L
            val speedEwma = SpeedMeter()
            val outDir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "ShareFast")
                .apply { mkdirs() }

            val batchSize = manifest.files.size
            var lastPublishedUri: String? = null
            var lastPublishedName: String? = null
            var completedFiles = 0

            for ((index, _) in manifest.files.withIndex()) {
                if (transferCancelled.get()) return@withContext
                waitWhilePaused()
                val nameLen = dis.readInt()
                if (nameLen <= 0 || nameLen > 1_000_000) error("Bad name")
                val nameBuf = ByteArray(nameLen)
                dis.readFully(nameBuf)
                val name = String(nameBuf, Charsets.UTF_8)
                val size = dis.readLong()
                if (size < 0) error("Bad size")
                var outFile = File(outDir, name)
                if (outFile.exists()) {
                    val base = name.substringBeforeLast('.', name)
                    val ext = name.substringAfterLast('.', "")
                    val suffix = System.currentTimeMillis().toString()
                    outFile = if (ext.isNotEmpty() && ext != name) {
                        File(outDir, "${base}_$suffix.$ext")
                    } else {
                        File(outDir, "${name}_$suffix")
                    }
                }
                FileOutputStream(outFile).use { fos ->
                    copyStreamIn(dis, fos, size) { chunk ->
                        done += chunk
                        val speed = speedEwma.update(chunk)
                        _progress.value = TransferProgress(
                            currentFileIndex = index + 1,
                            totalFiles = manifest.files.size,
                            currentFileName = name,
                            bytesTransferred = done,
                            totalBytes = total,
                            speedBytesPerSecond = speed,
                            isPaused = paused.get(),
                        )
                    }
                }
                val published = incomingTransferFiles.publishReceivedFile(outFile, name)
                lastPublishedUri = published
                lastPublishedName = name
                completedFiles++
                transferHistoryRepository.insert(
                    TransferHistoryEntry(
                        peerName = peerName,
                        fileName = name,
                        sizeBytes = size,
                        direction = TransferDirection.RECEIVED,
                        timestampEpochMs = System.currentTimeMillis(),
                        success = true,
                        storageUri = published,
                    ),
                )
            }
            _progress.value = null
            if (completedFiles == batchSize) {
                transferNotifications.notifyReceiveComplete(peerName, batchSize, lastPublishedUri, lastPublishedName)
                withContext(Dispatchers.Main) {
                    Feedback.vibrateSuccess(context)
                    Feedback.playSuccessTone()
                }
            }
        } catch (_: Exception) {
            _progress.value = null
        } finally {
            runCatching { socket.close() }
            if (activeSocket === socket) activeSocket = null
        }
    }

    private suspend fun copyStreamIn(
        dis: DataInputStream,
        fos: FileOutputStream,
        totalBytes: Long,
        onChunk: (Int) -> Unit,
    ) {
        var remaining = totalBytes
        val buf = ByteArray(ShareConstants.STREAM_BUFFER)
        while (remaining > 0) {
            if (transferCancelled.get()) return
            while (paused.get() && !transferCancelled.get()) {
                delay(50)
            }
            val toRead = minOf(buf.size.toLong(), remaining).toInt()
            dis.readFully(buf, 0, toRead)
            fos.write(buf, 0, toRead)
            remaining -= toRead
            onChunk(toRead)
        }
    }
}

private class ReceiverDeclinedException(message: String) : IllegalStateException(message)

private class SpeedMeter {
    private var last = System.nanoTime()
    private var ema: Double = 0.0

    fun update(bytes: Int): Double {
        val now = System.nanoTime()
        val dt = (now - last).coerceAtLeast(1L) / 1_000_000_000.0
        last = now
        val inst = bytes / dt
        ema = if (ema == 0.0) inst else ema * 0.75 + inst * 0.25
        return ema
    }
}

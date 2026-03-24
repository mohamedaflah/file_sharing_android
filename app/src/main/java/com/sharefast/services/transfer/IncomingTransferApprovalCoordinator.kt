package com.sharefast.services.transfer

import com.sharefast.core.network.FileDescriptorWire
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

data class IncomingTransferRequest(
    val id: Long,
    val deviceName: String,
    val files: List<FileDescriptorWire>,
)

@Singleton
class IncomingTransferApprovalCoordinator @Inject constructor() {
    private val idGen = AtomicLong(1)
    private val _request = MutableStateFlow<IncomingTransferRequest?>(null)
    val request: StateFlow<IncomingTransferRequest?> = _request.asStateFlow()
    private var pendingDecision: CompletableDeferred<Boolean>? = null

    suspend fun awaitDecision(
        deviceName: String,
        files: List<FileDescriptorWire>,
        timeoutMs: Long = 45_000L,
    ): Boolean {
        // If another request is in progress, reject this new one.
        if (_request.value != null) return false
        val id = idGen.getAndIncrement()
        val deferred = CompletableDeferred<Boolean>()
        pendingDecision = deferred
        _request.value = IncomingTransferRequest(
            id = id,
            deviceName = deviceName,
            files = files,
        )
        val approved = withTimeoutOrNull(timeoutMs) { deferred.await() } ?: false
        clear()
        return approved
    }

    fun accept(requestId: Long) {
        if (_request.value?.id != requestId) return
        pendingDecision?.complete(true)
    }

    fun decline(requestId: Long) {
        if (_request.value?.id != requestId) return
        pendingDecision?.complete(false)
    }

    fun clear() {
        pendingDecision = null
        _request.value = null
    }
}


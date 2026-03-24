package com.sharefast.domain.repository

import com.sharefast.domain.model.PeerDevice
import kotlinx.coroutines.flow.Flow

interface DiscoveryRepository {
    val peers: Flow<List<PeerDevice>>
    suspend fun startDiscovery(advertisePort: Int)
    suspend fun stopDiscovery()
}

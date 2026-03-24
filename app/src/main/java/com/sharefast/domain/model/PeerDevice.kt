package com.sharefast.domain.model

enum class DiscoverySource { UDP, NSD }

data class PeerDevice(
    val id: String,
    val displayName: String,
    val hostAddress: String,
    val port: Int,
    val source: DiscoverySource,
    val lastSeenEpochMs: Long = System.currentTimeMillis(),
)

package com.sharefast.domain.repository

import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    val deviceDisplayName: Flow<String>
    suspend fun setDeviceDisplayName(name: String)
    suspend fun localDeviceId(): String
}

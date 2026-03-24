package com.sharefast.domain.repository

import com.sharefast.domain.model.TransferHistoryEntry
import kotlinx.coroutines.flow.Flow

interface TransferHistoryRepository {
    fun history(): Flow<List<TransferHistoryEntry>>
    suspend fun insert(entry: TransferHistoryEntry)
}

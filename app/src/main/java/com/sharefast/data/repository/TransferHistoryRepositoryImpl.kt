package com.sharefast.data.repository

import com.sharefast.data.local.TransferRecordDao
import com.sharefast.data.mapper.toDomain
import com.sharefast.data.mapper.toEntity
import com.sharefast.domain.model.TransferHistoryEntry
import com.sharefast.domain.repository.TransferHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferHistoryRepositoryImpl @Inject constructor(
    private val dao: TransferRecordDao,
) : TransferHistoryRepository {
    override fun history(): Flow<List<TransferHistoryEntry>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun insert(entry: TransferHistoryEntry) {
        dao.insert(entry.toEntity())
    }
}

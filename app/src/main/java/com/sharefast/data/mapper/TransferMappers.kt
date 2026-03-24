package com.sharefast.data.mapper

import com.sharefast.data.local.entity.TransferRecordEntity
import com.sharefast.domain.model.TransferHistoryEntry

fun TransferRecordEntity.toDomain() = TransferHistoryEntry(
    id = id,
    peerName = peerName,
    fileName = fileName,
    sizeBytes = sizeBytes,
    direction = direction,
    timestampEpochMs = timestampEpochMs,
    success = success,
    storageUri = storageUri,
)

fun TransferHistoryEntry.toEntity() = TransferRecordEntity(
    id = id,
    peerName = peerName,
    fileName = fileName,
    sizeBytes = sizeBytes,
    direction = direction,
    timestampEpochMs = timestampEpochMs,
    success = success,
    storageUri = storageUri,
)

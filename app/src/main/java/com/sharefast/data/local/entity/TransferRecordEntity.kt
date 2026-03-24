package com.sharefast.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sharefast.domain.model.TransferDirection

@Entity(tableName = "transfer_records")
data class TransferRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val peerName: String,
    val fileName: String,
    val sizeBytes: Long,
    val direction: TransferDirection,
    val timestampEpochMs: Long,
    val success: Boolean,
    val storageUri: String? = null,
)

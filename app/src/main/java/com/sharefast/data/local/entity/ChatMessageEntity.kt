package com.sharefast.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    indices = [Index("peerKey"), Index("timestampEpochMs")],
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val peerKey: String,
    val peerName: String,
    val body: String,
    val isOutgoing: Boolean,
    val timestampEpochMs: Long,
)


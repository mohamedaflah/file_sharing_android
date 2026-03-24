package com.sharefast.data.repository

import com.sharefast.data.local.ChatMessageDao
import com.sharefast.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class ChatMessage(
    val id: Long,
    val peerKey: String,
    val peerName: String,
    val body: String,
    val isOutgoing: Boolean,
    val timestampEpochMs: Long,
)

data class ChatThread(
    val peerKey: String,
    val peerName: String,
    val lastBody: String,
    val timestampEpochMs: Long,
)

@Singleton
class ChatRepository @Inject constructor(
    private val dao: ChatMessageDao,
) {
    fun thread(peerKey: String): Flow<List<ChatMessage>> =
        dao.observePeer(peerKey).map { list ->
            list.map { e ->
                ChatMessage(
                    id = e.id,
                    peerKey = e.peerKey,
                    peerName = e.peerName,
                    body = e.body,
                    isOutgoing = e.isOutgoing,
                    timestampEpochMs = e.timestampEpochMs,
                )
            }
        }

    fun threads(): Flow<List<ChatThread>> =
        dao.observeLatestByPeer().map { list ->
            list.map { e ->
                ChatThread(
                    peerKey = e.peerKey,
                    peerName = e.peerName,
                    lastBody = e.body,
                    timestampEpochMs = e.timestampEpochMs,
                )
            }
        }

    suspend fun insertIncoming(peerKey: String, peerName: String, body: String) {
        dao.insert(
            ChatMessageEntity(
                peerKey = peerKey,
                peerName = peerName,
                body = body.trim(),
                isOutgoing = false,
                timestampEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun insertOutgoing(peerKey: String, peerName: String, body: String) {
        dao.insert(
            ChatMessageEntity(
                peerKey = peerKey,
                peerName = peerName,
                body = body.trim(),
                isOutgoing = true,
                timestampEpochMs = System.currentTimeMillis(),
            ),
        )
    }
}


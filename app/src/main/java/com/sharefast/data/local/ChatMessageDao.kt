package com.sharefast.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sharefast.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE peerKey = :peerKey ORDER BY timestampEpochMs ASC")
    fun observePeer(peerKey: String): Flow<List<ChatMessageEntity>>

    @Query(
        "SELECT * FROM chat_messages WHERE id IN (" +
            "SELECT MAX(id) FROM chat_messages GROUP BY peerKey" +
            ") ORDER BY timestampEpochMs DESC",
    )
    fun observeLatestByPeer(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ChatMessageEntity): Long
}


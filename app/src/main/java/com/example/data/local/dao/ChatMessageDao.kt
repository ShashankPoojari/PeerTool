package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.model.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE peerId = :peerId AND roomId IS NULL ORDER BY timestamp ASC")
    fun getMessagesForPeerFlow(peerId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE roomId = :roomId ORDER BY timestamp ASC")
    fun getMessagesForRoomFlow(roomId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE roomId IS NULL GROUP BY peerId ORDER BY timestamp DESC")
    fun getRecentChatsFlow(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)

    @Query("UPDATE chat_messages SET reactionEmoji = :emoji WHERE id = :messageId")
    suspend fun updateReaction(messageId: String, emoji: String)

    @Query("DELETE FROM chat_messages WHERE peerId = :peerId AND roomId IS NULL")
    suspend fun clearHistoryForPeer(peerId: String)

    @Query("DELETE FROM chat_messages WHERE roomId = :roomId")
    suspend fun clearHistoryForRoom(roomId: String)
}

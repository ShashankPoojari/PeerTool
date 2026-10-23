package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val peerId: String,
    val isIncoming: Boolean,
    val text: String,
    val type: MessageType = MessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT,
    val replyToId: String? = null,
    val reactionEmoji: String? = null,
    val fileTransferId: String? = null,
    val voiceDurationMs: Long = 0L,
    val roomId: String? = null,
    val isEncrypted: Boolean = true
)

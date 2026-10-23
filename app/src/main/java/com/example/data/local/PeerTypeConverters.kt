package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.example.data.model.TransferStatus

class PeerTypeConverters {
    @TypeConverter
    fun fromMessageStatus(status: MessageStatus): String = status.name

    @TypeConverter
    fun toMessageStatus(value: String): MessageStatus = runCatching {
        MessageStatus.valueOf(value)
    }.getOrDefault(MessageStatus.SENT)

    @TypeConverter
    fun fromMessageType(type: MessageType): String = type.name

    @TypeConverter
    fun toMessageType(value: String): MessageType = runCatching {
        MessageType.valueOf(value)
    }.getOrDefault(MessageType.TEXT)

    @TypeConverter
    fun fromTransferStatus(status: TransferStatus): String = status.name

    @TypeConverter
    fun toTransferStatus(value: String): TransferStatus = runCatching {
        TransferStatus.valueOf(value)
    }.getOrDefault(TransferStatus.PENDING)
}

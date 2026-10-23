package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.TransferStatus

@Entity(tableName = "file_transfers")
data class FileTransferEntity(
    @PrimaryKey
    val id: String,
    val peerId: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val isIncoming: Boolean,
    val totalChunks: Int,
    val status: TransferStatus = TransferStatus.PENDING,
    val localFilePath: String? = null,
    val sha256: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

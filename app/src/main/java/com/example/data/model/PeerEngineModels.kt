package com.example.data.model

data class ServerConfig(
    val host: String = "0.peerjs.com",
    val port: Int = 443,
    val path: String = "/",
    val secure: Boolean = true,
    val key: String = "peerjs"
)

data class IceServerConfig(
    val url: String,
    val username: String? = null,
    val credential: String? = null
)

data class PeerConnectionInfo(
    val peerId: String,
    val isConnected: Boolean = true,
    val latencyMs: Long = 0L,
    val iceState: String = "connected",
    val remoteMetadata: Map<String, String> = emptyMap(),
    val isEncrypted: Boolean = true,
    val safetyNumber: String? = null,
    val fingerprintHex: String? = null,
    val isVerified: Boolean = false
)

data class ActiveCallState(
    val peerId: String,
    val isVideo: Boolean,
    val isIncoming: Boolean,
    val isConnected: Boolean,
    val isMuted: Boolean = false,
    val isVideoEnabled: Boolean = true,
    val durationSeconds: Long = 0L
)

enum class TransferStatus {
    PENDING,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED,
    DECLINED
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

enum class MessageType {
    TEXT,
    FILE_REF,
    VOICE,
    SYSTEM
}

data class FileTransferProgress(
    val transferId: String,
    val peerId: String,
    val fileName: String,
    val fileSize: Long,
    val bytesTransferred: Long = 0L,
    val totalChunks: Int = 0,
    val currentChunk: Int = 0,
    val speedBps: Long = 0L,
    val isIncoming: Boolean = false,
    val status: TransferStatus = TransferStatus.PENDING,
    val localFilePath: String? = null
)

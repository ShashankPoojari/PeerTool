package com.example.data.engine

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.crypto.CryptoManager
import com.example.data.local.PeerDatabase
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.ContactEntity
import com.example.data.local.entities.FileTransferEntity
import com.example.data.model.ActiveCallState
import com.example.data.model.FileTransferProgress
import com.example.data.model.IceServerConfig
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.example.data.model.PeerConnectionInfo
import com.example.data.model.ServerConfig
import com.example.data.model.TransferStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PeerEngineManager(private val context: Context) {

    private val db = PeerDatabase.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val mainHandler = Handler(Looper.getMainLooper())

    private var webView: WebView? = null
    private var isEngineReady = false

    // State Flows
    private val _peerId = MutableStateFlow<String?>(null)
    val peerId: StateFlow<String?> = _peerId.asStateFlow()

    private val _isServerConnected = MutableStateFlow(false)
    val isServerConnected: StateFlow<Boolean> = _isServerConnected.asStateFlow()

    private val _connectedPeers = MutableStateFlow<Map<String, PeerConnectionInfo>>(emptyMap())
    val connectedPeers: StateFlow<Map<String, PeerConnectionInfo>> = _connectedPeers.asStateFlow()

    private val _activeCall = MutableStateFlow<ActiveCallState?>(null)
    val activeCall: StateFlow<ActiveCallState?> = _activeCall.asStateFlow()

    private val _typingPeers = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val typingPeers: StateFlow<Map<String, Boolean>> = _typingPeers.asStateFlow()

    private val _activeTransfers = MutableStateFlow<Map<String, FileTransferProgress>>(emptyMap())
    val activeTransfers: StateFlow<Map<String, FileTransferProgress>> = _activeTransfers.asStateFlow()

    private val _engineErrors = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val engineErrors: SharedFlow<String> = _engineErrors.asSharedFlow()

    // File transfer staging
    private val incomingFileChunks = ConcurrentHashMap<String, MutableMap<Int, ByteArray>>()
    private val pendingOutgoingFiles = ConcurrentHashMap<String, Uri>()
    private var callTimerJob: Job? = null

    // Configuration & Persistence
    private val prefs = context.getSharedPreferences("peerflow_prefs", Context.MODE_PRIVATE)
    val cryptoManager = CryptoManager(context)

    val savedCustomPeerId: String?
        get() = prefs.getString("saved_custom_peer_id", null)?.takeIf { it.isNotBlank() }

    var currentServerConfig = ServerConfig()
        private set
    var currentIceServers: List<IceServerConfig> = listOf(
        IceServerConfig("stun:stun.l.google.com:19302"),
        IceServerConfig("stun:global.stun.twilio.com:3478")
    )
        private set

    init {
        mainHandler.post {
            setupWebView()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val wv = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.allowFileAccess = true

            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest?) {
                    request?.grant(request.resources)
                }

                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    Log.d("PeerJS_Web", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()}")
                    return true
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    Log.i("PeerFlow", "PeerJS WebRTC Engine loaded successfully")
                    isEngineReady = true
                    initializeEngine(savedCustomPeerId, currentServerConfig, currentIceServers)
                }
            }

            addJavascriptInterface(EngineBridge(), "AndroidBridge")
        }

        webView = wv
        wv.loadUrl("file:///android_asset/peerjs/peer_engine.html")
    }

    fun getWebView(): WebView? = webView

    fun setCustomPeerId(newId: String?) {
        val cleanId = newId?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        if (cleanId != null) {
            prefs.edit().putString("saved_custom_peer_id", cleanId).apply()
        } else {
            prefs.edit().remove("saved_custom_peer_id").apply()
        }
        initializeEngine(cleanId, currentServerConfig, currentIceServers)
    }

    fun initializeEngine(
        customPeerId: String?,
        serverConfig: ServerConfig = currentServerConfig,
        iceServers: List<IceServerConfig> = currentIceServers
    ) {
        currentServerConfig = serverConfig
        currentIceServers = iceServers

        val serverJson = JSONObject().apply {
            put("host", serverConfig.host)
            put("port", serverConfig.port)
            put("path", serverConfig.path)
            put("secure", serverConfig.secure)
            put("key", serverConfig.key)
        }.toString()

        val iceArray = JSONArray().apply {
            iceServers.forEach { srv ->
                val obj = JSONObject().put("urls", srv.url)
                if (!srv.username.isNullOrEmpty()) obj.put("username", srv.username)
                if (!srv.credential.isNullOrEmpty()) obj.put("credential", srv.credential)
                put(obj)
            }
        }.toString()

        val safeId = if (customPeerId.isNullOrBlank()) "null" else "'${customPeerId.trim()}'"
        val js = "window.peerEngine.initPeer($safeId, '$serverJson', '$iceArray');"
        evaluateJs(js)
    }

    fun connectToPeer(targetId: String, nickname: String = "Peer") {
        val meta = JSONObject().apply {
            put("nickname", nickname)
            put("app", "PeerFlow")
            put("time", System.currentTimeMillis())
        }.toString()
        evaluateJs("window.peerEngine.connectToPeer('$targetId', '$meta');")
    }

    fun disconnectPeer(targetId: String) {
        evaluateJs("window.peerEngine.disconnectFromPeer('$targetId');")
        val current = _connectedPeers.value.toMutableMap()
        current.remove(targetId)
        _connectedPeers.value = current
    }

    fun sendChatMessage(targetId: String, text: String, replyToId: String? = null): String {
        val messageId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        scope.launch {
            val entity = ChatMessageEntity(
                id = messageId,
                peerId = targetId,
                isIncoming = false,
                text = text,
                type = MessageType.TEXT,
                timestamp = now,
                status = MessageStatus.SENT,
                replyToId = replyToId,
                isEncrypted = true
            )
            db.chatMessageDao().insertMessage(entity)
            db.contactDao().updateLastConnectedTime(targetId, now)
        }

        // Encrypt with AES-256-GCM if cryptographic session is established
        if (cryptoManager.hasSession(targetId)) {
            try {
                val (iv, ciphertext) = cryptoManager.encryptString(targetId, text.trim())
                val encJson = JSONObject().apply {
                    put("id", messageId)
                    put("iv", iv)
                    put("ciphertext", ciphertext)
                    put("timestamp", now)
                    if (replyToId != null) put("replyToId", replyToId)
                }.toString()

                evaluateJs("window.peerEngine.sendEncryptedMessage('$targetId', $encJson);")
                return messageId
            } catch (e: Exception) {
                Log.e("PeerEngine", "Encryption failed, falling back to plaintext", e)
            }
        }

        val msgJson = JSONObject().apply {
            put("id", messageId)
            put("text", text)
            put("timestamp", now)
            if (replyToId != null) put("replyToId", replyToId)
        }.toString()

        evaluateJs("window.peerEngine.sendChatMessage('$targetId', $msgJson);")
        return messageId
    }

    fun sendTyping(targetId: String, isTyping: Boolean) {
        evaluateJs("window.peerEngine.sendTyping('$targetId', $isTyping);")
    }

    fun sendReaction(targetId: String, messageId: String, emoji: String) {
        scope.launch {
            db.chatMessageDao().updateReaction(messageId, emoji)
        }
        evaluateJs("window.peerEngine.sendReaction('$targetId', '$messageId', '$emoji');")
    }

    fun sendReadReceipt(targetId: String, messageId: String) {
        scope.launch {
            db.chatMessageDao().updateMessageStatus(messageId, MessageStatus.READ)
        }
        evaluateJs("window.peerEngine.sendReadReceipt('$targetId', '$messageId');")
    }

    fun sendVoiceNote(targetId: String, audioBytes: ByteArray, durationMs: Long): String {
        val noteId = UUID.randomUUID().toString()
        val base64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
        val now = System.currentTimeMillis()

        scope.launch {
            // Save audio file locally
            val voiceDir = File(context.filesDir, "peer_voice")
            if (!voiceDir.exists()) voiceDir.mkdirs()
            val audioFile = File(voiceDir, "$noteId.mp4")
            audioFile.writeBytes(audioBytes)

            val entity = ChatMessageEntity(
                id = noteId,
                peerId = targetId,
                isIncoming = false,
                text = "Voice message (${durationMs / 1000}s)",
                type = MessageType.VOICE,
                timestamp = now,
                status = MessageStatus.SENT,
                voiceDurationMs = durationMs,
                fileTransferId = audioFile.absolutePath
            )
            db.chatMessageDao().insertMessage(entity)
        }

        evaluateJs("window.peerEngine.sendVoiceNote('$targetId', '$noteId', '$base64', $durationMs);")
        return noteId
    }

    // High Speed File Transfer
    fun offerFile(targetId: String, uri: Uri, fileName: String, fileSize: Long, mimeType: String): String {
        val transferId = UUID.randomUUID().toString()
        val chunkSize = 32768 // 32KB chunk
        val totalChunks = ((fileSize + chunkSize - 1) / chunkSize).toInt().coerceAtLeast(1)

        pendingOutgoingFiles[transferId] = uri

        val progress = FileTransferProgress(
            transferId = transferId,
            peerId = targetId,
            fileName = fileName,
            fileSize = fileSize,
            totalChunks = totalChunks,
            isIncoming = false,
            status = TransferStatus.PENDING
        )
        val current = _activeTransfers.value.toMutableMap()
        current[transferId] = progress
        _activeTransfers.value = current

        scope.launch {
            val entity = FileTransferEntity(
                id = transferId,
                peerId = targetId,
                fileName = fileName,
                fileSize = fileSize,
                mimeType = mimeType,
                isIncoming = false,
                totalChunks = totalChunks,
                status = TransferStatus.PENDING
            )
            db.fileTransferDao().insertTransfer(entity)

            val chatMsg = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                peerId = targetId,
                isIncoming = false,
                text = fileName,
                type = MessageType.FILE_REF,
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.SENT,
                fileTransferId = transferId
            )
            db.chatMessageDao().insertMessage(chatMsg)
        }

        val meta = JSONObject().apply {
            put("transferId", transferId)
            put("fileName", fileName)
            put("fileSize", fileSize)
            put("mimeType", mimeType)
            put("totalChunks", totalChunks)
        }.toString()

        evaluateJs("window.peerEngine.startFileOffer('$targetId', '$meta');")
        return transferId
    }

    fun acceptIncomingFile(transferId: String) {
        val transfer = _activeTransfers.value[transferId] ?: return
        val current = _activeTransfers.value.toMutableMap()
        current[transferId] = transfer.copy(status = TransferStatus.IN_PROGRESS)
        _activeTransfers.value = current

        scope.launch {
            db.fileTransferDao().updateStatus(transferId, TransferStatus.IN_PROGRESS)
        }

        evaluateJs("window.peerEngine.acceptFileTransfer('${transfer.peerId}', '$transferId');")
    }

    fun declineIncomingFile(transferId: String, reason: String = "Declined") {
        val transfer = _activeTransfers.value[transferId] ?: return
        val current = _activeTransfers.value.toMutableMap()
        current[transferId] = transfer.copy(status = TransferStatus.DECLINED)
        _activeTransfers.value = current

        scope.launch {
            db.fileTransferDao().updateStatus(transferId, TransferStatus.DECLINED)
        }

        evaluateJs("window.peerEngine.declineFileTransfer('${transfer.peerId}', '$transferId', '$reason');")
    }

    private fun streamOutgoingFileChunks(targetId: String, transferId: String) {
        val uri = pendingOutgoingFiles[transferId] ?: return
        val transfer = _activeTransfers.value[transferId] ?: return

        scope.launch {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    updateTransferFailed(transferId, "Cannot read file")
                    return@launch
                }

                val buffer = ByteArray(32768)
                var chunkIndex = 0
                var totalBytesSent = 0L
                val startTime = System.currentTimeMillis()

                inputStream.use { stream ->
                    var bytesRead: Int
                    while (stream.read(buffer).also { bytesRead = it } != -1 && isActive) {
                        val chunkData = if (bytesRead == buffer.size) buffer else buffer.copyOf(bytesRead)
                        val base64 = Base64.encodeToString(chunkData, Base64.NO_WRAP)

                        evaluateJs("window.peerEngine.sendFileChunk('$targetId', '$transferId', $chunkIndex, ${transfer.totalChunks}, '$base64');")

                        totalBytesSent += bytesRead
                        chunkIndex++

                        val elapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                        val speed = (totalBytesSent * 1000) / elapsed

                        val updated = _activeTransfers.value.toMutableMap()
                        updated[transferId] = transfer.copy(
                            bytesTransferred = totalBytesSent,
                            currentChunk = chunkIndex,
                            speedBps = speed,
                            status = TransferStatus.IN_PROGRESS
                        )
                        _activeTransfers.value = updated

                        // Small throttle to avoid choking SCTP data channel buffer
                        delay(12)
                    }
                }

                evaluateJs("window.peerEngine.completeFileTransfer('$targetId', '$transferId');")

                val completed = _activeTransfers.value.toMutableMap()
                completed[transferId] = transfer.copy(
                    bytesTransferred = transfer.fileSize,
                    currentChunk = transfer.totalChunks,
                    status = TransferStatus.COMPLETED
                )
                _activeTransfers.value = completed

                db.fileTransferDao().updateStatus(transferId, TransferStatus.COMPLETED)
                pendingOutgoingFiles.remove(transferId)

            } catch (e: Exception) {
                Log.e("PeerFlow", "Error streaming file chunks", e)
                updateTransferFailed(transferId, e.message ?: "Stream error")
            }
        }
    }

    private fun updateTransferFailed(transferId: String, error: String) {
        val transfer = _activeTransfers.value[transferId] ?: return
        val updated = _activeTransfers.value.toMutableMap()
        updated[transferId] = transfer.copy(status = TransferStatus.FAILED)
        _activeTransfers.value = updated
        scope.launch {
            db.fileTransferDao().updateStatus(transferId, TransferStatus.FAILED)
        }
    }

    // Audio & Video Calls
    fun startAudioCall(targetId: String) {
        _activeCall.value = ActiveCallState(
            peerId = targetId,
            isVideo = false,
            isIncoming = false,
            isConnected = false
        )
        evaluateJs("window.peerEngine.startCall('$targetId', false);")
    }

    fun startVideoCall(targetId: String) {
        _activeCall.value = ActiveCallState(
            peerId = targetId,
            isVideo = true,
            isIncoming = false,
            isConnected = false
        )
        evaluateJs("window.peerEngine.startCall('$targetId', true);")
    }

    fun answerCall(isVideo: Boolean) {
        val call = _activeCall.value ?: return
        _activeCall.value = call.copy(isConnected = true, isVideo = isVideo)
        evaluateJs("window.peerEngine.answerCall($isVideo);")
        startCallTimer()
    }

    fun endCall() {
        evaluateJs("window.peerEngine.endCall();")
        callTimerJob?.cancel()
        _activeCall.value = null
    }

    fun toggleAudio(enabled: Boolean) {
        _activeCall.value = _activeCall.value?.copy(isMuted = !enabled)
        evaluateJs("window.peerEngine.toggleAudio($enabled);")
    }

    fun toggleVideo(enabled: Boolean) {
        _activeCall.value = _activeCall.value?.copy(isVideoEnabled = enabled)
        evaluateJs("window.peerEngine.toggleVideo($enabled);")
    }

    fun switchCamera() {
        evaluateJs("window.peerEngine.switchCamera();")
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = scope.launch {
            var seconds = 0L
            while (isActive) {
                delay(1000)
                seconds++
                val call = _activeCall.value ?: break
                _activeCall.value = call.copy(durationSeconds = seconds)
            }
        }
    }

    // Group Mesh Room
    fun broadcastRoomMessage(roomId: String, text: String, peerIds: List<String>) {
        val messageId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        scope.launch {
            val entity = ChatMessageEntity(
                id = messageId,
                peerId = roomId,
                isIncoming = false,
                text = text,
                type = MessageType.TEXT,
                timestamp = now,
                status = MessageStatus.SENT,
                roomId = roomId
            )
            db.chatMessageDao().insertMessage(entity)
        }

        val peersJson = JSONArray(peerIds).toString()
        val msgObj = JSONObject().apply {
            put("id", messageId)
            put("text", text)
            put("timestamp", now)
            put("roomId", roomId)
        }.toString()

        evaluateJs("window.peerEngine.broadcastRoomMessage('$peersJson', $msgObj);")
    }

    fun reconnect() {
        evaluateJs("window.peerEngine.reconnect();")
    }

    fun destroy() {
        callTimerJob?.cancel()
        evaluateJs("window.peerEngine.destroyPeer();")
    }

    private fun evaluateJs(js: String) {
        mainHandler.post {
            try {
                webView?.evaluateJavascript(js, null)
            } catch (e: Exception) {
                Log.e("PeerFlow", "evaluateJavascript error: $js", e)
            }
        }
    }

    // Internal Bridge for Javascript to Android callbacks
    inner class EngineBridge {
        @JavascriptInterface
        fun onEngineEvent(type: String, payloadJson: String) {
            try {
                val payload = JSONObject(payloadJson)
                Log.d("PeerFlow", "EngineEvent: $type -> $payloadJson")

                when (type) {
                    "PEER_OPEN" -> {
                        val id = payload.optString("peerId")
                        _peerId.value = id
                        _isServerConnected.value = true
                    }

                    "PEER_DISCONNECTED" -> {
                        _isServerConnected.value = false
                    }

                    "PEER_CLOSE" -> {
                        _isServerConnected.value = false
                        _peerId.value = null
                    }

                    "PEER_ERROR" -> {
                        val errorType = payload.optString("type")
                        val errorMsg = payload.optString("message", "Peer error")
                        val userFacing = if (errorType == "unavailable-id") {
                            "Peer ID is already claimed by another active peer on this server. Please choose a different ID."
                        } else {
                            errorMsg
                        }
                        scope.launch { _engineErrors.emit(userFacing) }
                    }

                    "CONN_OPEN" -> {
                        val peer = payload.optString("peerId")
                        val metaObj = payload.optJSONObject("metadata")
                        val metaMap = mutableMapOf<String, String>()
                        metaObj?.keys()?.forEach { k -> metaMap[k] = metaObj.optString(k) }

                        val nickname = metaMap["nickname"] ?: peer.take(8)
                        val info = PeerConnectionInfo(
                            peerId = peer,
                            isConnected = true,
                            remoteMetadata = metaMap,
                            isEncrypted = cryptoManager.hasSession(peer),
                            safetyNumber = cryptoManager.getSafetyNumber(peer),
                            fingerprintHex = cryptoManager.getRemoteFingerprintHex(peer),
                            isVerified = cryptoManager.isPeerVerified(peer)
                        )
                        val map = _connectedPeers.value.toMutableMap()
                        map[peer] = info
                        _connectedPeers.value = map

                        // Automatically initiate cryptographic ECDH handshake over WebRTC DataChannel
                        val localPub = cryptoManager.getLocalPublicKeyBase64()
                        val localFp = cryptoManager.getLocalFingerprintHex()
                        evaluateJs("window.peerEngine.sendCryptoHandshake('$peer', 'CRYPTO_HANDSHAKE_INIT', '$localPub', '$localFp');")

                        scope.launch {
                            val existing = db.contactDao().getContact(peer)
                            if (existing == null) {
                                db.contactDao().insertOrUpdate(
                                    ContactEntity(
                                        peerId = peer,
                                        nickname = nickname,
                                        lastConnectedTime = System.currentTimeMillis()
                                    )
                                )
                            } else {
                                db.contactDao().updateLastConnectedTime(peer)
                            }
                        }
                    }

                    "CONN_CLOSE" -> {
                        val peer = payload.optString("peerId")
                        val map = _connectedPeers.value.toMutableMap()
                        map.remove(peer)
                        _connectedPeers.value = map
                    }

                    "CONN_ERROR" -> {
                        val peer = payload.optString("peerId")
                        val err = payload.optString("error")
                        scope.launch { _engineErrors.emit("Connection error ($peer): $err") }
                    }

                    "CHAT_MESSAGE" -> {
                        val senderId = payload.optString("senderId")
                        val id = payload.optString("id")
                        val text = payload.optString("text")
                        val time = payload.optLong("timestamp", System.currentTimeMillis())
                        val replyTo = payload.optString("replyToId").takeIf { it.isNotEmpty() && it != "null" }
                        val isBroadcast = payload.optBoolean("isRoomBroadcast", false)
                        val roomId = payload.optString("roomId").takeIf { it.isNotEmpty() && it != "null" }

                        scope.launch {
                            val entity = ChatMessageEntity(
                                id = id,
                                peerId = if (isBroadcast && roomId != null) roomId else senderId,
                                isIncoming = true,
                                text = text,
                                type = MessageType.TEXT,
                                timestamp = time,
                                status = MessageStatus.DELIVERED,
                                replyToId = replyTo,
                                roomId = roomId
                            )
                            db.chatMessageDao().insertMessage(entity)
                        }
                    }

                    "CRYPTO_HANDSHAKE_INIT" -> {
                        val senderId = payload.optString("senderId")
                        val remotePubKey = payload.optString("publicKey")
                        val remoteFp = payload.optString("fingerprint")
                        try {
                            val safetyNum = cryptoManager.establishSession(senderId, remotePubKey)
                            val myPub = cryptoManager.getLocalPublicKeyBase64()
                            val myFp = cryptoManager.getLocalFingerprintHex()
                            evaluateJs("window.peerEngine.sendCryptoHandshake('$senderId', 'CRYPTO_HANDSHAKE_ACK', '$myPub', '$myFp');")

                            val cur = _connectedPeers.value.toMutableMap()
                            cur[senderId]?.let { old ->
                                cur[senderId] = old.copy(
                                    isEncrypted = true,
                                    safetyNumber = safetyNum,
                                    fingerprintHex = remoteFp,
                                    isVerified = cryptoManager.isPeerVerified(senderId)
                                )
                                _connectedPeers.value = cur
                            }
                        } catch (e: Exception) {
                            Log.e("PeerEngine", "CRYPTO_HANDSHAKE_INIT failed for $senderId", e)
                        }
                    }

                    "CRYPTO_HANDSHAKE_ACK" -> {
                        val senderId = payload.optString("senderId")
                        val remotePubKey = payload.optString("publicKey")
                        val remoteFp = payload.optString("fingerprint")
                        try {
                            val safetyNum = cryptoManager.establishSession(senderId, remotePubKey)
                            val cur = _connectedPeers.value.toMutableMap()
                            cur[senderId]?.let { old ->
                                cur[senderId] = old.copy(
                                    isEncrypted = true,
                                    safetyNumber = safetyNum,
                                    fingerprintHex = remoteFp,
                                    isVerified = cryptoManager.isPeerVerified(senderId)
                                )
                                _connectedPeers.value = cur
                            }
                        } catch (e: Exception) {
                            Log.e("PeerEngine", "CRYPTO_HANDSHAKE_ACK failed for $senderId", e)
                        }
                    }

                    "ENCRYPTED_MESSAGE" -> {
                        val senderId = payload.optString("senderId")
                        val id = payload.optString("id")
                        val iv = payload.optString("iv")
                        val ciphertext = payload.optString("ciphertext")
                        val time = payload.optLong("timestamp", System.currentTimeMillis())
                        val replyTo = payload.optString("replyToId").takeIf { it.isNotEmpty() && it != "null" }
                        val isBroadcast = payload.optBoolean("isRoomBroadcast", false)
                        val roomId = payload.optString("roomId").takeIf { it.isNotEmpty() && it != "null" }

                        val decryptedText = try {
                            if (cryptoManager.hasSession(senderId)) {
                                cryptoManager.decryptString(senderId, iv, ciphertext)
                            } else {
                                "[Encrypted Message - Establishing Cryptographic Keys...]"
                            }
                        } catch (e: Exception) {
                            Log.e("PeerEngine", "Decryption failed for message $id", e)
                            "[Decryption Error - Tag Mismatch / Tampering Detected]"
                        }

                        scope.launch {
                            val entity = ChatMessageEntity(
                                id = id,
                                peerId = if (isBroadcast && roomId != null) roomId else senderId,
                                isIncoming = true,
                                text = decryptedText,
                                type = MessageType.TEXT,
                                timestamp = time,
                                status = MessageStatus.DELIVERED,
                                replyToId = replyTo,
                                roomId = roomId,
                                isEncrypted = true
                            )
                            db.chatMessageDao().insertMessage(entity)
                        }
                    }

                    "TYPING_STATUS" -> {
                        val senderId = payload.optString("senderId")
                        val isTyping = payload.optBoolean("isTyping", false)
                        val current = _typingPeers.value.toMutableMap()
                        current[senderId] = isTyping
                        _typingPeers.value = current
                    }

                    "READ_RECEIPT" -> {
                        val msgId = payload.optString("messageId")
                        scope.launch {
                            db.chatMessageDao().updateMessageStatus(msgId, MessageStatus.READ)
                        }
                    }

                    "DELIVERY_ACK" -> {
                        val msgId = payload.optString("messageId")
                        scope.launch {
                            db.chatMessageDao().updateMessageStatus(msgId, MessageStatus.DELIVERED)
                        }
                    }

                    "MESSAGE_REACTION" -> {
                        val msgId = payload.optString("messageId")
                        val emoji = payload.optString("emoji")
                        scope.launch {
                            db.chatMessageDao().updateReaction(msgId, emoji)
                        }
                    }

                    "VOICE_NOTE" -> {
                        val senderId = payload.optString("senderId")
                        val id = payload.optString("id")
                        val base64 = payload.optString("base64Audio")
                        val duration = payload.optLong("durationMs", 0L)
                        val time = payload.optLong("timestamp", System.currentTimeMillis())

                        scope.launch {
                            try {
                                val audioBytes = Base64.decode(base64, Base64.DEFAULT)
                                val voiceDir = File(context.filesDir, "peer_voice")
                                if (!voiceDir.exists()) voiceDir.mkdirs()
                                val audioFile = File(voiceDir, "$id.mp4")
                                audioFile.writeBytes(audioBytes)

                                val entity = ChatMessageEntity(
                                    id = id,
                                    peerId = senderId,
                                    isIncoming = true,
                                    text = "Voice message (${duration / 1000}s)",
                                    type = MessageType.VOICE,
                                    timestamp = time,
                                    status = MessageStatus.DELIVERED,
                                    voiceDurationMs = duration,
                                    fileTransferId = audioFile.absolutePath
                                )
                                db.chatMessageDao().insertMessage(entity)
                            } catch (e: Exception) {
                                Log.e("PeerFlow", "Voice note decode error", e)
                            }
                        }
                    }

                    "FILE_OFFER" -> {
                        val senderId = payload.optString("senderId")
                        val transferId = payload.optString("transferId")
                        val fileName = payload.optString("fileName")
                        val fileSize = payload.optLong("fileSize")
                        val mimeType = payload.optString("mimeType")
                        val totalChunks = payload.optInt("totalChunks")
                        val sha256 = payload.optString("sha256")

                        incomingFileChunks[transferId] = ConcurrentHashMap()

                        val progress = FileTransferProgress(
                            transferId = transferId,
                            peerId = senderId,
                            fileName = fileName,
                            fileSize = fileSize,
                            totalChunks = totalChunks,
                            isIncoming = true,
                            status = TransferStatus.PENDING
                        )
                        val current = _activeTransfers.value.toMutableMap()
                        current[transferId] = progress
                        _activeTransfers.value = current

                        scope.launch {
                            val entity = FileTransferEntity(
                                id = transferId,
                                peerId = senderId,
                                fileName = fileName,
                                fileSize = fileSize,
                                mimeType = mimeType,
                                isIncoming = true,
                                totalChunks = totalChunks,
                                status = TransferStatus.PENDING,
                                sha256 = sha256
                            )
                            db.fileTransferDao().insertTransfer(entity)

                            val chatMsg = ChatMessageEntity(
                                id = UUID.randomUUID().toString(),
                                peerId = senderId,
                                isIncoming = true,
                                text = fileName,
                                type = MessageType.FILE_REF,
                                timestamp = System.currentTimeMillis(),
                                status = MessageStatus.DELIVERED,
                                fileTransferId = transferId
                            )
                            db.chatMessageDao().insertMessage(chatMsg)
                        }
                    }

                    "FILE_ACCEPT" -> {
                        val transferId = payload.optString("transferId")
                        val senderId = payload.optString("senderId")
                        streamOutgoingFileChunks(senderId, transferId)
                    }

                    "FILE_DECLINE" -> {
                        val transferId = payload.optString("transferId")
                        val reason = payload.optString("reason", "Declined")
                        val current = _activeTransfers.value.toMutableMap()
                        current[transferId]?.let {
                            current[transferId] = it.copy(status = TransferStatus.DECLINED)
                            _activeTransfers.value = current
                        }
                        scope.launch {
                            db.fileTransferDao().updateStatus(transferId, TransferStatus.DECLINED)
                            _engineErrors.emit("File transfer declined: $reason")
                        }
                    }

                    "FILE_CHUNK" -> {
                        val transferId = payload.optString("transferId")
                        val chunkIdx = payload.optInt("chunkIndex")
                        val total = payload.optInt("totalChunks")
                        val base64Data = payload.optString("base64Data")

                        val chunks = incomingFileChunks.getOrPut(transferId) { ConcurrentHashMap() }
                        val rawBytes = Base64.decode(base64Data, Base64.DEFAULT)
                        chunks[chunkIdx] = rawBytes

                        val currentTransfer = _activeTransfers.value[transferId]
                        if (currentTransfer != null) {
                            val bytesCount = chunks.values.sumOf { it.size.toLong() }
                            val updated = _activeTransfers.value.toMutableMap()
                            updated[transferId] = currentTransfer.copy(
                                bytesTransferred = bytesCount,
                                currentChunk = chunks.size,
                                status = TransferStatus.IN_PROGRESS
                            )
                            _activeTransfers.value = updated
                        }
                    }

                    "FILE_COMPLETE" -> {
                        val transferId = payload.optString("transferId")
                        val senderId = payload.optString("senderId")
                        val chunks = incomingFileChunks[transferId]
                        val transfer = _activeTransfers.value[transferId]

                        if (chunks != null && transfer != null) {
                            scope.launch {
                                try {
                                    val downloadsDir = File(context.filesDir, "peer_downloads")
                                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                                    val safeName = "${System.currentTimeMillis()}_${transfer.fileName}"
                                    val finalFile = File(downloadsDir, safeName)

                                    FileOutputStream(finalFile).use { fos ->
                                        for (i in 0 until transfer.totalChunks) {
                                            val part = chunks[i]
                                            if (part != null) {
                                                fos.write(part)
                                            }
                                        }
                                    }

                                    incomingFileChunks.remove(transferId)

                                    val updated = _activeTransfers.value.toMutableMap()
                                    updated[transferId] = transfer.copy(
                                        bytesTransferred = transfer.fileSize,
                                        currentChunk = transfer.totalChunks,
                                        status = TransferStatus.COMPLETED,
                                        localFilePath = finalFile.absolutePath
                                    )
                                    _activeTransfers.value = updated

                                    db.fileTransferDao().updateFilePathAndStatus(
                                        transferId,
                                        finalFile.absolutePath,
                                        TransferStatus.COMPLETED
                                    )

                                    // File transfer completed and path updated in transfer record

                                } catch (e: Exception) {
                                    Log.e("PeerFlow", "Error saving completed file", e)
                                }
                            }
                        }
                    }

                    "CALL_INCOMING" -> {
                        val callerId = payload.optString("callerId")
                        val isVideo = payload.optBoolean("isVideo", true)
                        _activeCall.value = ActiveCallState(
                            peerId = callerId,
                            isVideo = isVideo,
                            isIncoming = true,
                            isConnected = false
                        )
                    }

                    "CALL_CONNECTED" -> {
                        val remoteId = payload.optString("remoteId")
                        _activeCall.value = _activeCall.value?.copy(isConnected = true, peerId = remoteId)
                        startCallTimer()
                    }

                    "CALL_ENDED" -> {
                        callTimerJob?.cancel()
                        _activeCall.value = null
                    }

                    "CALL_ERROR" -> {
                        val error = payload.optString("error", "Call failure")
                        scope.launch { _engineErrors.emit("Call error: $error") }
                        callTimerJob?.cancel()
                        _activeCall.value = null
                    }

                    "PING_RESULT" -> {
                        val target = payload.optString("targetId")
                        val rtt = payload.optLong("rttMs", 0L)
                        val current = _connectedPeers.value.toMutableMap()
                        current[target]?.let {
                            current[target] = it.copy(latencyMs = rtt)
                            _connectedPeers.value = current
                        }
                    }

                    "ICE_STATE_CHANGE" -> {
                        val peer = payload.optString("peerId")
                        val state = payload.optString("state")
                        val current = _connectedPeers.value.toMutableMap()
                        current[peer]?.let {
                            current[peer] = it.copy(iceState = state)
                            _connectedPeers.value = current
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("PeerFlow", "Error parsing engine event: $payloadJson", e)
            }
        }
    }

    fun verifyPeerSecurity(peerId: String, isVerified: Boolean) {
        cryptoManager.setPeerVerified(peerId, isVerified)
        val cur = _connectedPeers.value.toMutableMap()
        cur[peerId]?.let { old ->
            cur[peerId] = old.copy(isVerified = isVerified)
            _connectedPeers.value = cur
        }
        scope.launch {
            db.contactDao().getContact(peerId)?.let { contact ->
                db.contactDao().insertOrUpdate(
                    contact.copy(
                        isSecurityVerified = isVerified,
                        safetyNumber = cryptoManager.getSafetyNumber(peerId)
                    )
                )
            }
        }
    }

    fun getSafetyNumber(peerId: String): String? = cryptoManager.getSafetyNumber(peerId)
    fun getLocalFingerprint(): String = cryptoManager.getLocalFingerprintHex()
    fun getRemoteFingerprint(peerId: String): String? = cryptoManager.getRemoteFingerprintHex(peerId)
    fun isPeerVerified(peerId: String): Boolean = cryptoManager.isPeerVerified(peerId)
}

package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.engine.PeerEngineManager
import com.example.data.local.PeerDatabase
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.ContactEntity
import com.example.data.local.entities.FileTransferEntity
import com.example.data.model.ActiveCallState
import com.example.data.model.FileTransferProgress
import com.example.data.model.IceServerConfig
import com.example.data.model.PeerConnectionInfo
import com.example.data.model.ServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PeerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PeerDatabase.getInstance(application)
    val engineManager = PeerEngineManager(application)

    val peerId: StateFlow<String?> = engineManager.peerId
    val isServerConnected: StateFlow<Boolean> = engineManager.isServerConnected
    val connectedPeers: StateFlow<Map<String, PeerConnectionInfo>> = engineManager.connectedPeers
    val activeCall: StateFlow<ActiveCallState?> = engineManager.activeCall
    val typingPeers: StateFlow<Map<String, Boolean>> = engineManager.typingPeers
    val activeTransfers: StateFlow<Map<String, FileTransferProgress>> = engineManager.activeTransfers
    private val _customPeerId = MutableStateFlow<String?>(engineManager.savedCustomPeerId)
    val customPeerId: StateFlow<String?> = _customPeerId.asStateFlow()

    private val _nickname = MutableStateFlow("Peer-" + (1000..9999).random())
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    private val _selectedPeerId = MutableStateFlow<String?>(null)
    val selectedPeerId: StateFlow<String?> = _selectedPeerId.asStateFlow()

    private val _selectedRoomId = MutableStateFlow<String?>(null)
    val selectedRoomId: StateFlow<String?> = _selectedRoomId.asStateFlow()

    private val _roomMembers = MutableStateFlow<Set<String>>(emptySet())
    val roomMembers: StateFlow<Set<String>> = _roomMembers.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val contacts: Flow<List<ContactEntity>> = db.contactDao().getAllContactsFlow()
    val allTransfers: Flow<List<FileTransferEntity>> = db.fileTransferDao().getAllTransfersFlow()
    val recentChats: Flow<List<ChatMessageEntity>> = db.chatMessageDao().getRecentChatsFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeConversationMessages: StateFlow<List<ChatMessageEntity>> = _selectedPeerId.flatMapLatest { id ->
        if (id != null) db.chatMessageDao().getMessagesForPeerFlow(id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeRoomMessages: StateFlow<List<ChatMessageEntity>> = _selectedRoomId.flatMapLatest { room ->
        if (room != null) db.chatMessageDao().getMessagesForRoomFlow(room)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            engineManager.engineErrors.collect { err ->
                _snackbarMessage.emit(err)
            }
        }
    }

    fun setNickname(name: String) {
        if (name.isNotBlank()) {
            _nickname.value = name.trim()
        }
    }

    fun setCustomPeerId(desiredId: String?) {
        val cleanId = desiredId?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        _customPeerId.value = cleanId
        engineManager.setCustomPeerId(cleanId)
        viewModelScope.launch {
            if (cleanId != null) {
                _snackbarMessage.emit("Requesting custom Peer ID: $cleanId")
            } else {
                _snackbarMessage.emit("Reverting to random server Peer ID")
            }
        }
    }

    fun selectPeer(peerId: String?) {
        _selectedPeerId.value = peerId
        if (peerId != null) {
            _selectedRoomId.value = null
        }
    }

    fun selectRoom(roomId: String?) {
        _selectedRoomId.value = roomId
        if (roomId != null) {
            _selectedPeerId.value = null
        }
    }

    fun joinMeshRoom(roomId: String) {
        _selectedRoomId.value = roomId
        // Automatically discover connected peers for room
        val members = connectedPeers.value.keys.toSet()
        _roomMembers.value = members
        viewModelScope.launch {
            _snackbarMessage.emit("Joined mesh room: $roomId")
        }
    }

    fun connectToPeer(targetId: String, customNickname: String? = null) {
        val cleanId = targetId.trim()
        if (cleanId.isEmpty()) return
        val nick = customNickname ?: _nickname.value
        engineManager.connectToPeer(cleanId, nick)
        viewModelScope.launch {
            val existing = db.contactDao().getContact(cleanId)
            if (existing == null) {
                db.contactDao().insertOrUpdate(
                    ContactEntity(
                        peerId = cleanId,
                        nickname = customNickname ?: "Peer ${cleanId.take(6)}"
                    )
                )
            }
        }
    }

    fun disconnectPeer(targetId: String) {
        engineManager.disconnectPeer(targetId)
    }

    fun sendChatMessage(targetId: String, text: String, replyToId: String? = null) {
        if (text.isBlank()) return
        engineManager.sendChatMessage(targetId, text.trim(), replyToId)
    }

    fun sendRoomMessage(roomId: String, text: String) {
        if (text.isBlank()) return
        val peers = connectedPeers.value.keys.toList()
        engineManager.broadcastRoomMessage(roomId, text.trim(), peers)
    }

    fun sendTyping(targetId: String, isTyping: Boolean) {
        engineManager.sendTyping(targetId, isTyping)
    }

    fun sendReaction(targetId: String, messageId: String, emoji: String) {
        engineManager.sendReaction(targetId, messageId, emoji)
    }

    fun sendReadReceipt(targetId: String, messageId: String) {
        engineManager.sendReadReceipt(targetId, messageId)
    }

    fun sendFile(targetId: String, uri: Uri, fileName: String, fileSize: Long, mimeType: String) {
        engineManager.offerFile(targetId, uri, fileName, fileSize, mimeType)
        viewModelScope.launch {
            _snackbarMessage.emit("Offering file transfer: $fileName")
        }
    }

    fun acceptIncomingFile(transferId: String) {
        engineManager.acceptIncomingFile(transferId)
    }

    fun declineIncomingFile(transferId: String, reason: String = "Declined by user") {
        engineManager.declineIncomingFile(transferId, reason)
    }

    fun startAudioCall(targetId: String) {
        engineManager.startAudioCall(targetId)
    }

    fun startVideoCall(targetId: String) {
        engineManager.startVideoCall(targetId)
    }

    fun answerCall(isVideo: Boolean) {
        engineManager.answerCall(isVideo)
    }

    fun endCall() {
        engineManager.endCall()
    }

    fun toggleAudio(enabled: Boolean) {
        engineManager.toggleAudio(enabled)
    }

    fun toggleVideo(enabled: Boolean) {
        engineManager.toggleVideo(enabled)
    }

    fun switchCamera() {
        engineManager.switchCamera()
    }

    fun addOrUpdateContact(contact: ContactEntity) {
        viewModelScope.launch {
            db.contactDao().insertOrUpdate(contact)
            _snackbarMessage.emit("Contact saved: ${contact.nickname}")
        }
    }

    fun deleteContact(contact: ContactEntity) {
        viewModelScope.launch {
            db.contactDao().delete(contact)
        }
    }

    fun clearChatHistory(peerId: String) {
        viewModelScope.launch {
            db.chatMessageDao().clearHistoryForPeer(peerId)
            _snackbarMessage.emit("Chat cleared")
        }
    }

    fun clearRoomHistory(roomId: String) {
        viewModelScope.launch {
            db.chatMessageDao().clearHistoryForRoom(roomId)
            _snackbarMessage.emit("Room messages cleared")
        }
    }

    fun updateServerConfig(config: ServerConfig, iceServers: List<IceServerConfig>) {
        engineManager.initializeEngine(peerId.value, config, iceServers)
        viewModelScope.launch {
            _snackbarMessage.emit("Reconnected to PeerServer ${config.host}")
        }
    }

    fun reconnect() {
        engineManager.reconnect()
    }

    fun verifyPeerSecurity(peerId: String, isVerified: Boolean) {
        engineManager.verifyPeerSecurity(peerId, isVerified)
        viewModelScope.launch {
            _snackbarMessage.emit(if (isVerified) "Peer marked as cryptographically verified" else "Verification removed")
        }
    }

    fun getSafetyNumber(peerId: String): String? = engineManager.getSafetyNumber(peerId)
    fun getLocalFingerprint(): String = engineManager.getLocalFingerprint()
    fun getRemoteFingerprint(peerId: String): String? = engineManager.getRemoteFingerprint(peerId)

    override fun onCleared() {
        super.onCleared()
        engineManager.destroy()
    }
}

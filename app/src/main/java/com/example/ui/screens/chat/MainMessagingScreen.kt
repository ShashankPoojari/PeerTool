package com.example.ui.screens.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.ContactEntity
import com.example.data.model.MessageStatus
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import com.example.data.model.PeerConnectionInfo
import com.example.ui.components.ConnectPeerDialog
import com.example.ui.components.CustomPeerIdDialog
import com.example.ui.components.PeerQrDialog
import com.example.ui.components.PeerStatusBadge
import com.example.ui.components.SecurityVerificationDialog
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusOnline
import com.example.ui.viewmodel.PeerViewModel
import java.io.File

/**
 * Main Messaging Screen
 * Behavior:
 * 1. When no peer is connected: Displays the Peer Connection Hub with live signaling indicator,
 *    quick connect input, contact shortcuts, and "Waiting for peer connection" radar.
 * 2. When a peer is connected: Immediately activates and transitions into the Full Chat UI and Window
 *    with real-time message stream, precise delivery timestamps (hh:mm:ss a), file transfers, and text input area.
 */
@Composable
fun MainMessagingScreen(
    viewModel: PeerViewModel
) {
    val context = LocalContext.current
    val peerId by viewModel.peerId.collectAsState()
    val isServerConnected by viewModel.isServerConnected.collectAsState()
    val connectedPeers by viewModel.connectedPeers.collectAsState()
    val activeTransfers by viewModel.activeTransfers.collectAsState()
    val contacts by viewModel.contacts.collectAsState(initial = emptyList())
    val typingPeers by viewModel.typingPeers.collectAsState()

    val selectedPeerId by viewModel.selectedPeerId.collectAsState()
    val activeMessages by viewModel.activeConversationMessages.collectAsState()

    var showQrDialog by remember { mutableStateOf(false) }
    var showConnectDialog by remember { mutableStateOf(false) }
    var showCustomIdDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }

    var inputText by remember { mutableStateOf("") }
    var selectedMessageForReaction by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    // Active connected target peer ID
    val currentTargetPeerId = selectedPeerId ?: connectedPeers.keys.firstOrNull()

    // Check if target peer is connected and online
    val isTargetOnline = currentTargetPeerId != null && connectedPeers.containsKey(currentTargetPeerId)
    val hasConnectedPeer = connectedPeers.isNotEmpty() && isTargetOnline && currentTargetPeerId != null

    // Auto-select first connected peer when connection opens
    LaunchedEffect(connectedPeers) {
        if (selectedPeerId == null && connectedPeers.isNotEmpty()) {
            viewModel.selectPeer(connectedPeers.keys.first())
        }
    }

    // Scroll to bottom on new message
    LaunchedEffect(activeMessages.size) {
        if (activeMessages.isNotEmpty()) {
            listState.animateScrollToItem(activeMessages.size - 1)
        }
    }

    // Automatically send read receipts for incoming messages displayed in the active chat window
    LaunchedEffect(activeMessages, currentTargetPeerId, hasConnectedPeer) {
        if (currentTargetPeerId != null && hasConnectedPeer) {
            activeMessages.filter { it.isIncoming && it.status != MessageStatus.READ }.forEach { msg ->
                viewModel.sendReadReceipt(currentTargetPeerId, msg.id)
            }
        }
    }

    // File picker launcher for P2P chunked binary transfer
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && currentTargetPeerId != null) {
            val (name, size) = queryFileInfo(context, uri)
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            viewModel.sendFile(currentTargetPeerId, uri, name, size, mimeType)
        } else if (currentTargetPeerId == null) {
            Toast.makeText(context, "Connect to a peer first to send files", Toast.LENGTH_SHORT).show()
        }
    }

    val contactNameMap = remember(contacts) {
        contacts.associate { it.peerId to it.nickname }
    }
    val activePeerNickname = currentTargetPeerId?.let { contactNameMap[it] ?: "Peer ${it.take(6)}" } ?: "Peer"
    val latencyMs = currentTargetPeerId?.let { connectedPeers[it]?.latencyMs } ?: 0L
    val isTargetTyping = currentTargetPeerId != null && typingPeers[currentTargetPeerId] == true

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_messaging_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            if (!hasConnectedPeer) {
                // STATE 1: PEER CONNECTION HUB & WAITING SCREEN
                PeerConnectionHubView(
                    peerId = peerId,
                    isServerConnected = isServerConnected,
                    contacts = contacts,
                    onConnect = { targetId, nick ->
                        viewModel.connectToPeer(targetId, nick)
                        viewModel.selectPeer(targetId)
                    },
                    onShowQr = { showQrDialog = true },
                    onCustomId = { showCustomIdDialog = true },
                    onOpenConnectDialog = { showConnectDialog = true }
                )
            } else {
                // STATE 2: ACTIVE CHAT UI AND WINDOW (Activated after peer connects)
                ActiveChatWindowView(
                    currentTargetPeerId = currentTargetPeerId!!,
                    activePeerNickname = activePeerNickname,
                    isTargetOnline = isTargetOnline,
                    latencyMs = latencyMs,
                    isTargetTyping = isTargetTyping,
                    connectedPeers = connectedPeers,
                    contactNameMap = contactNameMap,
                    activeMessages = activeMessages,
                    activeTransfers = activeTransfers,
                    listState = listState,
                    inputText = inputText,
                    onInputTextChange = {
                        inputText = it
                        viewModel.sendTyping(currentTargetPeerId, it.isNotEmpty())
                    },
                    onSendMessage = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendChatMessage(currentTargetPeerId, inputText)
                            viewModel.sendTyping(currentTargetPeerId, false)
                            inputText = ""
                        }
                    },
                    onAttachFile = { filePickerLauncher.launch("*/*") },
                    onSelectPeer = { pId -> viewModel.selectPeer(pId) },
                    onDisconnect = {
                        viewModel.disconnectPeer(currentTargetPeerId)
                        viewModel.selectPeer(null)
                    },
                    onAudioCall = { viewModel.startAudioCall(currentTargetPeerId) },
                    onVideoCall = { viewModel.startVideoCall(currentTargetPeerId) },
                    onSelectForReaction = { msgId -> selectedMessageForReaction = msgId },
                    onSendReaction = { emoji ->
                        selectedMessageForReaction?.let { msgId ->
                            viewModel.sendReaction(currentTargetPeerId, msgId, emoji)
                        }
                        selectedMessageForReaction = null
                    },
                    selectedMessageForReaction = selectedMessageForReaction,
                    onDismissReaction = { selectedMessageForReaction = null },
                    onOpenFile = { path -> openFileIntent(context, path) },
                    onAcceptTransfer = { tId -> viewModel.acceptIncomingFile(tId) },
                    onDeclineTransfer = { tId -> viewModel.declineIncomingFile(tId) },
                    onCustomId = { showCustomIdDialog = true },
                    onShowQr = { showQrDialog = true },
                    onOpenConnectDialog = { showConnectDialog = true },
                    onOpenSecurityDialog = { showSecurityDialog = true }
                )
            }
        }

        // Dialogs
        if (showSecurityDialog && currentTargetPeerId != null) {
            SecurityVerificationDialog(
                peerId = currentTargetPeerId,
                peerNickname = activePeerNickname,
                safetyNumber = viewModel.getSafetyNumber(currentTargetPeerId),
                localFingerprint = viewModel.getLocalFingerprint(),
                remoteFingerprint = viewModel.getRemoteFingerprint(currentTargetPeerId),
                isVerified = connectedPeers[currentTargetPeerId]?.isVerified == true,
                onToggleVerified = { verified ->
                    viewModel.verifyPeerSecurity(currentTargetPeerId, verified)
                },
                onDismiss = { showSecurityDialog = false }
            )
        }

        if (showQrDialog) {
            PeerQrDialog(
                peerId = peerId,
                onDismiss = { showQrDialog = false },
                onCustomizeClick = {
                    showQrDialog = false
                    showCustomIdDialog = true
                }
            )
        }

        if (showCustomIdDialog) {
            CustomPeerIdDialog(
                currentPeerId = peerId,
                onDismiss = { showCustomIdDialog = false },
                onSaveCustomId = { desiredId ->
                    viewModel.setCustomPeerId(desiredId)
                }
            )
        }

        if (showConnectDialog) {
            ConnectPeerDialog(
                onDismiss = { showConnectDialog = false },
                onConnect = { targetId, nick ->
                    viewModel.connectToPeer(targetId, nick)
                    viewModel.selectPeer(targetId)
                }
            )
        }
    }
}

/**
 * Screen displayed BEFORE a peer is connected.
 * Includes Peer ID status indicator, quick connect input field, QR sharing, and live waiting state.
 */
@Composable
private fun PeerConnectionHubView(
    peerId: String?,
    isServerConnected: Boolean,
    contacts: List<ContactEntity>,
    onConnect: (String, String?) -> Unit,
    onShowQr: () -> Unit,
    onCustomId: () -> Unit,
    onOpenConnectDialog: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var inputTargetId by remember { mutableStateOf("") }

    // Pulsing animation for signaling connection status
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
            .testTag("peer_connection_hub")
    ) {
        // Top Connection Status Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("connection_status_header_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PeerStatusBadge(
                    isConnected = isServerConnected,
                    peerId = peerId,
                    latencyMs = 0L,
                    onClick = onShowQr
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCustomId,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("header_custom_id_button")
                    ) {
                        Icon(
                            Icons.Default.Badge,
                            contentDescription = "Custom Peer ID",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onShowQr,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("header_qr_button")
                    ) {
                        Icon(
                            Icons.Default.QrCode,
                            contentDescription = "Show QR Code",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenConnectDialog,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("header_connect_button")
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Connect Peer",
                            tint = PrimaryCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Hero Direct P2P Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.WifiTethering,
                        contentDescription = null,
                        tint = PrimaryCyan,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Peer-to-Peer Messenger",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Direct WebRTC DataChannel connection. Connect to a remote peer to open the real-time chat window.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isServerConnected) StatusOnline.copy(alpha = 0.15f) else StatusError.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .alpha(pulseAlpha)
                            .background(if (isServerConnected) StatusOnline else StatusError)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isServerConnected) "Signaling Online • Waiting for peer connection" else "Connecting to signaling server...",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isServerConnected) StatusOnline else StatusError,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Connect Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Connect to Remote Peer",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Enter or paste the remote peer's ID to open chat",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = inputTargetId,
                    onValueChange = { inputTargetId = it },
                    placeholder = { Text("Remote Peer ID (e.g. peer_abc123)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_connect_input_field"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                clipboardManager.getText()?.text?.let { clipText ->
                                    inputTargetId = clipText.trim()
                                    Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.ContentPaste,
                                contentDescription = "Paste",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (inputTargetId.isNotBlank()) {
                            onConnect(inputTargetId.trim(), null)
                        }
                    },
                    enabled = inputTargetId.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("quick_connect_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryCyan,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect & Open Chat Window", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Saved Contacts / Recent Peers Quick Connect
        if (contacts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Saved Peers (Tap to Connect)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(contacts, key = { it.peerId }) { contact ->
                    Card(
                        modifier = Modifier
                            .clickable { onConnect(contact.peerId, contact.nickname) }
                            .width(160.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryCyan.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = contact.nickname,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "ID: ${contact.peerId.take(12)}...",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap to Connect",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Share My Peer ID Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Share Your Peer ID",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Give your ID to the remote peer so they can connect directly to you:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = peerId.orEmpty().ifEmpty { "Generating Peer ID..." },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryCyan
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (!peerId.isNullOrEmpty()) {
                                clipboardManager.setText(AnnotatedString(peerId))
                                Toast.makeText(context, "Peer ID copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy ID", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onShowQr,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("QR Code", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

/**
 * Screen displayed AFTER a peer is connected.
 * Shows connected peer header bar with telemetry, message history view with precise delivery timestamps,
 * remote typing indicators, and bottom text/file input area.
 */
@Composable
private fun ActiveChatWindowView(
    currentTargetPeerId: String,
    activePeerNickname: String,
    isTargetOnline: Boolean,
    latencyMs: Long,
    isTargetTyping: Boolean,
    connectedPeers: Map<String, PeerConnectionInfo>,
    contactNameMap: Map<String, String>,
    activeMessages: List<ChatMessageEntity>,
    activeTransfers: Map<String, com.example.data.model.FileTransferProgress>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onAttachFile: () -> Unit,
    onSelectPeer: (String) -> Unit,
    onDisconnect: () -> Unit,
    onAudioCall: () -> Unit,
    onVideoCall: () -> Unit,
    onSelectForReaction: (String) -> Unit,
    onSendReaction: (String) -> Unit,
    selectedMessageForReaction: String?,
    onDismissReaction: () -> Unit,
    onOpenFile: (String) -> Unit,
    onAcceptTransfer: (String) -> Unit,
    onDeclineTransfer: (String) -> Unit,
    onCustomId: () -> Unit,
    onShowQr: () -> Unit,
    onOpenConnectDialog: () -> Unit,
    onOpenSecurityDialog: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 1. CONNECTED PEER TOP HEADER BAR
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("connection_status_header_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Disconnect / Return to Hub button
                        IconButton(
                            onClick = onDisconnect,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Return to Hub",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Avatar with online dot
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activePeerNickname.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryCyan,
                                    fontSize = 16.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    .background(if (isTargetOnline) StatusOnline else Color.Gray)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        val isPeerVerified = connectedPeers[currentTargetPeerId]?.isVerified == true
                        Column {
                            Text(
                                text = activePeerNickname,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isTargetOnline) "Direct P2P • ${latencyMs}ms" else "Peer Offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isTargetOnline) StatusOnline else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isPeerVerified) StatusOnline.copy(alpha = 0.15f) else PrimaryCyan.copy(alpha = 0.15f))
                                        .clickable { onOpenSecurityDialog() }
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                        .testTag("e2ee_security_badge"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isPeerVerified) Icons.Default.VerifiedUser else Icons.Default.Lock,
                                        contentDescription = "Cryptographic Security",
                                        tint = if (isPeerVerified) StatusOnline else PrimaryCyan,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isPeerVerified) "Verified" else "E2EE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPeerVerified) StatusOnline else PrimaryCyan
                                    )
                                }
                            }
                        }
                    }

                    // Direct WebRTC Actions (Security, Call, Video, Disconnect)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isPeerVerified = connectedPeers[currentTargetPeerId]?.isVerified == true
                        IconButton(
                            onClick = onOpenSecurityDialog,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("header_security_button")
                        ) {
                            Icon(
                                imageVector = if (isPeerVerified) Icons.Default.VerifiedUser else Icons.Default.Lock,
                                contentDescription = "End-to-End Cryptographic Security",
                                tint = if (isPeerVerified) StatusOnline else PrimaryCyan,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        IconButton(
                            onClick = onAudioCall,
                            enabled = isTargetOnline,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = "Voice Call",
                                tint = if (isTargetOnline) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onVideoCall,
                            enabled = isTargetOnline,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Videocam,
                                contentDescription = "Video Call",
                                tint = if (isTargetOnline) PrimaryCyan else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onDisconnect,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.LinkOff,
                                contentDescription = "Disconnect Peer",
                                tint = StatusError,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // If multiple peers connected, show quick switcher tabs
                if (connectedPeers.size > 1) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(connectedPeers.keys.toList()) { pId ->
                            val isSelected = pId == currentTargetPeerId
                            val name = contactNameMap[pId] ?: pId.take(6)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onSelectPeer(pId) }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. MESSAGE HISTORY VIEW
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("message_history_container")
        ) {
            if (activeMessages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = PrimaryCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Connected with $activePeerNickname",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Encrypted WebRTC connection active. Send a message or attach a file below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item(key = "e2ee_security_banner") {
                        val isPeerVerified = connectedPeers[currentTargetPeerId]?.isVerified == true
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onOpenSecurityDialog() }
                                .testTag("e2ee_security_notice_card"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPeerVerified) StatusOnline.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPeerVerified) Icons.Default.VerifiedUser else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isPeerVerified) StatusOnline else PrimaryCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isPeerVerified)
                                        "Verified Connection • Messages and transfers are end-to-end encrypted with AES-256-GCM. Tap to view safety numbers."
                                    else
                                        "Messages and transfers are end-to-end encrypted with AES-256-GCM & ECDH P-256. Tap to verify safety numbers.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    items(activeMessages, key = { it.id }) { msg ->
                        ChatMessageBubble(
                            message = msg,
                            activeTransfer = activeTransfers[msg.fileTransferId],
                            onSelectForReaction = { onSelectForReaction(msg.id) },
                            onOpenFile = onOpenFile,
                            onAcceptTransfer = onAcceptTransfer,
                            onDeclineTransfer = onDeclineTransfer
                        )
                    }
                }
            }
        }

        // Remote Peer Typing Indicator
        AnimatedVisibility(visible = isTargetTyping) {
            Text(
                text = "$activePeerNickname is typing...",
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryCyan,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 3.dp)
            )
        }

        // Emoji Reaction Bar (if message selected)
        if (selectedMessageForReaction != null) {
            EmojiReactionBar(
                onSelectEmoji = onSendReaction,
                onDismiss = onDismissReaction
            )
        }

        // 3. TEXT & FILE INPUT AREA
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("text_input_area_surface"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attachment Button for Chunked Binary File Sharing
                IconButton(
                    onClick = onAttachFile,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("chat_attach_file_button")
                ) {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = "Attach & Send File",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Text Input Field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    placeholder = {
                        Text(
                            text = "Message $activePeerNickname...",
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("main_chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Send Button
                IconButton(
                    onClick = onSendMessage,
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .testTag("main_send_message_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = if (inputText.isNotBlank()) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun queryFileInfo(context: Context, uri: Uri): Pair<String, Long> {
    var name = "file_${System.currentTimeMillis()}"
    var size = 0L
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) name = cursor.getString(nameIndex)
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }
    } catch (e: Exception) {}
    return Pair(name, size)
}

private fun openFileIntent(context: Context, filePath: String) {
    try {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open file"))
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

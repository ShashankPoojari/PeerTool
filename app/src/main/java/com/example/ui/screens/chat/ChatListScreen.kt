package com.example.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.model.MessageStatus
import com.example.ui.components.ConnectPeerDialog
import com.example.ui.components.PeerQrDialog
import com.example.ui.components.PeerStatusBadge
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.SecondaryIndigo
import com.example.ui.theme.StatusOnline
import com.example.ui.viewmodel.PeerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatListScreen(
    viewModel: PeerViewModel,
    onOpenConversation: (peerId: String) -> Unit
) {
    val peerId by viewModel.peerId.collectAsState()
    val isConnected by viewModel.isServerConnected.collectAsState()
    val recentChats by viewModel.recentChats.collectAsState(initial = emptyList())
    val connectedPeers by viewModel.connectedPeers.collectAsState()
    val contacts by viewModel.contacts.collectAsState(initial = emptyList())

    var showQrDialog by remember { mutableStateOf(false) }
    var showConnectDialog by remember { mutableStateOf(false) }
    var showCustomIdDialog by remember { mutableStateOf(false) }

    val contactNameMap = remember(contacts) {
        contacts.associate { it.peerId to it.nickname }
    }

    Box(modifier = Modifier.fillMaxSize().testTag("chat_list_screen")) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Peer Tool",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Encrypted WebRTC P2P Messenger",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    PeerStatusBadge(
                        isConnected = isConnected,
                        peerId = peerId,
                        onClick = { showQrDialog = true }
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = { showCustomIdDialog = true },
                        modifier = Modifier.testTag("custom_peer_id_quick_button")
                    ) {
                        Icon(
                            androidx.compose.material.icons.Icons.Default.Add,
                            contentDescription = "Custom Peer ID",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { showQrDialog = true },
                        modifier = Modifier.testTag("show_my_qr_button")
                    ) {
                        Icon(
                            Icons.Default.QrCode,
                            contentDescription = "My QR Code",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Chat Items List or Empty State
            if (recentChats.isEmpty() && connectedPeers.isEmpty()) {
                EmptyChatsView(
                    onConnectClick = { showConnectDialog = true },
                    onShowQrClick = { showQrDialog = true }
                )
            } else {
                // Combine recent chats and active connections
                val allPeers = (recentChats.map { it.peerId } + connectedPeers.keys).distinct()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Text(
                            text = "Direct Messages (${allPeers.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(allPeers, key = { it }) { targetPeerId ->
                        val lastMessage = recentChats.firstOrNull { it.peerId == targetPeerId }
                        val isPeerActive = connectedPeers.containsKey(targetPeerId)
                        val peerInfo = connectedPeers[targetPeerId]
                        val displayName = contactNameMap[targetPeerId] ?: "Peer ${targetPeerId.take(6)}"

                        ChatListItem(
                            peerId = targetPeerId,
                            displayName = displayName,
                            lastMessage = lastMessage,
                            isOnline = isPeerActive,
                            latencyMs = peerInfo?.latencyMs ?: 0L,
                            onClick = {
                                viewModel.selectPeer(targetPeerId)
                                onOpenConversation(targetPeerId)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Floating Action Button to connect to peer
        FloatingActionButton(
            onClick = { showConnectDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("new_chat_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.Black
        ) {
            Icon(Icons.Default.Add, contentDescription = "Connect to Peer")
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
            com.example.ui.components.CustomPeerIdDialog(
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
                    onOpenConversation(targetId)
                }
            )
        }
    }
}

@Composable
fun ChatListItem(
    peerId: String,
    displayName: String,
    lastMessage: ChatMessageEntity?,
    isOnline: Boolean,
    latencyMs: Long,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_item_$peerId")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(PrimaryCyan, SecondaryIndigo)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.take(2).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                if (isOnline) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(StatusOnline)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Information
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (lastMessage != null) {
                        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault())
                            .format(Date(lastMessage.timestamp))
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (lastMessage != null && !lastMessage.isIncoming) {
                            val icon = when (lastMessage.status) {
                                MessageStatus.READ -> Icons.Default.DoneAll
                                MessageStatus.DELIVERED -> Icons.Default.DoneAll
                                else -> Icons.Default.Done
                            }
                            val tint = if (lastMessage.status == MessageStatus.READ) PrimaryCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = tint)
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Text(
                            text = lastMessage?.text ?: if (isOnline) "DataChannel open • Ready to chat" else "Tap to connect",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOnline && lastMessage == null) StatusOnline else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isOnline && latencyMs > 0) {
                        Text(
                            text = "${latencyMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryCyan,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChatsView(
    onConnectClick: () -> Unit,
    onShowQrClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "No Peer Connections Yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Peer Tool establishes direct, serverless WebRTC data pipes directly between devices. Connect to a remote peer by entering their Peer ID or share your QR code.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.material3.OutlinedButton(
                onClick = onShowQrClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("My QR Code")
            }

            androidx.compose.material3.Button(
                onClick = onConnectClick,
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Connect Peer")
            }
        }
    }
}

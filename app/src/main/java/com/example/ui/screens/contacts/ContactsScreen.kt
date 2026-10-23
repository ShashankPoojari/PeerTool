package com.example.ui.screens.contacts

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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.data.local.entities.ContactEntity
import com.example.ui.components.ConnectPeerDialog
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.SecondaryIndigo
import com.example.ui.theme.StatusOnline
import com.example.ui.viewmodel.PeerViewModel

@Composable
fun ContactsScreen(
    viewModel: PeerViewModel,
    onOpenChat: (peerId: String) -> Unit
) {
    val contacts by viewModel.contacts.collectAsState(initial = emptyList())
    val connectedPeers by viewModel.connectedPeers.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("contacts_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Peer Directory",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${contacts.size} Saved Peers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_contact_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Peer", color = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (contacts.isEmpty()) {
            EmptyContactsCard(onAddClick = { showAddDialog = true })
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(contacts, key = { it.peerId }) { contact ->
                    val isOnline = connectedPeers.containsKey(contact.peerId)
                    val latency = connectedPeers[contact.peerId]?.latencyMs ?: 0L

                    ContactItemCard(
                        contact = contact,
                        isOnline = isOnline,
                        latencyMs = latency,
                        onOpenChat = {
                            viewModel.selectPeer(contact.peerId)
                            onOpenChat(contact.peerId)
                        },
                        onToggleFavorite = {
                            viewModel.addOrUpdateContact(contact.copy(isFavorite = !contact.isFavorite))
                        },
                        onDelete = {
                            viewModel.deleteContact(contact)
                        },
                        onAudioCall = {
                            viewModel.startAudioCall(contact.peerId)
                        }
                    )
                }
            }
        }

        if (showAddDialog) {
            ConnectPeerDialog(
                onDismiss = { showAddDialog = false },
                onConnect = { peerId, nickname ->
                    viewModel.addOrUpdateContact(
                        ContactEntity(
                            peerId = peerId,
                            nickname = nickname.ifEmpty { "Peer ${peerId.take(6)}" }
                        )
                    )
                    viewModel.connectToPeer(peerId, nickname)
                }
            )
        }
    }
}

@Composable
fun ContactItemCard(
    contact: ContactEntity,
    isOnline: Boolean,
    latencyMs: Long,
    onOpenChat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onAudioCall: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenChat),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(PrimaryCyan, SecondaryIndigo))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.nickname.take(2).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
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

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.nickname,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (contact.isSecurityVerified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = "Cryptographically Verified",
                            tint = StatusOnline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (contact.isFavorite) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = contact.peerId,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isOnline) {
                    Text(
                        text = "Connected • ${latencyMs}ms RTT",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusOnline,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onAudioCall) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = "Call",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onOpenChat) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = "Chat",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (contact.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (contact.isFavorite) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyContactsCard(onAddClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Saved Peers Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Save peer IDs with memorable labels for 1-tap messaging, file sharing, and WebRTC calling.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Add First Peer", color = Color.Black)
            }
        }
    }
}

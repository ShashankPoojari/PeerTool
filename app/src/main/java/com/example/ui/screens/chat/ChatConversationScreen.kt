package com.example.ui.screens.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.SecondaryIndigo
import com.example.ui.theme.StatusOnline
import com.example.ui.viewmodel.PeerViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatConversationScreen(
    peerId: String,
    viewModel: PeerViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val messages by viewModel.activeConversationMessages.collectAsState()
    val connectedPeers by viewModel.connectedPeers.collectAsState()
    val typingPeers by viewModel.typingPeers.collectAsState()
    val contacts by viewModel.contacts.collectAsState(initial = emptyList())

    val isOnline = connectedPeers.containsKey(peerId)
    val latencyMs = connectedPeers[peerId]?.latencyMs ?: 0L
    val isTyping = typingPeers[peerId] == true
    val contact = contacts.firstOrNull { it.peerId == peerId }
    val displayName = contact?.nickname ?: "Peer ${peerId.take(6)}"

    var inputText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var selectedMessageForReaction by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    // Auto-scroll on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Notify read receipts for incoming messages
    LaunchedEffect(messages) {
        messages.filter { it.isIncoming && it.status != MessageStatus.READ }.forEach { msg ->
            viewModel.sendReadReceipt(peerId, msg.id)
        }
    }

    // File picker for high speed chunked file transfer
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val (name, size) = queryFileInfo(context, uri)
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            viewModel.sendFile(peerId, uri, name, size, mimeType)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("chat_conversation_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) StatusOnline else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isOnline) "Direct P2P Online" + (if (latencyMs > 0) " (${latencyMs}ms)" else "") else "Offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOnline) StatusOnline else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Audio Call Action
                IconButton(
                    onClick = {
                        if (isOnline) {
                            viewModel.startAudioCall(peerId)
                        } else {
                            Toast.makeText(context, "Peer must be connected to call", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("start_audio_call_button")
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Audio Call", tint = MaterialTheme.colorScheme.primary)
                }

                // Video Call Action
                IconButton(
                    onClick = {
                        if (isOnline) {
                            viewModel.startVideoCall(peerId)
                        } else {
                            Toast.makeText(context, "Peer must be connected to call", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("start_video_call_button")
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = PrimaryCyan)
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Clear Conversation") },
                            leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                            onClick = {
                                viewModel.clearChatHistory(peerId)
                                showMenu = false
                            }
                        )
                    }
                }
            }

            val activeTransfers by viewModel.activeTransfers.collectAsState()

            // Message History List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageBubble(
                        message = msg,
                        activeTransfer = activeTransfers[msg.fileTransferId],
                        onSelectForReaction = { selectedMessageForReaction = msg.id },
                        onOpenFile = { filePath ->
                            openFileIntent(context, filePath)
                        },
                        onAcceptTransfer = { transferId -> viewModel.acceptIncomingFile(transferId) },
                        onDeclineTransfer = { transferId -> viewModel.declineIncomingFile(transferId) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Typing Indicator
            AnimatedVisibility(visible = isTyping) {
                Text(
                    text = "$displayName is typing...",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryCyan,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            // Reaction Bar (if message selected)
            if (selectedMessageForReaction != null) {
                EmojiReactionBar(
                    onSelectEmoji = { emoji ->
                        selectedMessageForReaction?.let { msgId ->
                            viewModel.sendReaction(peerId, msgId, emoji)
                        }
                        selectedMessageForReaction = null
                    },
                    onDismiss = { selectedMessageForReaction = null }
                )
            }

            // Bottom Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File Attachment Button
                IconButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier.testTag("attach_file_button")
                ) {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = "Attach File",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        viewModel.sendTyping(peerId, it.isNotEmpty())
                    },
                    placeholder = { Text("Type an encrypted P2P message...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendChatMessage(peerId, inputText)
                            viewModel.sendTyping(peerId, false)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("send_message_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessageEntity,
    activeTransfer: com.example.data.model.FileTransferProgress? = null,
    onSelectForReaction: () -> Unit = {},
    onOpenFile: (String) -> Unit = {},
    onAcceptTransfer: (String) -> Unit = {},
    onDeclineTransfer: (String) -> Unit = {}
) {
    val isMe = !message.isIncoming
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val bubbleColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    val timeStr = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(message.timestamp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelectForReaction),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                when (message.type) {
                    MessageType.TEXT -> {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }

                    MessageType.FILE_REF -> {
                        val transfer = activeTransfer
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val path = transfer?.localFilePath ?: message.fileTransferId
                                        if (path != null && File(path).exists()) {
                                            onOpenFile(path)
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryCyan.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.InsertDriveFile,
                                        contentDescription = null,
                                        tint = PrimaryCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = message.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    val sizeLabel = if (transfer != null && transfer.fileSize > 0) {
                                        when {
                                            transfer.fileSize >= 1_000_000 -> "${transfer.fileSize / 1_000_000} MB"
                                            else -> "${transfer.fileSize / 1000} KB"
                                        }
                                    } else "P2P Document"

                                    Text(
                                        text = sizeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            // Dynamic Transfer Status & Actions
                            if (transfer != null) {
                                when (transfer.status) {
                                    com.example.data.model.TransferStatus.PENDING -> {
                                        if (message.isIncoming) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                androidx.compose.material3.OutlinedButton(
                                                    onClick = { onDeclineTransfer(transfer.transferId) },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Decline", fontSize = 11.sp, color = com.example.ui.theme.StatusError)
                                                }
                                                androidx.compose.material3.Button(
                                                    onClick = { onAcceptTransfer(transfer.transferId) },
                                                    modifier = Modifier.weight(1.5f),
                                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.StatusOnline)
                                                ) {
                                                    Text("Accept", fontSize = 11.sp, color = Color.White)
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = "Waiting for peer to accept...",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = textColor.copy(alpha = 0.6f),
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }

                                    com.example.data.model.TransferStatus.IN_PROGRESS -> {
                                        val percent = if (transfer.fileSize > 0) {
                                            (transfer.bytesTransferred.toFloat() / transfer.fileSize.toFloat()).coerceIn(0f, 1f)
                                        } else 0f
                                        Spacer(modifier = Modifier.height(6.dp))
                                        androidx.compose.material3.LinearProgressIndicator(
                                            progress = { percent },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = PrimaryCyan,
                                            trackColor = textColor.copy(alpha = 0.15f)
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${(percent * 100).toInt()}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PrimaryCyan,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (transfer.speedBps > 0) {
                                                Text(
                                                    text = "${transfer.speedBps / 1000} KB/s",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = textColor.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    }

                                    com.example.data.model.TransferStatus.COMPLETED -> {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                transfer.localFilePath?.let { onOpenFile(it) }
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.DoneAll,
                                                contentDescription = null,
                                                tint = com.example.ui.theme.StatusOnline,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Download complete • Tap to open",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = com.example.ui.theme.StatusOnline,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    com.example.data.model.TransferStatus.DECLINED -> {
                                        Text(
                                            text = "Transfer declined",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = com.example.ui.theme.StatusError,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }

                                    else -> {}
                                }
                            } else {
                                // Historical completed or direct file reference
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap to open",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryCyan
                                )
                            }
                        }
                    }

                    MessageType.VOICE -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play Voice Note",
                                tint = PrimaryCyan,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor
                            )
                        }
                    }

                    else -> {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag(if (message.status == MessageStatus.READ) "message_read_status_row" else "message_status_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.isEncrypted) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted with AES-256-GCM",
                            modifier = Modifier
                                .size(10.dp)
                                .testTag("message_lock_icon"),
                            tint = if (isMe && message.status == MessageStatus.READ) PrimaryCyan else textColor.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }

                    if (isMe && message.status == MessageStatus.READ) {
                        // High-visibility Read status indicator: Double cyan checkmarks + "Read" badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PrimaryCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("read_status_indicator")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Seen by recipient (Read)",
                                modifier = Modifier
                                    .size(14.dp)
                                    .testTag("read_double_checkmarks"),
                                tint = PrimaryCyan
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Read • $timeStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        val deliveryPrefix = if (isMe) {
                            when (message.status) {
                                MessageStatus.DELIVERED -> "Delivered"
                                MessageStatus.SENT -> "Sent"
                                MessageStatus.SENDING -> "Sending"
                                MessageStatus.FAILED -> "Failed"
                                else -> "Sent"
                            }
                        } else {
                            "Delivered"
                        }

                        Text(
                            text = "$deliveryPrefix • $timeStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        if (isMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            val statusIcon = when (message.status) {
                                MessageStatus.DELIVERED -> Icons.Default.DoneAll
                                MessageStatus.FAILED -> Icons.Default.Error
                                MessageStatus.SENDING -> Icons.Default.Schedule
                                else -> Icons.Default.Done
                            }
                            val statusTint = when (message.status) {
                                MessageStatus.FAILED -> com.example.ui.theme.StatusError
                                else -> textColor.copy(alpha = 0.7f)
                            }
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = deliveryPrefix,
                                modifier = Modifier
                                    .size(13.dp)
                                    .testTag("delivery_status_icon"),
                                tint = statusTint
                            )
                        }
                    }
                }
            }
        }

        // Reaction Emoji Badge
        if (!message.reactionEmoji.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp, start = if (isMe) 0.dp else 8.dp, end = if (isMe) 8.dp else 0.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = message.reactionEmoji, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun EmojiReactionBar(
    onSelectEmoji: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val emojis = listOf("❤️", "👍", "🔥", "😂", "🚀", "🎉")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        emojis.forEach { emoji ->
            Text(
                text = emoji,
                fontSize = 24.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onSelectEmoji(emoji) }
                    .padding(4.dp)
            )
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
    } catch (e: Exception) {
        // Fallback
    }
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

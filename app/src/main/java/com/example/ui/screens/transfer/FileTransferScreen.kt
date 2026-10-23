package com.example.ui.screens.transfer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.local.entities.FileTransferEntity
import com.example.data.model.FileTransferProgress
import com.example.data.model.TransferStatus
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.SecondaryIndigo
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusOnline
import com.example.ui.viewmodel.PeerViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FileTransferScreen(
    viewModel: PeerViewModel
) {
    val context = LocalContext.current
    val activeTransfers by viewModel.activeTransfers.collectAsState()
    val allTransfers by viewModel.allTransfers.collectAsState(initial = emptyList())
    val connectedPeers by viewModel.connectedPeers.collectAsState()

    var showPeerPicker by remember { mutableStateOf(false) }
    var selectedTargetPeer by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val target = selectedTargetPeer
        if (uri != null && target != null) {
            val (name, size) = queryFileInfo(context, uri)
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            viewModel.sendFile(target, uri, name, size, mimeType)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("file_transfer_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "P2P File Transfer",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "High-speed SCTP Chunked Streaming",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                Button(
                    onClick = {
                        if (connectedPeers.isEmpty()) {
                            Toast.makeText(context, "No active peers connected", Toast.LENGTH_SHORT).show()
                        } else if (connectedPeers.size == 1) {
                            selectedTargetPeer = connectedPeers.keys.first()
                            filePickerLauncher.launch("*/*")
                        } else {
                            showPeerPicker = true
                        }
                    },
                    modifier = Modifier.testTag("send_file_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send File", color = Color.Black)
                }

                DropdownMenu(
                    expanded = showPeerPicker,
                    onDismissRequest = { showPeerPicker = false }
                ) {
                    connectedPeers.keys.forEach { peerId ->
                        DropdownMenuItem(
                            text = { Text("Send to $peerId") },
                            onClick = {
                                selectedTargetPeer = peerId
                                showPeerPicker = false
                                filePickerLauncher.launch("*/*")
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Active Transfers Section
            if (activeTransfers.isNotEmpty()) {
                item {
                    Text(
                        text = "Active Streams (${activeTransfers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryCyan
                    )
                }

                items(activeTransfers.values.toList(), key = { it.transferId }) { progress ->
                    ActiveTransferCard(
                        progress = progress,
                        onAccept = { viewModel.acceptIncomingFile(progress.transferId) },
                        onDecline = { viewModel.declineIncomingFile(progress.transferId) }
                    )
                }
            }

            // Transfer History Section
            item {
                Text(
                    text = "Transfer History (${allTransfers.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (allTransfers.isEmpty()) {
                item {
                    EmptyTransfersCard()
                }
            } else {
                items(allTransfers, key = { it.id }) { transfer ->
                    HistoricalTransferCard(
                        transfer = transfer,
                        onOpenFile = { path -> openFileIntent(context, path) }
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveTransferCard(
    progress: FileTransferProgress,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val percent = if (progress.fileSize > 0) {
        (progress.bytesTransferred.toFloat() / progress.fileSize.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val speedText = formatSpeed(progress.speedBps)
    val sizeText = formatFileSize(progress.fileSize)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (progress.isIncoming) SecondaryIndigo else PrimaryCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (progress.isIncoming) Icons.Default.Download else Icons.Default.Upload,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = progress.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${if (progress.isIncoming) "From" else "To"}: ${progress.peerId.take(10)}... • $sizeText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (progress.status == TransferStatus.IN_PROGRESS) {
                    Text(
                        text = speedText,
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (progress.status == TransferStatus.PENDING && progress.isIncoming) {
                Text(
                    text = "Peer wants to send you a file directly. Accept to begin streaming?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDecline) {
                        Text("Decline", color = StatusError)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusOnline)
                    ) {
                        Text("Accept & Download")
                    }
                }
            } else {
                LinearProgressIndicator(
                    progress = { percent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PrimaryCyan,
                    trackColor = MaterialTheme.colorScheme.surface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Chunk ${progress.currentChunk} / ${progress.totalChunks}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(percent * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryCyan
                    )
                }
            }
        }
    }
}

@Composable
fun HistoricalTransferCard(
    transfer: FileTransferEntity,
    onOpenFile: (String) -> Unit
) {
    val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(transfer.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (transfer.status) {
                    TransferStatus.COMPLETED -> Icons.Default.CheckCircle
                    TransferStatus.FAILED -> Icons.Default.Error
                    else -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                tint = when (transfer.status) {
                    TransferStatus.COMPLETED -> StatusOnline
                    TransferStatus.FAILED -> StatusError
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transfer.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatFileSize(transfer.fileSize)} • ${if (transfer.isIncoming) "Received" else "Sent"} • $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!transfer.localFilePath.isNullOrEmpty()) {
                IconButton(onClick = { onOpenFile(transfer.localFilePath) }) {
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = "Open File",
                        tint = PrimaryCyan
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyTransfersCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
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
                Icons.Default.FolderZip,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Zero-Server File Transfer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Files are streamed directly between browser/devices with WebRTC DataChannels in 32KB binary chunks with SHA-256 integrity checks.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun formatSpeed(bps: Long): String {
    return when {
        bps >= 1_000_000 -> String.format(Locale.getDefault(), "%.1f MB/s", bps / 1_000_000f)
        bps >= 1_000 -> String.format(Locale.getDefault(), "%d KB/s", bps / 1_000)
        else -> "$bps B/s"
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> String.format(Locale.getDefault(), "%.2f GB", bytes / 1_000_000_000f)
        bytes >= 1_000_000 -> String.format(Locale.getDefault(), "%.1f MB", bytes / 1_000_000f)
        bytes >= 1_000 -> "${bytes / 1000} KB"
        else -> "$bytes B"
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

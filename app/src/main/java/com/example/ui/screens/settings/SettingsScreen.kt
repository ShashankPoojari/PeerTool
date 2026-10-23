package com.example.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IceServerConfig
import com.example.data.model.ServerConfig
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.StatusOnline
import com.example.ui.viewmodel.PeerViewModel

@Composable
fun SettingsScreen(
    viewModel: PeerViewModel
) {
    val context = LocalContext.current
    val peerId by viewModel.peerId.collectAsState()
    val isConnected by viewModel.isServerConnected.collectAsState()
    val nickname by viewModel.nickname.collectAsState()

    var nickInput by remember(nickname) { mutableStateOf(nickname) }
    var hostInput by remember { mutableStateOf(viewModel.engineManager.currentServerConfig.host) }
    var portInput by remember { mutableStateOf(viewModel.engineManager.currentServerConfig.port.toString()) }
    var pathInput by remember { mutableStateOf(viewModel.engineManager.currentServerConfig.path) }
    var secureSSL by remember { mutableStateOf(viewModel.engineManager.currentServerConfig.secure) }
    var keyInput by remember { mutableStateOf(viewModel.engineManager.currentServerConfig.key) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
            .testTag("settings_screen")
    ) {
        Text(
            text = "Settings & Network",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "PeerJS Signaling Server & WebRTC Configuration",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Identity Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "My Peer Identity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = nickInput,
                        onValueChange = { nickInput = it },
                        label = { Text("Display Nickname") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.setNickname(nickInput)
                            Toast.makeText(context, "Nickname updated", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Save", color = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Active Peer ID:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = peerId ?: "Connecting to signaling server...",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = if (isConnected) PrimaryCyan else Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                    if (peerId != null) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Peer ID", peerId))
                                Toast.makeText(context, "Peer ID copied", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Peer ID Creator Section
                Text(
                    text = "Create Custom Peer ID",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Claim a fixed, memorable identifier on the PeerServer network",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                var customIdInput by remember(peerId) { mutableStateOf(viewModel.customPeerId.value ?: "") }
                val isValidId = customIdInput.length in 3..36 && customIdInput.all { it.isLetterOrDigit() || it == '-' || it == '_' }

                val adjectives = listOf("cyber", "quantum", "hyper", "stellar", "neon", "crypto", "nexus", "pulse")
                val nouns = listOf("falcon", "matrix", "rover", "spark", "node", "beacon", "relay", "vortex")

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customIdInput,
                    onValueChange = { customIdInput = it.trim().lowercase() },
                    label = { Text("Desired Custom Peer ID") },
                    placeholder = { Text("e.g. alice-device, my-node-99") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_custom_peer_id_input"),
                    singleLine = true,
                    supportingText = {
                        Text("3-36 alphanumeric characters, hyphens, and underscores.")
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            val adj = adjectives.random()
                            val noun = nouns.random()
                            val num = (10..99).random()
                            customIdInput = "$adj-$noun-$num"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Suggest ID", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (isValidId) {
                                viewModel.setCustomPeerId(customIdInput)
                                Toast.makeText(context, "Registering ID: $customIdInput", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = isValidId,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("settings_claim_custom_peer_id_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Claim Custom ID", color = Color.Black, fontSize = 12.sp)
                    }
                }

                if (viewModel.customPeerId.value != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    androidx.compose.material3.TextButton(
                        onClick = {
                            viewModel.setCustomPeerId(null)
                            customIdInput = ""
                            Toast.makeText(context, "Reverted to random server ID", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Revert to Random Server ID", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PeerJS Signaling Server Config
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Dns, contentDescription = null, tint = PrimaryCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PeerJS Signaling Server",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = hostInput,
                    onValueChange = { hostInput = it },
                    label = { Text("Server Host (default: 0.peerjs.com)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = portInput,
                        onValueChange = { portInput = it },
                        label = { Text("Port") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = pathInput,
                        onValueChange = { pathInput = it },
                        label = { Text("Path") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Use HTTPS / WSS (Secure SSL)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = secureSSL,
                        onCheckedChange = { secureSSL = it }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val parsedPort = portInput.toIntOrNull() ?: 443
                        val config = ServerConfig(
                            host = hostInput.trim().ifEmpty { "0.peerjs.com" },
                            port = parsedPort,
                            path = pathInput.trim().ifEmpty { "/" },
                            secure = secureSSL,
                            key = keyInput.trim().ifEmpty { "peerjs" }
                        )
                        viewModel.updateServerConfig(config, viewModel.engineManager.currentIceServers)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Apply & Reconnect Server", color = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // WebRTC STUN/TURN Telemetry
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = StatusOnline)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "WebRTC Engine Telemetry",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                TelemetryRow("Signaling Protocol", "PeerJS v1.5.4 WebSocket")
                TelemetryRow("DataChannel Protocol", "SCTP over DTLS (RFC 8831)")
                TelemetryRow("Chunk Buffer Limit", "256 KB Backpressure")
                TelemetryRow("Audio Codec", "Opus (48kHz Stereo)")
                TelemetryRow("Video Codec", "VP8 / VP9 / H.264 Unified Plan")
                TelemetryRow("STUN Server 1", "stun:stun.l.google.com:19302")
                TelemetryRow("STUN Server 2", "stun:global.stun.twilio.com:3478")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

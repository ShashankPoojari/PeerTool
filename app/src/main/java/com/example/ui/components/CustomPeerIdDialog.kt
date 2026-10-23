package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.PrimaryCyan

@Composable
fun CustomPeerIdDialog(
    currentPeerId: String?,
    onDismiss: () -> Unit,
    onSaveCustomId: (String?) -> Unit
) {
    var desiredId by remember { mutableStateOf(currentPeerId ?: "") }

    val adjectives = listOf("cyber", "quantum", "hyper", "stellar", "neon", "crypto", "nexus", "pulse", "echo", "turbo")
    val nouns = listOf("falcon", "matrix", "rover", "spark", "node", "beacon", "relay", "vortex", "pilot", "flow")

    fun generateRandomAlias(): String {
        val adj = adjectives.random()
        val noun = nouns.random()
        val num = (10..99).random()
        return "$adj-$noun-$num"
    }

    val isValid = desiredId.length in 3..36 && desiredId.all { it.isLetterOrDigit() || it == '-' || it == '_' }
    val isError = desiredId.isNotEmpty() && !isValid

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("custom_peer_id_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Badge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Custom Peer ID",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Claim a unique identifier on PeerServer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = desiredId,
                    onValueChange = { desiredId = it.trim().lowercase() },
                    label = { Text("Desired Peer ID") },
                    placeholder = { Text("e.g. alice-phone, cyber-node-1") },
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text(
                                "3-36 characters. Letters, numbers, hyphens, and underscores only.",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("Your peers can connect to you directly using this ID.")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_peer_id_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Suggestion Generator
                OutlinedButton(
                    onClick = { desiredId = generateRandomAlias() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = PrimaryCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Suggest Memorable Alias", fontSize = 13.sp)
                }

                if (desiredId.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = "Preview Identity:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = desiredId,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = PrimaryCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            onSaveCustomId(null) // Reset to random ID
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Random ID", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (isValid) {
                                onSaveCustomId(desiredId)
                                onDismiss()
                            }
                        },
                        enabled = isValid,
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("claim_custom_peer_id_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Claim & Register", color = Color.Black, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

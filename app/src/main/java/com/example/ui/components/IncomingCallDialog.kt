package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ActiveCallState
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusOnline

@Composable
fun IncomingCallDialog(
    call: ActiveCallState,
    onAnswer: (isVideo: Boolean) -> Unit,
    onDecline: () -> Unit
) {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        while (true) {
            scale.animateTo(1.15f, animationSpec = tween(600))
            scale.animateTo(1.0f, animationSpec = tween(600))
        }
    }

    Dialog(onDismissRequest = onDecline) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("incoming_call_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(scale.value)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (call.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = "Incoming call icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (call.isVideo) "Incoming Video Call" else "Incoming Audio Call",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Peer: ${call.peerId.take(12)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Decline
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledIconButton(
                            onClick = onDecline,
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("decline_call_button"),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = StatusError)
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = "Decline", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Decline", style = MaterialTheme.typography.labelSmall, color = StatusError)
                    }

                    // Answer Audio
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledIconButton(
                            onClick = { onAnswer(false) },
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("answer_audio_button"),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Answer Audio", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Voice", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                    }

                    // Answer Video
                    if (call.isVideo) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FilledIconButton(
                                onClick = { onAnswer(true) },
                                modifier = Modifier
                                    .size(56.dp)
                                    .testTag("answer_video_button"),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = StatusOnline)
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = "Answer Video", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Video", style = MaterialTheme.typography.labelSmall, color = StatusOnline)
                        }
                    }
                }
            }
        }
    }
}

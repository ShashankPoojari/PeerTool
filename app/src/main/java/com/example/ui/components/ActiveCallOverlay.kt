package com.example.ui.components

import android.webkit.WebView
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.ActiveCallState
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.StatusError

@Composable
fun ActiveCallOverlay(
    call: ActiveCallState,
    webView: WebView?,
    onMuteToggle: (Boolean) -> Unit,
    onVideoToggle: (Boolean) -> Unit,
    onSwitchCamera: () -> Unit,
    onEndCall: () -> Unit
) {
    val durationText = String.format(
        "%02d:%02d",
        call.durationSeconds / 60,
        call.durationSeconds % 60
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("active_call_overlay"),
        color = DarkBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Render WebView holding WebRTC video tracks
            if (webView != null) {
                AndroidView(
                    factory = {
                        webView.parent?.let { parent ->
                            (parent as? android.view.ViewGroup)?.removeView(webView)
                        }
                        webView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top Status Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (call.isConnected) "Call Active" else "Calling...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Peer: ${call.peerId.take(12)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Bottom In-Call Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 40.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Toggle
                    FilledIconButton(
                        onClick = { onMuteToggle(!call.isMuted) },
                        modifier = Modifier.testTag("toggle_mute_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (call.isMuted) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = if (call.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            tint = if (call.isMuted) StatusError else Color.White
                        )
                    }

                    // Video Toggle
                    if (call.isVideo) {
                        FilledIconButton(
                            onClick = { onVideoToggle(!call.isVideoEnabled) },
                            modifier = Modifier.testTag("toggle_video_button"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (!call.isVideoEnabled) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = if (call.isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                contentDescription = "Video",
                                tint = if (!call.isVideoEnabled) StatusError else Color.White
                            )
                        }

                        // Switch Camera
                        FilledIconButton(
                            onClick = onSwitchCamera,
                            modifier = Modifier.testTag("switch_camera_button"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(
                                Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera",
                                tint = Color.White
                            )
                        }
                    }

                    // End Call
                    FilledIconButton(
                        onClick = onEndCall,
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("end_call_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = StatusError)
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White)
                    }
                }
            }
        }
    }
}

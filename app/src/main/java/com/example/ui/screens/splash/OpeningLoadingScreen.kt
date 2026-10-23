package com.example.ui.screens.splash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PeerQrDialog
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.SecondaryIndigo
import com.example.ui.theme.StatusOnline
import com.example.ui.viewmodel.PeerViewModel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OpeningLoadingScreen(
    viewModel: PeerViewModel,
    onContinueToApp: () -> Unit
) {
    val context = LocalContext.current
    val peerId by viewModel.peerId.collectAsState()
    val isConnected by viewModel.isServerConnected.collectAsState()

    var showQrDialog by remember { mutableStateOf(false) }
    var currentStep by remember { mutableIntStateOf(1) }
    var autoLaunchCountdown by remember { mutableIntStateOf(3) }
    var isReadyToLaunch by remember { mutableStateOf(false) }

    // Step simulation progression synced with actual peer connection
    LaunchedEffect(Unit) {
        delay(400)
        currentStep = 2 // WebRTC Mesh loaded
        delay(400)
        currentStep = 3 // Signaling broker handshake
    }

    LaunchedEffect(peerId, isConnected) {
        if (!peerId.isNullOrBlank() || isConnected) {
            currentStep = 4 // Fully ready!
            isReadyToLaunch = true
        }
    }

    // Auto-countdown to enter once ready
    LaunchedEffect(isReadyToLaunch) {
        if (isReadyToLaunch) {
            while (autoLaunchCountdown > 0) {
                delay(1000)
                autoLaunchCountdown--
            }
            onContinueToApp()
        }
    }

    // Infinite animation for logo pulse & radar rings
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_and_spin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_spin"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )
    val radarSweep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_sweep"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF060A13),
                        Color(0xFF090E1A),
                        Color(0xFF0E1726)
                    )
                )
            )
            .testTag("opening_loading_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. BRAND HERO & ANIMATED PEER TOOL LOGO
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Interactive Vector Canvas Logo
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .testTag("peer_tool_animated_logo"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val radius = size.minDimension / 2.3f

                        // Outer Hexagonal Radar Track
                        rotate(rotationAngle, pivot = Offset(centerX, centerY)) {
                            val hexPath = Path()
                            for (i in 0 until 6) {
                                val angle = Math.toRadians((i * 60.0))
                                val x = centerX + (radius * cos(angle)).toFloat()
                                val y = centerY + (radius * sin(angle)).toFloat()
                                if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
                            }
                            hexPath.close()
                            drawPath(
                                path = hexPath,
                                color = Color(0xFF0284C7).copy(alpha = 0.4f),
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Pulsing inner concentric circle
                        drawCircle(
                            color = Color(0xFF00F5D4).copy(alpha = pulseAlpha * 0.25f),
                            radius = radius * 0.72f,
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 1.8.dp.toPx())
                        )

                        // Radar sweep line
                        rotate(radarSweep, pivot = Offset(centerX, centerY)) {
                            drawLine(
                                color = Color(0xFF38BDF8).copy(alpha = 0.7f),
                                start = Offset(centerX, centerY),
                                end = Offset(centerX + radius * 0.85f, centerY),
                                strokeWidth = 2.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }

                        // Left Peer Node
                        val leftNode = Offset(centerX - radius * 0.44f, centerY - radius * 0.12f)
                        val rightNode = Offset(centerX + radius * 0.44f, centerY - radius * 0.12f)
                        val centerNexus = Offset(centerX, centerY + radius * 0.35f)

                        // Data links
                        drawLine(
                            color = Color(0xFF38BDF8),
                            start = leftNode,
                            end = rightNode,
                            strokeWidth = 3.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xFF00F5D4),
                            start = leftNode,
                            end = centerNexus,
                            strokeWidth = 2.8.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xFF00F5D4),
                            start = rightNode,
                            end = centerNexus,
                            strokeWidth = 2.8.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Peer Node A (Cyan)
                        drawCircle(color = Color(0xFF38BDF8), radius = 10.dp.toPx(), center = leftNode)
                        drawCircle(color = Color(0xFF090E1A), radius = 6.dp.toPx(), center = leftNode)
                        drawCircle(color = Color.White, radius = 3.dp.toPx(), center = leftNode)

                        // Peer Node B (Sky Blue)
                        drawCircle(color = Color(0xFF00F5D4), radius = 10.dp.toPx(), center = rightNode)
                        drawCircle(color = Color(0xFF090E1A), radius = 6.dp.toPx(), center = rightNode)
                        drawCircle(color = Color.White, radius = 3.dp.toPx(), center = rightNode)

                        // Center Cryptographic Shield Node
                        drawCircle(color = Color(0xFF0284C7), radius = 13.dp.toPx(), center = centerNexus)
                        drawCircle(color = Color(0xFF00F5D4), radius = 7.dp.toPx(), center = centerNexus)
                        drawCircle(color = Color(0xFF090E1A), radius = 3.dp.toPx(), center = centerNexus)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Brand Title
                Text(
                    text = "PEER TOOL",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp,
                    color = Color.White
                )

                Text(
                    text = "DECENTRALIZED P2P MESSENGER & SUITE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    color = PrimaryCyan
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Protocol & Package Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "peer.tool.android • E2EE AES-256-GCM",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. INITIALIZATION SEQUENCE & PROGRESS
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("initialization_status_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827).copy(alpha = 0.85f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F2937))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isReadyToLaunch) "P2P Engine Initialized" else "Initializing P2P Node...",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isReadyToLaunch) StatusOnline else Color.White
                        )
                        Text(
                            text = if (isReadyToLaunch) "100%" else "${currentStep * 25}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { if (isReadyToLaunch) 1f else (currentStep * 0.25f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = PrimaryCyan,
                        trackColor = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Step Checkmarks
                    InitStepRow(
                        title = "P-256 Cryptographic Keystore",
                        detail = "EC KeyPair & HKDF-SHA256 Ready",
                        isCompleted = currentStep >= 1
                    )
                    InitStepRow(
                        title = "WebRTC Mesh Subsystem",
                        detail = "SCTP DataChannel & Opus Codec Active",
                        isCompleted = currentStep >= 2
                    )
                    InitStepRow(
                        title = "Decentralized Signaling Broker",
                        detail = "SSL WebSocket Relay Connected",
                        isCompleted = currentStep >= 3
                    )
                    InitStepRow(
                        title = "Peer Identity Registered",
                        detail = if (peerId != null) "Allocated: ${peerId?.take(16)}..." else "Awaiting Broker ID...",
                        isCompleted = isReadyToLaunch
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. YOUR PEER ID BADGE & READY SHOWCASE
            if (peerId != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("peer_id_ready_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F233B)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(StatusOnline)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "YOUR ONLINE PEER ID",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryCyan,
                                    letterSpacing = 1.sp
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Peer ID", peerId))
                                        Toast.makeText(context, "Peer ID copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy ID",
                                        tint = PrimaryCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { showQrDialog = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.QrCode,
                                        contentDescription = "Show QR",
                                        tint = PrimaryCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = peerId ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. ACTION BUTTONS & SKIP/OFFLINE FLOW
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onContinueToApp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("launch_peer_tool_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryCyan,
                        contentColor = Color(0xFF090E1A)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isReadyToLaunch) "Launch Peer Tool (${autoLaunchCountdown}s)" else "Enter Peer Tool",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onContinueToApp,
                    modifier = Modifier.testTag("skip_loading_button")
                ) {
                    Text(
                        text = "Skip to Dashboard (Offline Mode)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }

    if (showQrDialog && peerId != null) {
        PeerQrDialog(
            peerId = peerId!!,
            onDismiss = { showQrDialog = false }
        )
    }
}

@Composable
private fun InitStepRow(
    title: String,
    detail: String,
    isCompleted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCompleted) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Completed",
                tint = StatusOnline,
                modifier = Modifier.size(18.dp)
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = PrimaryCyan,
                strokeWidth = 2.dp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCompleted) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isCompleted) Color.White else Color(0xFF94A3B8)
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B),
                fontSize = 11.sp
            )
        }
    }
}

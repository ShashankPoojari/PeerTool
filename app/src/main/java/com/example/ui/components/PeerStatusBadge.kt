package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StatusConnecting
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusOnline

@Composable
fun PeerStatusBadge(
    isConnected: Boolean,
    peerId: String?,
    latencyMs: Long = 0L,
    onClick: () -> Unit = {}
) {
    val statusColor = when {
        isConnected && peerId != null -> StatusOnline
        isConnected -> StatusConnecting
        else -> StatusOffline
    }

    val statusText = when {
        isConnected && peerId != null -> "ID: ${peerId.take(6)}...${peerId.takeLast(4)}"
        isConnected -> "Connecting..."
        else -> "Server Offline"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Row(
        modifier = Modifier
            .testTag("peer_status_badge")
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .scale(if (isConnected) pulseScale else 1f)
                .clip(CircleShape)
                .background(statusColor)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp
        )

        if (latencyMs > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${latencyMs}ms",
                style = MaterialTheme.typography.labelSmall,
                color = if (latencyMs < 100) StatusOnline else StatusConnecting,
                fontSize = 11.sp
            )
        }
    }
}

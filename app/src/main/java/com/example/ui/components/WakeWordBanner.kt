package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PujaMood

@Composable
fun WakeWordBanner(
    isHandsFreeActive: Boolean,
    isListening: Boolean,
    mood: PujaMood,
    onToggleHandsFree: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WakePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("wake_word_banner"),
        shape = RoundedCornerShape(16.dp),
        color = if (isHandsFreeActive) Color(0xFF1E1038) else Color(0xFF170C2A),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isHandsFreeActive) mood.accentGlow.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (isHandsFreeActive) {
                                Brush.radialGradient(
                                    listOf(mood.primaryColor, mood.secondaryColor)
                                )
                            } else {
                                Brush.linearGradient(
                                    listOf(Color(0xFF372054), Color(0xFF231338))
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isHandsFreeActive) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .scale(if (isListening) pulseScale else 1f)
                                .clip(CircleShape)
                                .background(mood.accentGlow.copy(alpha = 0.35f))
                        )
                    }

                    Icon(
                        imageVector = if (isHandsFreeActive) Icons.Default.Hearing else Icons.Default.MicOff,
                        contentDescription = "Hands-free wake-word",
                        tint = if (isHandsFreeActive) Color.White else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isHandsFreeActive) "Hands-Free & Background" else "Hands-Free Voice",
                        color = if (isHandsFreeActive) Color.White else Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isHandsFreeActive) "• Active" else "• Off",
                        color = if (isHandsFreeActive) mood.accentGlow else Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                }
            }

            Switch(
                checked = isHandsFreeActive,
                onCheckedChange = onToggleHandsFree,
                modifier = Modifier.testTag("hands_free_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = mood.primaryColor,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color(0xFF2C1B47)
                )
            )
        }
    }
}

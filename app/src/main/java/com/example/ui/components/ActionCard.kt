package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PujaAction

@Composable
fun ActionCard(
    action: PujaAction,
    rawJson: String?,
    onExecuteAction: (PujaAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var showJson by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("action_card_${action.type}"),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1E1433),
        tonalElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = if (action is PujaAction.TriggerSos) {
                    listOf(Color(0xFFFF1744), Color(0xFFFF5252))
                } else {
                    listOf(Color(0xFF8A2BE2).copy(alpha = 0.6f), Color(0xFF00F2FE).copy(alpha = 0.4f))
                }
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Action Header with badge & JSON toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val (icon, badgeTitle, badgeColor) = when (action) {
                        is PujaAction.OpenApp -> Triple(Icons.Default.Launch, "OPEN APP", Color(0xFF00E5FF))
                        is PujaAction.SendMessage -> Triple(
                            if (action.platform.contains("what", true)) Icons.Default.Chat else Icons.Default.Message,
                            "SEND ${action.platform.uppercase()}",
                            Color(0xFF25D366)
                        )
                        is PujaAction.MakeCall -> Triple(
                            if (action.callType.contains("video", true)) Icons.Default.Videocam else Icons.Default.Call,
                            "${action.callType.uppercase()} CALL (${action.platform.uppercase()})",
                            Color(0xFFFF7043)
                        )
                        is PujaAction.TriggerSos -> Triple(Icons.Default.Warning, "EMERGENCY SOS", Color(0xFFFF1744))
                        is PujaAction.CameraControl -> {
                            val camTitle = when (action.mode.lowercase()) {
                                "capture_photo" -> "CAPTURE PHOTO"
                                "record_video" -> "RECORD VIDEO"
                                "analyze_view" -> "ANALYZE SCENE"
                                else -> "OPEN CAMERA"
                            }
                            val camIcon = when (action.mode.lowercase()) {
                                "capture_photo" -> Icons.Default.PhotoCamera
                                "record_video" -> Icons.Default.Videocam
                                "analyze_view" -> Icons.Default.Visibility
                                else -> Icons.Default.CameraAlt
                            }
                            Triple(camIcon, camTitle, Color(0xFFFF4081))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(badgeColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = badgeTitle,
                            tint = badgeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = badgeTitle,
                        color = badgeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(
                    onClick = { showJson = !showJson },
                    modifier = Modifier.size(32.dp).testTag("toggle_json_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "View Action JSON",
                        tint = if (showJson) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Details Body
            when (action) {
                is PujaAction.OpenApp -> {
                    Text(
                        text = "Target App: ${action.appName}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Ready to launch ${action.appName} on your Android device.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onExecuteAction(action) },
                        modifier = Modifier.fillMaxWidth().testTag("execute_open_app_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open ${action.appName}", fontWeight = FontWeight.Bold)
                    }
                }

                is PujaAction.SendMessage -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF130924))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "To: ${action.contact}",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"${action.message}\"",
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onExecuteAction(action) },
                        modifier = Modifier.fillMaxWidth().testTag("execute_send_message_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (action.platform.contains("what", true)) Color(0xFF25D366) else Color(0xFF7C4DFF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send via ${action.platform.replaceFirstChar { it.uppercase() }}", fontWeight = FontWeight.Bold)
                    }
                }

                is PujaAction.MakeCall -> {
                    Text(
                        text = "Contact: ${action.contact}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Type: ${action.callType.replaceFirstChar { it.uppercase() }} • Platform: ${action.platform.replaceFirstChar { it.uppercase() }}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onExecuteAction(action) },
                        modifier = Modifier.fillMaxWidth().testTag("execute_make_call_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6D00)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Call ${action.contact}", fontWeight = FontWeight.Bold)
                    }
                }

                is PujaAction.TriggerSos -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFF1744).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFFF1744), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "⚠️ EMERGENCY PROTOCOL ACTIVE",
                            color = Color(0xFFFF5252),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Triggers alarm siren, SOS vibration pattern, location coordinates dispatch & emergency dialer.",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onExecuteAction(action) },
                        modifier = Modifier.fillMaxWidth().testTag("execute_trigger_sos_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Trigger SOS Alarm & Dispatch", fontWeight = FontWeight.ExtraBold)
                    }
                }

                is PujaAction.CameraControl -> {
                    val modeLabel = when (action.mode.lowercase()) {
                        "capture_photo" -> "📸 Take Photo / Selfie"
                        "record_video" -> "🎥 Record Video"
                        "analyze_view" -> "👁️ Vision Scene Analysis"
                        else -> "📷 Open Camera"
                    }
                    Text(
                        text = modeLabel,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Mode: ${action.mode.replaceFirstChar { it.uppercase() }}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onExecuteAction(action) },
                        modifier = Modifier.fillMaxWidth().testTag("execute_camera_control_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(modeLabel, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Expandable JSON Payload Inspector
            AnimatedVisibility(
                visible = showJson,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D0618))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "JSON Action Output:",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rawJson ?: "{}",
                        color = Color(0xFF80D8FF),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

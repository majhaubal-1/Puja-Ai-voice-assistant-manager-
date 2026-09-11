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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.InteractionEntity
import com.example.data.model.PujaAction
import com.example.data.model.PujaMood
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatBubble(
    interaction: InteractionEntity,
    onReplaySpeech: (String) -> Unit,
    onExecuteAction: (PujaAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeStr = timeFormat.format(Date(interaction.timestamp))
    val mood = PujaMood.fromString(interaction.detectedMood)
    val action = PujaAction.parseFromJson(interaction.actionJson)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("interaction_item_${interaction.id}")
    ) {
        // User Query Bubble (Right-aligned)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = Color(0xFF2C194D),
                modifier = Modifier.widthIn(max = 290.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = interaction.userQuery,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = timeStr,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4A148C)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Puja Response Bubble (Left-aligned)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(mood.primaryColor, mood.secondaryColor)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = mood.emoji, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.widthIn(max = 310.dp)) {
                // Header with name & mood tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Puja",
                        color = Color(0xFFFF4081),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = mood.primaryColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${mood.displayName} • ${interaction.sassLevel}% Sass",
                            color = mood.accentGlow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                    color = Color(0xFF1B102E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, mood.primaryColor.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = interaction.spokenText.ifBlank { interaction.assistantResponse },
                            color = Color(0xFFF1E4FF),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = timeStr,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )

                            IconButton(
                                onClick = { onReplaySpeech(interaction.spokenText) },
                                modifier = Modifier.size(26.dp).testTag("replay_speech_${interaction.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Speak again",
                                    tint = mood.accentGlow,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // If this interaction generated a phone action, render the interactive ActionCard!
                if (action != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    ActionCard(
                        action = action,
                        rawJson = interaction.actionJson,
                        onExecuteAction = onExecuteAction
                    )
                }
            }
        }
    }
}

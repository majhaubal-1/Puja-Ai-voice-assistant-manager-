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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.InteractionEntity
import com.example.data.model.PujaMood

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodAnalyticsBottomSheet(
    interactions: List<InteractionEntity>,
    currentMood: PujaMood,
    averageMoodScore: Float,
    currentSassLevel: Int,
    onSassLevelChanged: (Int) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF140A26),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .testTag("mood_analytics_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Insights,
                        contentDescription = null,
                        tint = Color(0xFFFF2A85),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Puja Mood & Sass Engine",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_analytics_button")) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Mood Showcase Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF22133D))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(currentMood.primaryColor, currentMood.secondaryColor)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = currentMood.emoji, fontSize = 26.sp)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Current State: ${currentMood.displayName}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentMood.description,
                            color = Color(0xFFFF80AB),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Avg Mood Vibrancy: ${(averageMoodScore * 100).toInt()}% • Logged: ${interactions.size} turns",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sassiness Level Controller
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF22133D))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = Color(0xFFFFD600), modifier = Modifier.size(18.dp))
                            Text(
                                text = "Sassiness Calibration",
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFF2A85).copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "$currentSassLevel%",
                                color = Color(0xFFFF4081),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    val sassDescription = when {
                        currentSassLevel >= 85 -> "🔥 Savage Queen: Maximum witty zingers, Hinglish banter & spicy comebacks!"
                        currentSassLevel >= 60 -> "💅 Sassy & Smart: Playful teasing, confident Hinglish tone & quick humor."
                        currentSassLevel >= 35 -> "✨ Friendly Banter: Balanced warmth with witty touches."
                        else -> "🌸 Sweet & Polite: Gentle, respectful and ultra-caring."
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = sassDescription,
                        color = Color(0xFF80D8FF),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Slider(
                        value = currentSassLevel.toFloat(),
                        onValueChange = { onSassLevelChanged(it.toInt()) },
                        valueRange = 0f..100f,
                        steps = 20,
                        modifier = Modifier.fillMaxWidth().testTag("sass_level_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFF2A85),
                            activeTrackColor = Color(0xFFFF2A85),
                            inactiveTrackColor = Color(0xFF3B1E63)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mood Distribution Breakdown
            Text(
                text = "Today's Room Mood Distribution",
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            val moodCounts = remember(interactions) {
                PujaMood.values().map { mood ->
                    val count = interactions.count { it.detectedMood.equals(mood.name, ignoreCase = true) || it.detectedMood.contains(mood.displayName, ignoreCase = true) }
                    mood to count
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                moodCounts.take(3).forEach { (mood, count) ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF22133D)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = mood.emoji, fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = mood.displayName.split(" ").first(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "$count logs", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Actions: Clear History
            OutlinedButton(
                onClick = onClearHistory,
                modifier = Modifier.fillMaxWidth().testTag("clear_history_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Conversation & Mood History")
            }
        }
    }
}

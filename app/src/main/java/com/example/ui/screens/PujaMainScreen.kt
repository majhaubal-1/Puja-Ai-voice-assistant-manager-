package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AssistantState
import com.example.data.model.PujaAction
import com.example.data.model.PujaMood
import com.example.ui.components.ChatBubble
import com.example.ui.components.GlowingMoodOrb
import com.example.ui.components.MoodAnalyticsBottomSheet
import com.example.ui.components.SoundWaveVisualizer
import com.example.ui.components.WakeWordBanner
import com.example.ui.viewmodel.PujaViewModel

@Composable
fun PujaMainScreen(
    viewModel: PujaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val assistantState by viewModel.assistantState.collectAsStateWithLifecycle()
    val currentMood by viewModel.currentMood.collectAsStateWithLifecycle()
    val sassLevel by viewModel.sassLevel.collectAsStateWithLifecycle()
    val isHandsFreeActive by viewModel.isHandsFreeActive.collectAsStateWithLifecycle()
    val audioAmplitude by viewModel.audioAmplitude.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.currentPartialTranscript.collectAsStateWithLifecycle()
    val interactions by viewModel.interactions.collectAsStateWithLifecycle()
    val averageMoodScore by viewModel.averageMoodScore.collectAsStateWithLifecycle()

    var showAnalyticsSheet by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Request Audio & Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (recordGranted) {
            Toast.makeText(context, "Microphone enabled! You can now use voice and 'Hey Puja'.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice commands.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        val hasRecordPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val hasCameraPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (!hasRecordPerm || !hasCameraPerm) {
            val permissionsToRequest = mutableListOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.VIBRATE
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // Auto-scroll to top when new interaction arrives
    LaunchedEffect(interactions.size) {
        if (interactions.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    val quickPrompts = listOf(
        "📸 Take a selfie",
        "🎥 Record video",
        "👁️ Analyze camera view",
        "Open WhatsApp",
        "Call Rahul on WhatsApp",
        "Send SMS to Mom: I'm reaching home soon",
        "🚨 Emergency SOS!",
        "Who is the boss here?",
        "Tell me a sassy joke"
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0517))
            .imePadding()
            .testTag("puja_main_screen"),
        containerColor = Color(0xFF0C0517)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // --- TOP HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(currentMood.primaryColor, currentMood.secondaryColor)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "P", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Puja",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFF2A85).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "AI VOICE",
                                    color = Color(0xFFFF4081),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        val statusText = when (assistantState) {
                            is AssistantState.ListeningSpeech -> "Listening to you..."
                            is AssistantState.ListeningWakeWord -> "Listening for 'Hey Puja'..."
                            is AssistantState.Thinking -> "Thinking..."
                            is AssistantState.Speaking -> "Speaking..."
                            is AssistantState.Error -> "Attention needed"
                            else -> if (isHandsFreeActive) "Hands-Free Active" else "Ready"
                        }

                        Text(
                            text = statusText,
                            color = when (assistantState) {
                                is AssistantState.ListeningSpeech -> Color(0xFF00E5FF)
                                is AssistantState.Speaking -> currentMood.accentGlow
                                is AssistantState.Thinking -> Color(0xFFB388FF)
                                else -> Color.White.copy(alpha = 0.6f)
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Header Badges & Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mood & Sass Pill (Tap opens Room analytics)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showAnalyticsSheet = true }
                            .testTag("mood_badge_button"),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1E1038),
                        border = androidx.compose.foundation.BorderStroke(1.dp, currentMood.primaryColor.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = currentMood.emoji, fontSize = 13.sp)
                            Text(
                                text = "${currentMood.displayName.split(" ").first()} ($sassLevel%)",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Emergency SOS Shortcut Button
                    IconButton(
                        onClick = { viewModel.processUserQuery("Emergency SOS! Help me!") },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF1744).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFFFF1744).copy(alpha = 0.6f), CircleShape)
                            .testTag("top_emergency_sos_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Trigger Emergency SOS",
                            tint = Color(0xFFFF1744),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // --- HERO GLOWING MOOD ORB & VISUALIZER ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    GlowingMoodOrb(
                        mood = currentMood,
                        assistantState = assistantState,
                        audioAmplitude = audioAmplitude,
                        onClick = {
                            if (assistantState is AssistantState.ListeningSpeech) {
                                viewModel.stopManualVoiceInput()
                            } else {
                                viewModel.startManualVoiceInput()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    SoundWaveVisualizer(
                        isActive = assistantState is AssistantState.ListeningSpeech || assistantState is AssistantState.Speaking,
                        mood = currentMood,
                        audioLevel = audioAmplitude,
                        barCount = 18,
                        maxHeight = 24.dp
                    )

                    if (partialTranscript.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1F0D3D),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Text(
                                text = "“$partialTranscript”",
                                color = Color(0xFF80D8FF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // --- HANDS-FREE "HEY PUJA" TOGGLE BANNER ---
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                WakeWordBanner(
                    isHandsFreeActive = isHandsFreeActive,
                    isListening = assistantState is AssistantState.ListeningWakeWord || assistantState is AssistantState.ListeningSpeech,
                    mood = currentMood,
                    onToggleHandsFree = { enable -> viewModel.toggleHandsFree(enable) }
                )
            }

            // --- QUICK ACTION SUGGESTION CHIPS ---
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickPrompts) { prompt ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { viewModel.processUserQuery(prompt) }
                            .testTag("quick_chip_${prompt.take(10).replace(" ", "_")}"),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E1338),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (prompt.contains("SOS", true)) Color(0xFFFF1744).copy(alpha = 0.6f) else Color(0xFF4A2574)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (prompt.contains("SOS", true)) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(12.dp))
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = currentMood.accentGlow, modifier = Modifier.size(12.dp))
                            }
                            Text(
                                text = prompt,
                                color = if (prompt.contains("SOS", true)) Color(0xFFFF5252) else Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // --- CONVERSATION STREAM & ACTION LOG ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (interactions.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "👋 Namaste! I'm Puja",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Say \"Hey Puja\" or tap the glowing orb to launch apps, send WhatsApp messages, make calls, or emergency SOS.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(interactions, key = { it.id }) { interaction ->
                            ChatBubble(
                                interaction = interaction,
                                onReplaySpeech = { text -> viewModel.replaySpeech(text) },
                                onExecuteAction = { action -> viewModel.executeAction(action) }
                            )
                        }
                    }
                }
            }

            // --- BOTTOM INPUT & CONTROLS DOCK ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF130924),
                tonalElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E1952))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mood & Insights Toggle Button
                    IconButton(
                        onClick = { showAnalyticsSheet = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22113D))
                            .testTag("open_analytics_dock_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = "Mood & Sass Analytics",
                            tint = Color(0xFFFF2A85),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Text Input Field
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("text_input_field"),
                        placeholder = {
                            Text(
                                text = "Ask Puja or type command...",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = currentMood.primaryColor,
                            unfocusedBorderColor = Color(0xFF381F5E),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF1A0D30),
                            unfocusedContainerColor = Color(0xFF1A0D30)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (textInput.isNotBlank()) {
                                    viewModel.processUserQuery(textInput)
                                    textInput = ""
                                    focusManager.clearFocus()
                                }
                            }
                        ),
                        trailingIcon = {
                            if (textInput.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        viewModel.processUserQuery(textInput)
                                        textInput = ""
                                        focusManager.clearFocus()
                                    },
                                    modifier = Modifier.testTag("send_text_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send",
                                        tint = currentMood.accentGlow
                                    )
                                }
                            }
                        }
                    )

                    // Glow Floating Mic Button
                    FloatingActionButton(
                        onClick = {
                            if (assistantState is AssistantState.ListeningSpeech) {
                                viewModel.stopManualVoiceInput()
                            } else {
                                viewModel.startManualVoiceInput()
                            }
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .testTag("main_mic_fab"),
                        shape = CircleShape,
                        containerColor = if (assistantState is AssistantState.ListeningSpeech) Color(0xFFFF1744) else currentMood.primaryColor,
                        contentColor = Color.White
                    ) {
                        Icon(
                            imageVector = if (assistantState is AssistantState.ListeningSpeech) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }

    // Bottom Sheet for Mood & Sass Analytics
    if (showAnalyticsSheet) {
        MoodAnalyticsBottomSheet(
            interactions = interactions,
            currentMood = currentMood,
            averageMoodScore = averageMoodScore,
            currentSassLevel = sassLevel,
            onSassLevelChanged = { level -> viewModel.setSassLevel(level) },
            onClearHistory = { viewModel.clearHistory() },
            onDismiss = { showAnalyticsSheet = false }
        )
    }
}

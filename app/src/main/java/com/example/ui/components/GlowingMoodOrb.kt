package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantState
import com.example.data.model.PujaMood
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GlowingMoodOrb(
    mood: PujaMood,
    assistantState: AssistantState,
    audioAmplitude: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbPulseTransition")

    // Dynamic pulse duration based on mood
    val pulseDuration = when (assistantState) {
        is AssistantState.ListeningSpeech -> 600
        is AssistantState.Thinking -> 450
        is AssistantState.Speaking -> 700
        else -> mood.pulseDurationMillis
    }

    val basePulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = pulseDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (assistantState is AssistantState.Thinking) 2500 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    val waveRing1 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = pulseDuration * 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Ring1"
    )

    val waveRing2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (pulseDuration * 2.5).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Ring2"
    )

    // Animated colors
    val animatedPrimaryColor by animateColorAsState(
        targetValue = when (assistantState) {
            is AssistantState.Error -> Color(0xFFFF1744)
            is AssistantState.ListeningSpeech -> Color(0xFF00E5FF)
            is AssistantState.Thinking -> Color(0xFF7C4DFF)
            else -> mood.primaryColor
        },
        animationSpec = tween(500),
        label = "PrimaryColor"
    )

    val animatedSecondaryColor by animateColorAsState(
        targetValue = when (assistantState) {
            is AssistantState.Error -> Color(0xFFFF5252)
            is AssistantState.ListeningSpeech -> Color(0xFF1DE9B6)
            is AssistantState.Thinking -> Color(0xFF651FFF)
            else -> mood.secondaryColor
        },
        animationSpec = tween(500),
        label = "SecondaryColor"
    )

    val animatedGlowColor by animateColorAsState(
        targetValue = when (assistantState) {
            is AssistantState.Error -> Color(0xFFFF1744).copy(alpha = 0.6f)
            is AssistantState.ListeningSpeech -> Color(0xFF00E5FF).copy(alpha = 0.6f)
            else -> mood.accentGlow.copy(alpha = 0.5f)
        },
        animationSpec = tween(500),
        label = "GlowColor"
    )

    val effectiveAudioScale by animateFloatAsState(
        targetValue = 1f + audioAmplitude * 0.4f,
        animationSpec = tween(80),
        label = "AudioScale"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(240.dp)
            .testTag("glowing_mood_orb")
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(240.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.width * 0.28f

            // Outer expanding ripple rings when active or speaking
            val ringAlpha1 = ((1.45f - waveRing1) / 0.55f).coerceIn(0f, 0.45f)
            drawCircle(
                color = animatedGlowColor.copy(alpha = ringAlpha1),
                radius = baseRadius * waveRing1 * effectiveAudioScale,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            val ringAlpha2 = ((1.7f - waveRing2) / 0.7f).coerceIn(0f, 0.3f)
            drawCircle(
                color = animatedPrimaryColor.copy(alpha = ringAlpha2),
                radius = baseRadius * waveRing2 * effectiveAudioScale,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Outer ambient glow blur layer
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedGlowColor.copy(alpha = 0.65f),
                        animatedSecondaryColor.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.6f * basePulse * effectiveAudioScale
                ),
                radius = baseRadius * 1.6f * basePulse * effectiveAudioScale,
                center = center
            )

            // Inner core gradient orb
            val dynamicRadius = baseRadius * basePulse * effectiveAudioScale
            val radAngle = Math.toRadians(rotationAngle.toDouble())
            val focalOffset = Offset(
                center.x + (dynamicRadius * 0.35f * cos(radAngle)).toFloat(),
                center.y + (dynamicRadius * 0.35f * sin(radAngle)).toFloat()
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        animatedPrimaryColor,
                        animatedSecondaryColor,
                        Color(0xFF0F051D)
                    ),
                    center = focalOffset,
                    radius = dynamicRadius
                ),
                radius = dynamicRadius,
                center = center
            )

            // Orbiting particle sparks
            for (i in 0 until 6) {
                val sparkAngle = Math.toRadians((rotationAngle + i * 60.0))
                val sparkDist = dynamicRadius * (0.95f + 0.15f * sin((rotationAngle * 2 + i * 45).toDouble()).toFloat())
                val sparkPos = Offset(
                    center.x + (sparkDist * cos(sparkAngle)).toFloat(),
                    center.y + (sparkDist * sin(sparkAngle)).toFloat()
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = 2.5.dp.toPx(),
                    center = sparkPos
                )
            }

            // Core highlight reflection
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.7f), Color.Transparent),
                    center = Offset(center.x - dynamicRadius * 0.25f, center.y - dynamicRadius * 0.25f),
                    radius = dynamicRadius * 0.45f
                ),
                radius = dynamicRadius * 0.45f,
                center = Offset(center.x - dynamicRadius * 0.25f, center.y - dynamicRadius * 0.25f)
            )
        }
    }
}

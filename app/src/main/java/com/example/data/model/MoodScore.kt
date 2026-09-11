package com.example.data.model

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class PujaMood(
    val displayName: String,
    val emoji: String,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentGlow: Color,
    val pulseDurationMillis: Int,
    val defaultScore: Float
) {
    SASSY(
        displayName = "Sassy & Witty",
        emoji = "💅",
        description = "Teasing, sharp & playfully sarcastic",
        primaryColor = Color(0xFFFF2A85), // Neon Fuchsia / Pink
        secondaryColor = Color(0xFF7928CA), // Deep Purple
        accentGlow = Color(0xFFFF0080),
        pulseDurationMillis = 900,
        defaultScore = 0.85f
    ),
    HAPPY(
        displayName = "Happy & Energetic",
        emoji = "✨",
        description = "Bubbly, enthusiastic & cheerful",
        primaryColor = Color(0xFF00F2FE), // Electric Cyan
        secondaryColor = Color(0xFF4FACFE), // Bright Azure
        accentGlow = Color(0xFF00E5FF),
        pulseDurationMillis = 1100,
        defaultScore = 0.75f
    ),
    CARING(
        displayName = "Caring & Sweet",
        emoji = "🌸",
        description = "Warm, attentive & supportive",
        primaryColor = Color(0xFFFF758C), // Rose Blush
        secondaryColor = Color(0xFFFF7EB3), // Soft Blossom
        accentGlow = Color(0xFFFF80AB),
        pulseDurationMillis = 1400,
        defaultScore = 0.60f
    ),
    THOUGHTFUL(
        displayName = "Focused & Analytical",
        emoji = "⚡",
        description = "Smart, precise & direct",
        primaryColor = Color(0xFF6A11CB), // Royal Violet
        secondaryColor = Color(0xFF2575FC), // Cobalt Neon
        accentGlow = Color(0xFF7C4DFF),
        pulseDurationMillis = 1600,
        defaultScore = 0.50f
    ),
    PROTECTIVE(
        displayName = "Emergency / Protective",
        emoji = "🚨",
        description = "Immediate action & safety shield",
        primaryColor = Color(0xFFFF1744), // Crimson Red
        secondaryColor = Color(0xFFFF5252), // Electric Coral
        accentGlow = Color(0xFFFF1744),
        pulseDurationMillis = 500,
        defaultScore = 0.95f
    ),
    CALM(
        displayName = "Calm & Zen",
        emoji = "🍃",
        description = "Relaxed, patient & peaceful",
        primaryColor = Color(0xFF0BA360), // Mint Jade
        secondaryColor = Color(0xFF3CBA92), // Emerald
        accentGlow = Color(0xFF00E676),
        pulseDurationMillis = 1800,
        defaultScore = 0.40f
    );

    fun getBrush(): Brush {
        return Brush.radialGradient(
            colors = listOf(primaryColor, secondaryColor, Color.Transparent)
        )
    }

    companion object {
        fun fromScore(score: Float, isEmergency: Boolean = false): PujaMood {
            if (isEmergency) return PROTECTIVE
            return when {
                score >= 0.80f -> SASSY
                score >= 0.65f -> HAPPY
                score >= 0.50f -> CARING
                score >= 0.35f -> THOUGHTFUL
                else -> CALM
            }
        }

        fun fromString(moodStr: String?): PujaMood {
            val lower = moodStr?.lowercase() ?: return SASSY
            return when {
                lower.contains("sos") || lower.contains("danger") || lower.contains("emergency") || lower.contains("protect") -> PROTECTIVE
                lower.contains("sassy") || lower.contains("sarcastic") || lower.contains("witty") -> SASSY
                lower.contains("happy") || lower.contains("energetic") || lower.contains("cheerful") -> HAPPY
                lower.contains("caring") || lower.contains("sweet") || lower.contains("warm") -> CARING
                lower.contains("thoughtful") || lower.contains("focused") || lower.contains("smart") -> THOUGHTFUL
                lower.contains("calm") || lower.contains("zen") || lower.contains("peaceful") -> CALM
                else -> SASSY
            }
        }
    }
}

package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "interactions")
data class InteractionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val userQuery: String,
    val assistantResponse: String,
    val spokenText: String,
    val detectedMood: String, // e.g., "Sassy", "Happy", "Caring", "Witty", "Protective", "Calm", "Alert"
    val moodScore: Float, // 0.0 to 1.0
    val sassLevel: Int, // 0 to 100
    val actionType: String? = null, // "open_app", "send_message", "make_call", "trigger_sos", null
    val actionJson: String? = null,
    val actionExecuted: Boolean = false
)

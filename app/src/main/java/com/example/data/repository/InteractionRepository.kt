package com.example.data.repository

import com.example.data.local.InteractionDao
import com.example.data.local.InteractionEntity
import com.example.data.model.PujaMood
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InteractionRepository(private val dao: InteractionDao) {

    val allInteractions: Flow<List<InteractionEntity>> = dao.getAllInteractions()

    val recentInteractions: Flow<List<InteractionEntity>> = dao.getRecentInteractions(30)

    val averageMoodScore: Flow<Float> = dao.getAverageMoodScore().map { it ?: 0.75f }

    val interactionCount: Flow<Int> = dao.getInteractionCount()

    suspend fun logInteraction(
        userQuery: String,
        assistantResponse: String,
        spokenText: String,
        detectedMood: String,
        moodScore: Float,
        sassLevel: Int,
        actionType: String? = null,
        actionJson: String? = null,
        actionExecuted: Boolean = false
    ): Long {
        val entity = InteractionEntity(
            userQuery = userQuery,
            assistantResponse = assistantResponse,
            spokenText = spokenText,
            detectedMood = detectedMood,
            moodScore = moodScore,
            sassLevel = sassLevel,
            actionType = actionType,
            actionJson = actionJson,
            actionExecuted = actionExecuted
        )
        return dao.insertInteraction(entity)
    }

    suspend fun markActionExecuted(id: Long) {
        // Can be updated if needed
    }

    suspend fun clearHistory() {
        dao.clearAll()
    }
}

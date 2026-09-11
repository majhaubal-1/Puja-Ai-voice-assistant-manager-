package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InteractionDao {
    @Query("SELECT * FROM interactions ORDER BY timestamp DESC")
    fun getAllInteractions(): Flow<List<InteractionEntity>>

    @Query("SELECT * FROM interactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentInteractions(limit: Int): Flow<List<InteractionEntity>>

    @Query("SELECT AVG(moodScore) FROM interactions")
    fun getAverageMoodScore(): Flow<Float?>

    @Query("SELECT AVG(sassLevel) FROM interactions")
    fun getAverageSassLevel(): Flow<Float?>

    @Query("SELECT COUNT(*) FROM interactions")
    fun getInteractionCount(): Flow<Int>

    @Query("SELECT * FROM interactions WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getInteractionsSince(sinceTimestamp: Long): Flow<List<InteractionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteraction(interaction: InteractionEntity): Long

    @Update
    suspend fun updateInteraction(interaction: InteractionEntity)

    @Query("DELETE FROM interactions WHERE id = :id")
    suspend fun deleteInteractionById(id: Long)

    @Query("DELETE FROM interactions")
    suspend fun clearAll()
}

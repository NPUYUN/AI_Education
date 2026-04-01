package com.example.common.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.common.database.models.ReviewHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewHistoryDao {
    @Query("SELECT * FROM review_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ReviewHistoryEntity>>

    @Query("SELECT * FROM review_history WHERE type = :type ORDER BY timestamp DESC")
    fun getHistoryByType(type: String): Flow<List<ReviewHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ReviewHistoryEntity)

    @Delete
    suspend fun deleteHistory(history: ReviewHistoryEntity)

    @Query("DELETE FROM review_history")
    suspend fun deleteAllHistory()
}
package com.example.common.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.common.database.models.SummaryHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryHistoryDao {
    @Query("SELECT * FROM summary_history WHERE type = :type ORDER BY timestamp DESC")
    fun getHistoryByType(type: String): Flow<List<SummaryHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: SummaryHistoryEntity)

    @Delete
    suspend fun deleteHistory(history: SummaryHistoryEntity)
}
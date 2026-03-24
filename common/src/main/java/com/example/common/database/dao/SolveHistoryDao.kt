package com.example.common.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.common.database.models.SolveHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SolveHistoryDao {
    @Query("SELECT * FROM solve_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<SolveHistoryEntity>>

    @Query("SELECT * FROM solve_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<SolveHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SolveHistoryEntity): Long

    @Update
    suspend fun update(record: SolveHistoryEntity)

    @Query("UPDATE solve_history SET isInErrorBook = 1 WHERE id = :id")
    suspend fun markInErrorBook(id: Long)

    @Query("DELETE FROM solve_history")
    suspend fun clearAll()
}

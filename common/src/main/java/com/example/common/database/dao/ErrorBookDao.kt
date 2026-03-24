package com.example.common.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.common.database.models.ErrorBookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ErrorBookDao {
    @Query("SELECT * FROM error_book ORDER BY timestamp DESC")
    fun getAllErrorRecords(): Flow<List<ErrorBookEntity>>

    @Query("SELECT * FROM error_book WHERE subject = :subject ORDER BY timestamp DESC")
    fun getErrorRecordsBySubject(subject: String): Flow<List<ErrorBookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertErrorRecord(record: ErrorBookEntity)

    @Delete
    suspend fun deleteErrorRecord(record: ErrorBookEntity)
    
    @Query("DELETE FROM error_book")
    suspend fun deleteAllErrorRecords()
}

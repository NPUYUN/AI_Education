package com.example.common.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM student_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<StudentEntity>>

    @Insert
    suspend fun insertRecord(record: StudentEntity)
}

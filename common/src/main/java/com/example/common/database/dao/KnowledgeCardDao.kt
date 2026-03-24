package com.example.common.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.common.database.models.KnowledgeCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeCardDao {
    @Query("SELECT * FROM knowledge_cards ORDER BY timestamp DESC")
    fun getAllCards(): Flow<List<KnowledgeCardEntity>>

    @Query("SELECT * FROM knowledge_cards WHERE tags LIKE '%' || :tag || '%' ORDER BY timestamp DESC")
    fun getCardsByTag(tag: String): Flow<List<KnowledgeCardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: KnowledgeCardEntity)

    @Update
    suspend fun updateCard(card: KnowledgeCardEntity)

    @Delete
    suspend fun deleteCard(card: KnowledgeCardEntity)
}
package com.example.ai_tutor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.ai_tutor.data.local.entity.ChatSessionEntity
import com.example.ai_tutor.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getSessions(userId: String): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessages(sessionId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE chat_sessions SET lastMessage = :lastMessage, timestamp = :timestamp WHERE id = :sessionId")
    suspend fun updateSessionPreview(sessionId: String, lastMessage: String, timestamp: Long)

    @Query("UPDATE chat_sessions SET title = :title WHERE id = :sessionId")
    suspend fun updateSessionTitle(sessionId: String, title: String)

    @Transaction
    suspend fun addMessageAndUpdateSession(message: MessageEntity) {
        insertMessage(message)
        updateSessionPreview(message.sessionId, message.content, message.timestamp)
    }
}

package com.example.ai_tutor.learning_record.services

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.ai_tutor.learning_record.models.ChatSessionEntity
import com.example.ai_tutor.learning_record.models.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ChatDao {
    @Query("SELECT * FROM chat_sessions WHERE userId = :userId ORDER BY timestamp DESC")
    abstract fun getSessions(userId: String): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    abstract fun getMessages(sessionId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSession(session: ChatSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE chat_sessions SET lastMessage = :lastMessage, timestamp = :timestamp WHERE id = :sessionId")
    abstract suspend fun updateSessionPreview(sessionId: String, lastMessage: String, timestamp: Long)

    @Query("UPDATE chat_sessions SET title = :title WHERE id = :sessionId")
    abstract suspend fun updateSessionTitle(sessionId: String, title: String)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    abstract suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    abstract suspend fun deleteMessages(sessionId: String)

    @Transaction
    open suspend fun deleteSessionAndMessages(sessionId: String) {
        deleteMessages(sessionId)
        deleteSession(sessionId)
    }
}

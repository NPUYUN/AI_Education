package com.example.ai_tutor.data

import com.example.common.database.ChatDao
import com.example.common.database.ChatMessageEntity
import com.example.common.database.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {

    fun getAllSessions(): Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()

    suspend fun createSession(title: String): Long {
        return chatDao.insertSession(ChatSessionEntity(title = title))
    }
    
    suspend fun updateSessionTitle(sessionId: Long, title: String) {
        chatDao.updateSessionTitle(sessionId, title)
    }

    suspend fun deleteSession(sessionId: Long) {
        chatDao.deleteSession(sessionId)
    }

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>> = chatDao.getMessagesForSession(sessionId)

    suspend fun addMessage(sessionId: Long, role: String, content: String, imagePath: String? = null) {
        chatDao.insertMessage(
            ChatMessageEntity(
                sessionId = sessionId,
                role = role,
                content = content,
                imagePath = imagePath
            )
        )
    }
}

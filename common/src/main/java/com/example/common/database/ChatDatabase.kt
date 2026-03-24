package com.example.common.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.common.database.dao.ChatDao
import com.example.common.database.dao.KnowledgeCardDao
import com.example.common.database.models.ChatSessionEntity
import com.example.common.database.models.KnowledgeCardEntity
import com.example.common.database.models.MessageEntity
import com.example.common.database.models.ErrorBookEntity
import com.example.common.database.models.SolveHistoryEntity
import com.example.common.database.dao.ErrorBookDao
import com.example.common.database.dao.SolveHistoryDao

@Database(
    entities = [ChatSessionEntity::class, MessageEntity::class, KnowledgeCardEntity::class, ErrorBookEntity::class, SolveHistoryEntity::class],
    version = 5,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun knowledgeCardDao(): KnowledgeCardDao
    abstract fun errorBookDao(): ErrorBookDao
    abstract fun solveHistoryDao(): SolveHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        fun getDatabase(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "ai_tutor_db" // Keeping the old name so existing data is preserved
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

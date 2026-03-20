package com.example.ai_tutor.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.ai_tutor.data.local.dao.ChatDao
import com.example.ai_tutor.data.local.entity.ChatSessionEntity
import com.example.ai_tutor.data.local.entity.MessageEntity

@Database(
    entities = [ChatSessionEntity::class, MessageEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AiTutorDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AiTutorDatabase? = null

        fun getDatabase(context: Context): AiTutorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AiTutorDatabase::class.java,
                    "ai_tutor_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

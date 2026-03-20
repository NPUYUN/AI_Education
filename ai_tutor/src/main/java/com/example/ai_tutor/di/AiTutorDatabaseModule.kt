package com.example.ai_tutor.di

import android.content.Context
import androidx.room.Room
import com.example.ai_tutor.data.local.AiTutorDatabase
import com.example.ai_tutor.data.local.dao.ChatDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiTutorDatabaseModule {

    @Provides
    @Singleton
    fun provideAiTutorDatabase(
        @ApplicationContext context: Context
    ): AiTutorDatabase {
        return Room.databaseBuilder(
            context,
            AiTutorDatabase::class.java,
            "ai_tutor_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideChatDao(database: AiTutorDatabase): ChatDao {
        return database.chatDao()
    }
}

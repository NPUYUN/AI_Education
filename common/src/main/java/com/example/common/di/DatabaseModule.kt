package com.example.common.di

import android.content.Context
import androidx.room.Room
import com.example.common.data.local.auth.AuthDatabase
import com.example.common.data.local.auth.UserDao
import com.example.common.database.ChatDatabase
import com.example.common.database.dao.ChatDao
import com.example.common.database.dao.KnowledgeCardDao
import com.example.common.database.dao.ErrorBookDao
import com.example.common.database.PreferencesManager
import com.example.common.manager.VoskVoiceManager
import com.example.common.dispatchers.DispatcherProvider
import com.example.common.dispatchers.DefaultDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommonModule {

    @Provides
    @Singleton
    fun provideAuthDatabase(
        @ApplicationContext context: Context
    ): AuthDatabase {
        return Room.databaseBuilder(
            context,
            AuthDatabase::class.java,
            "auth_db"
        )
        .fallbackToDestructiveMigration(true)
         .build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AuthDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideChatDatabase(
        @ApplicationContext context: Context
    ): ChatDatabase {
        return ChatDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideChatDao(database: ChatDatabase): ChatDao {
        return database.chatDao()
    }

    @Provides
    @Singleton
    fun provideKnowledgeCardDao(database: ChatDatabase): KnowledgeCardDao {
        return database.knowledgeCardDao()
    }

    @Provides
    @Singleton
    fun provideErrorBookDao(database: ChatDatabase): ErrorBookDao {
        return database.errorBookDao()
    }

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideVoskVoiceManager(
        @ApplicationContext context: Context,
        voskModelManager: com.example.common.manager.VoskModelManager
    ): VoskVoiceManager {
        return VoskVoiceManager(context, voskModelManager)
    }

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider {
        return DefaultDispatcherProvider()
    }
}

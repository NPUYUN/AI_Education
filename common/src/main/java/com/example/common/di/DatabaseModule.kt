package com.example.common.di

import android.content.Context
import androidx.room.Room
import com.example.common.data.local.auth.AuthDatabase
import com.example.common.data.local.auth.UserDao
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
        ).build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AuthDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideVoskVoiceManager(@ApplicationContext context: Context): VoskVoiceManager {
        return VoskVoiceManager(context)
    }

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider {
        return DefaultDispatcherProvider()
    }
}

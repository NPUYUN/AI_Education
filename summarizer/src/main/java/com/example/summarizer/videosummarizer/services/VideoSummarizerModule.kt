package com.example.summarizer.videosummarizer.services

import android.content.Context
import com.example.common.dispatchers.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VideoSummarizerModule {
    @Provides
    @Singleton
    fun provideVideoDownloader(
        @ApplicationContext context: Context,
        dispatcherProvider: DispatcherProvider,
    ): VideoDownloader {
        return VideoDownloader(context, dispatcherProvider)
    }

    @Provides
    @Singleton
    fun provideSherpaAsrManager(
        @ApplicationContext context: Context,
        dispatcherProvider: DispatcherProvider,
    ): SherpaAsrManager {
        return SherpaAsrManager(context, dispatcherProvider)
    }

    @Provides
    @Singleton
    fun provideModelDownloader(
        @ApplicationContext context: Context,
        dispatcherProvider: DispatcherProvider,
    ): ModelDownloader {
        return ModelDownloader(context, dispatcherProvider)
    }
}

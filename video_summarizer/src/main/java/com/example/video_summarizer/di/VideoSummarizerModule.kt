package com.example.video_summarizer.di

import android.content.Context
import com.example.common.dispatchers.DispatcherProvider
import com.example.video_summarizer.data.downloader.ModelDownloader
import com.example.video_summarizer.data.downloader.VideoDownloader
import com.example.video_summarizer.data.asr.SherpaAsrManager
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
        dispatcherProvider: DispatcherProvider
    ): VideoDownloader {
        return VideoDownloader(context, dispatcherProvider)
    }

    @Provides
    @Singleton
    fun provideSherpaAsrManager(
        @ApplicationContext context: Context,
        dispatcherProvider: DispatcherProvider
    ): SherpaAsrManager {
        return SherpaAsrManager(context, dispatcherProvider)
    }

    @Provides
    @Singleton
    fun provideModelDownloader(
        @ApplicationContext context: Context,
        dispatcherProvider: DispatcherProvider
    ): ModelDownloader {
        return ModelDownloader(context, dispatcherProvider)
    }
}

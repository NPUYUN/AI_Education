package com.example.common.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.common.config.AppConstants

object RoomDatabaseBuilder {
    inline fun <reified T : RoomDatabase> build(
        context: Context,
        databaseName: String = AppConstants.DATABASE_NAME,
    ): T {
        return Room.databaseBuilder(
            context.applicationContext,
            T::class.java,
            databaseName,
        )
            .fallbackToDestructiveMigration(true) // For development simplicity
            .build()
    }
}

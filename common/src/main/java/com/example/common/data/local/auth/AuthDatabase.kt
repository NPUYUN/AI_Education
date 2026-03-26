package com.example.common.data.local.auth

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UserEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AuthDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AuthDatabase? = null

        fun getDatabase(context: Context): AuthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance =
                    Room.databaseBuilder(
                        context.applicationContext,
                        AuthDatabase::class.java,
                        "auth_db",
                    )
                        .fallbackToDestructiveMigration(true)
                        .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.project.qrcode.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.project.qrcode.data.entity.GenerateQrEntity

@Database(entities = [GenerateQrEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun generateQrDao(): GenerateQrDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

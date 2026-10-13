package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AudioRecording
import com.example.data.model.CommandMacro
import com.example.data.model.Contact
import com.example.data.model.SyncLog

@Database(
    entities = [AudioRecording::class, SyncLog::class, CommandMacro::class, Contact::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioRecordingDao(): AudioRecordingDao
    abstract fun syncLogDao(): SyncLogDao
    abstract fun commandMacroDao(): CommandMacroDao
    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "audio_sync_hub.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

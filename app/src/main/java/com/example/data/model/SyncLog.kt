package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_logs")
data class SyncLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordingId: Long?,
    val commandExecuted: String,
    val status: String, // "PENDING", "IN_PROGRESS", "SUCCESS", "FAILED"
    val progressPercent: Int,
    val bytesTransferred: String,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)

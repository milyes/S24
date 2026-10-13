package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_recordings")
data class AudioRecording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val fileName: String,
    val durationSeconds: Int,
    val fileSizeMb: Float,
    val localPath: String,
    val cloudPath: String,
    val isSynced: Boolean,
    val syncStatus: String, // "LOCAL_ONLY", "SYNCING", "CLOUD_SYNCED", "ERROR"
    val timestamp: Long = System.currentTimeMillis(),
    val sourceStream: String = "Flux WAY", // e.g., "Flux WAY", "Z-CORE Capture", "Voice Note"
    val transcription: String? = null,
    val aiSummary: String? = null,
    val waveformData: String = "15,28,45,80,60,30,90,75,40,20,65,85,95,50,30,70,80,40,20,60" // CSV amplitude list
)

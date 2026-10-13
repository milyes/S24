package com.example.data.local

import androidx.room.*
import com.example.data.model.AudioRecording
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioRecordingDao {
    @Query("SELECT * FROM audio_recordings ORDER BY timestamp DESC")
    fun getAllRecordings(): Flow<List<AudioRecording>>

    @Query("SELECT * FROM audio_recordings ORDER BY timestamp DESC")
    suspend fun getAllRecordingsDirect(): List<AudioRecording>

    @Query("SELECT * FROM audio_recordings WHERE id = :id")
    suspend fun getRecordingById(id: Long): AudioRecording?

    @Query("SELECT * FROM audio_recordings WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedRecordings(): List<AudioRecording>

    @Query("SELECT * FROM audio_recordings WHERE title LIKE '%' || :query || '%' OR fileName LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchRecordings(query: String): Flow<List<AudioRecording>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: AudioRecording): Long

    @Update
    suspend fun updateRecording(recording: AudioRecording)

    @Query("DELETE FROM audio_recordings WHERE id = :id")
    suspend fun deleteRecording(id: Long)

    @Query("SELECT COUNT(*) FROM audio_recordings")
    suspend fun getCount(): Int
}

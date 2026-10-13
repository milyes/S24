package com.example.data.local

import androidx.room.*
import com.example.data.model.SyncLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncLogDao {
    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC")
    fun getAllSyncLogs(): Flow<List<SyncLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncLog(syncLog: SyncLog): Long

    @Update
    suspend fun updateSyncLog(syncLog: SyncLog)

    @Query("SELECT COUNT(*) FROM sync_logs")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(syncLogs: List<SyncLog>)

    @Query("DELETE FROM sync_logs")
    suspend fun clearLogs()
}

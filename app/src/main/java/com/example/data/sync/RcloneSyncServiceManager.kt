package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.AudioRecording
import com.example.data.model.SyncLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min
import kotlin.math.pow

/**
 * Service Layer for automated background synchronization of local audio files to Google Drive using Rclone configuration.
 * Handles network constraint checks, efficient network usage, and exponential backoff retry mechanisms.
 */
class RcloneSyncServiceManager(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val audioDao = db.audioRecordingDao()
    private val syncLogDao = db.syncLogDao()

    private val configRepository = RcloneConfigRepository(context)
    private val networkManager = RcloneNetworkConstraintManager(context)
    private val intentHelper = TermuxRcloneSyncIntentHelper(context)
    private val googleDriveSyncService = GoogleDriveSyncService(context)

    private val _isSyncActive = MutableStateFlow(false)
    val isSyncActive: StateFlow<Boolean> = _isSyncActive.asStateFlow()

    private val _syncProgress = MutableStateFlow(0)
    val syncProgress: StateFlow<Int> = _syncProgress.asStateFlow()

    private val _currentSyncCommand = MutableStateFlow("")
    val currentSyncCommand: StateFlow<String> = _currentSyncCommand.asStateFlow()

    companion object {
        private const val TAG = "RcloneSyncServiceManager"
    }

    /**
     * Executes automated background synchronization for all unsynced local audio recordings
     * or a specific recording ID if provided.
     */
    suspend fun processAutomatedSync(targetRecordingId: Long? = null): SyncResultSummary = withContext(Dispatchers.IO) {
        val config = configRepository.getConfig()
        Log.d(TAG, "Starting automated background sync process. Remote: ${config.remoteName}:${config.remotePath}, Max retries: ${config.maxRetries}")

        // 1. Verify Network Constraints
        when (val netCheck = networkManager.checkNetworkAllowed(config)) {
            is RcloneNetworkConstraintManager.NetworkCheckResult.Rejected -> {
                Log.w(TAG, "Sync aborted due to network constraint: ${netCheck.reason}")
                logGlobalSyncError("NETWORK_REJECTED", netCheck.reason)
                return@withContext SyncResultSummary(
                    successCount = 0,
                    failureCount = 0,
                    abortedReason = netCheck.reason
                )
            }
            RcloneNetworkConstraintManager.NetworkCheckResult.Allowed -> {
                // Network check passed
            }
        }

        // 2. Fetch pending recordings from local database
        val pendingRecordings = if (targetRecordingId != null) {
            audioDao.getRecordingById(targetRecordingId)?.let { listOf(it) } ?: emptyList()
        } else {
            audioDao.getUnsyncedRecordings()
        }

        if (pendingRecordings.isEmpty()) {
            Log.d(TAG, "No audio recordings pending cloud synchronization.")
            return@withContext SyncResultSummary(successCount = 0, failureCount = 0, abortedReason = "Tous les fichiers sont déjà synchronisés.")
        }

        _isSyncActive.value = true
        var successCount = 0
        var failureCount = 0

        try {
            for ((index, recording) in pendingRecordings.withIndex()) {
                val overallProgress = ((index.toFloat() / pendingRecordings.size) * 100).toInt()
                _syncProgress.value = overallProgress

                val recordingSuccess = syncRecordingWithRetry(recording, config)
                if (recordingSuccess) {
                    successCount++
                } else {
                    failureCount++
                }
            }
            _syncProgress.value = 100
        } finally {
            _isSyncActive.value = false
            _currentSyncCommand.value = ""
        }

        return@withContext SyncResultSummary(
            successCount = successCount,
            failureCount = failureCount,
            abortedReason = null
        )
    }

    /**
     * Synchronizes an individual audio recording with automated exponential backoff retries.
     */
    private suspend fun syncRecordingWithRetry(
        recording: AudioRecording,
        config: RcloneSyncConfig
    ): Boolean {
        val rcloneCommand = config.buildCopyCommand(recording.localPath, recording.fileName)
        _currentSyncCommand.value = rcloneCommand

        // Update DB status to SYNCING
        audioDao.updateRecording(
            recording.copy(
                syncStatus = "SYNCING",
                cloudPath = "${config.remoteName}:${config.remotePath}${recording.fileName}"
            )
        )

        logSyncLog(
            recordingId = recording.id,
            command = rcloneCommand,
            status = "IN_PROGRESS",
            progress = 10,
            bytes = "0 MB / ${recording.fileSizeMb} MB",
            error = null
        )

        // Attempt dispatch via Termux Intent if available
        val termuxIntent = intentHelper.buildRcloneCopyFileIntent(
            localFilePath = recording.localPath,
            options = TermuxRcloneSyncIntentHelper.RcloneSyncOptions(
                remoteDestination = "${config.remoteName}:${config.remotePath}",
                bandwidthLimit = config.bandwidthLimit,
                maxTransfers = config.transfers,
                dryRun = config.dryRun
            )
        )
        intentHelper.dispatchTermuxIntent(termuxIntent)

        // Exponential Backoff Retry Loop
        var attempt = 1
        var isSuccess = false
        var lastError: String? = null

        while (attempt <= config.maxRetries && !isSuccess) {
            Log.d(TAG, "Sync attempt $attempt/${config.maxRetries} for '${recording.fileName}'")

            // Verify network before each retry attempt
            val netCheck = networkManager.checkNetworkAllowed(config)
            if (netCheck is RcloneNetworkConstraintManager.NetworkCheckResult.Rejected) {
                lastError = netCheck.reason
                Log.w(TAG, "Retry attempt $attempt paused due to network: ${netCheck.reason}")
                logSyncLog(
                    recordingId = recording.id,
                    command = rcloneCommand,
                    status = "RETRY_WAIT",
                    progress = 15,
                    bytes = "0 MB",
                    error = "Tentative $attempt: ${netCheck.reason}"
                )
                break
            }

            // Execute cloud transfer (Google Drive API / Rclone stream worker)
            val result = googleDriveSyncService.uploadRecordingToDrive(recording) { progress ->
                val adjusted = 20 + ((progress * 80) / 100)
                _syncProgress.value = min(100, adjusted)
            }

            result.onSuccess { syncRes ->
                isSuccess = true
                Log.d(TAG, "Successfully synced '${recording.fileName}' on attempt $attempt")

                audioDao.updateRecording(
                    recording.copy(
                        isSynced = true,
                        syncStatus = "CLOUD_SYNCED",
                        cloudPath = syncRes.cloudPath
                    )
                )

                logSyncLog(
                    recordingId = recording.id,
                    command = rcloneCommand,
                    status = "SUCCESS",
                    progress = 100,
                    bytes = "${recording.fileSizeMb} MB",
                    error = null
                )
            }.onFailure { err ->
                lastError = err.localizedMessage ?: "Unknown transfer error"
                Log.e(TAG, "Attempt $attempt failed for '${recording.fileName}': $lastError")

                if (attempt < config.maxRetries) {
                    val backoffDelay = calculateExponentialBackoff(attempt, config.initialRetryDelayMs, config.maxRetryDelayMs)
                    Log.d(TAG, "Waiting ${backoffDelay}ms before retry attempt ${attempt + 1}")

                    logSyncLog(
                        recordingId = recording.id,
                        command = rcloneCommand,
                        status = "RETRYING",
                        progress = 20,
                        bytes = "0 MB",
                        error = "Échec tentative $attempt/$config.maxRetries ($lastError). Nouvelle tentative dans ${backoffDelay / 1000}s..."
                    )

                    delay(backoffDelay)
                }
            }

            attempt++
        }

        if (!isSuccess) {
            Log.e(TAG, "All $config.maxRetries retry attempts failed for '${recording.fileName}'")
            audioDao.updateRecording(recording.copy(syncStatus = "SYNC_FAILED"))

            logSyncLog(
                recordingId = recording.id,
                command = rcloneCommand,
                status = "FAILED",
                progress = 0,
                bytes = "0 MB",
                error = "Échec définitif après ${config.maxRetries} tentatives: $lastError"
            )
        }

        return isSuccess
    }

    /**
     * Calculates exponential backoff delay in milliseconds with capping.
     * Formula: min(maxDelay, baseDelay * 2^(attempt - 1))
     */
    private fun calculateExponentialBackoff(attempt: Int, baseDelayMs: Long, maxDelayMs: Long): Long {
        val calculated = (baseDelayMs * 2.0.pow((attempt - 1).toDouble())).toLong()
        return min(maxDelayMs, calculated)
    }

    private suspend fun logSyncLog(
        recordingId: Long?,
        command: String,
        status: String,
        progress: Int,
        bytes: String,
        error: String?
    ) {
        syncLogDao.insertSyncLog(
            SyncLog(
                recordingId = recordingId,
                commandExecuted = command,
                status = status,
                progressPercent = progress,
                bytesTransferred = bytes,
                errorMessage = error
            )
        )
    }

    private suspend fun logGlobalSyncError(status: String, errorMsg: String) {
        syncLogDao.insertSyncLog(
            SyncLog(
                recordingId = null,
                commandExecuted = "rclone bg-sync",
                status = status,
                progressPercent = 0,
                bytesTransferred = "0 MB",
                errorMessage = errorMsg
            )
        )
    }

    data class SyncResultSummary(
        val successCount: Int,
        val failureCount: Int,
        val abortedReason: String?
    )
}

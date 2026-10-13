package com.example.data.sync

import com.example.data.model.AudioRecording

/**
 * Data class representing the configuration options for Rclone CLI and Google Drive synchronization.
 */
data class RcloneSyncConfig(
    val remoteName: String = "gdrive",
    val remotePath: String = "/Z-CORE/Captures/",
    val maxRetries: Int = 3,
    val initialRetryDelayMs: Long = 2000L,
    val maxRetryDelayMs: Long = 30000L,
    val bandwidthLimit: String? = "10M",
    val chunkSize: String = "8M",
    val transfers: Int = 2,
    val requireUnmeteredNetwork: Boolean = false,
    val requireWifi: Boolean = false,
    val autoSyncEnabled: Boolean = true,
    val dryRun: Boolean = false,
    val verboseLogging: Boolean = true,
    val logFilePath: String = "/data/data/com.termux/files/home/rclone_sync.log"
) {
    /**
     * Constructs a full `rclone copy` CLI command string for a single file.
     */
    fun buildCopyCommand(localFilePath: String, customFileName: String? = null): String {
        val fileName = customFileName ?: localFilePath.substringAfterLast("/")
        val normalizedRemotePath = if (remotePath.startsWith("/")) remotePath else "/$remotePath"
        val fullDestination = "$remoteName:$normalizedRemotePath$fileName"

        val sb = StringBuilder("rclone copy \"$localFilePath\" \"$fullDestination\"")
        if (verboseLogging) sb.append(" -v")
        if (dryRun) sb.append(" --dry-run")
        if (!bandwidthLimit.isNullOrBlank()) sb.append(" --bwlimit $bandwidthLimit")
        if (chunkSize.isNotBlank()) sb.append(" --drive-chunk-size $chunkSize")
        if (transfers > 0) sb.append(" --transfers $transfers")
        if (logFilePath.isNotBlank()) sb.append(" --log-file=\"$logFilePath\"")

        return sb.toString()
    }

    /**
     * Constructs a full `rclone sync` CLI command string for mirroring a local directory.
     */
    fun buildDirectorySyncCommand(localDirPath: String): String {
        val normalizedRemotePath = if (remotePath.startsWith("/")) remotePath else "/$remotePath"
        val fullDestination = "$remoteName:$normalizedRemotePath"

        val sb = StringBuilder("rclone sync \"$localDirPath\" \"$fullDestination\"")
        if (verboseLogging) sb.append(" --progress -v")
        if (dryRun) sb.append(" --dry-run")
        if (!bandwidthLimit.isNullOrBlank()) sb.append(" --bwlimit $bandwidthLimit")
        if (chunkSize.isNotBlank()) sb.append(" --drive-chunk-size $chunkSize")
        if (transfers > 0) sb.append(" --transfers $transfers")
        if (logFilePath.isNotBlank()) sb.append(" --log-file=\"$logFilePath\"")

        return sb.toString()
    }
}

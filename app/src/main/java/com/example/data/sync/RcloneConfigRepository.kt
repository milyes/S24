package com.example.data.sync

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists Rclone configuration settings using Android SharedPreferences.
 */
class RcloneConfigRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _configState = MutableStateFlow(loadConfig())
    val configState: StateFlow<RcloneSyncConfig> = _configState.asStateFlow()

    fun getConfig(): RcloneSyncConfig = _configState.value

    fun updateConfig(newConfig: RcloneSyncConfig) {
        prefs.edit().apply {
            putString(KEY_REMOTE_NAME, newConfig.remoteName)
            putString(KEY_REMOTE_PATH, newConfig.remotePath)
            putInt(KEY_MAX_RETRIES, newConfig.maxRetries)
            putLong(KEY_RETRY_DELAY, newConfig.initialRetryDelayMs)
            putLong(KEY_MAX_RETRY_DELAY, newConfig.maxRetryDelayMs)
            putString(KEY_BW_LIMIT, newConfig.bandwidthLimit ?: "")
            putString(KEY_CHUNK_SIZE, newConfig.chunkSize)
            putInt(KEY_TRANSFERS, newConfig.transfers)
            putBoolean(KEY_REQUIRE_UNMETERED, newConfig.requireUnmeteredNetwork)
            putBoolean(KEY_REQUIRE_WIFI, newConfig.requireWifi)
            putBoolean(KEY_AUTO_SYNC, newConfig.autoSyncEnabled)
            putBoolean(KEY_DRY_RUN, newConfig.dryRun)
            putBoolean(KEY_VERBOSE, newConfig.verboseLogging)
            apply()
        }
        _configState.value = newConfig
    }

    private fun loadConfig(): RcloneSyncConfig {
        val bw = prefs.getString(KEY_BW_LIMIT, "10M")
        return RcloneSyncConfig(
            remoteName = prefs.getString(KEY_REMOTE_NAME, "gdrive") ?: "gdrive",
            remotePath = prefs.getString(KEY_REMOTE_PATH, "/Z-CORE/Captures/") ?: "/Z-CORE/Captures/",
            maxRetries = prefs.getInt(KEY_MAX_RETRIES, 3),
            initialRetryDelayMs = prefs.getLong(KEY_RETRY_DELAY, 2000L),
            maxRetryDelayMs = prefs.getLong(KEY_MAX_RETRY_DELAY, 30000L),
            bandwidthLimit = if (bw.isNull_or_blank()) null else bw,
            chunkSize = prefs.getString(KEY_CHUNK_SIZE, "8M") ?: "8M",
            transfers = prefs.getInt(KEY_TRANSFERS, 2),
            requireUnmeteredNetwork = prefs.getBoolean(KEY_REQUIRE_UNMETERED, false),
            requireWifi = prefs.getBoolean(KEY_REQUIRE_WIFI, false),
            autoSyncEnabled = prefs.getBoolean(KEY_AUTO_SYNC, true),
            dryRun = prefs.getBoolean(KEY_DRY_RUN, false),
            verboseLogging = prefs.getBoolean(KEY_VERBOSE, true)
        )
    }

    private fun String?.isNull_or_blank(): Boolean {
        return this == null || this.trim().isEmpty()
    }

    companion object {
        private const val PREFS_NAME = "rclone_sync_config_prefs"
        private const val KEY_REMOTE_NAME = "remote_name"
        private const val KEY_REMOTE_PATH = "remote_path"
        private const val KEY_MAX_RETRIES = "max_retries"
        private const val KEY_RETRY_DELAY = "retry_delay"
        private const val KEY_MAX_RETRY_DELAY = "max_retry_delay"
        private const val KEY_BW_LIMIT = "bw_limit"
        private const val KEY_CHUNK_SIZE = "chunk_size"
        private const val KEY_TRANSFERS = "transfers"
        private const val KEY_REQUIRE_UNMETERED = "require_unmetered"
        private const val KEY_REQUIRE_WIFI = "require_wifi"
        private const val KEY_AUTO_SYNC = "auto_sync"
        private const val KEY_DRY_RUN = "dry_run"
        private const val KEY_VERBOSE = "verbose"
    }
}

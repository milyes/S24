package com.example.data.sync

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.example.data.model.AudioRecording
import java.io.File

/**
 * Helper class to construct and dispatch Android Intents that trigger bash scripts
 * and rclone sync jobs within the Termux sandbox environment.
 *
 * Utilizes the standard `com.termux.RUN_COMMAND` intent interface supported by Termux
 * and Termux:Tasker / Termux:API plugins.
 */
class TermuxRcloneSyncIntentHelper(private val context: Context) {

    companion object {
        private const val TAG = "TermuxSyncIntentHelper"

        // Termux Package Identifiers
        const val TERMUX_PACKAGE_NAME = "com.termux"
        const val TERMUX_API_PACKAGE_NAME = "com.termux.api"

        // Termux RUN_COMMAND Intent Action & Extras
        const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
        const val EXTRA_RUN_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
        const val EXTRA_RUN_COMMAND_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
        const val EXTRA_RUN_COMMAND_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
        const val EXTRA_RUN_COMMAND_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
        const val EXTRA_RUN_COMMAND_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"

        // Default Paths in Termux Environment
        const val DEFAULT_BASH_PATH = "/data/data/com.termux/files/usr/bin/bash"
        const val DEFAULT_RCLONE_PATH = "/data/data/com.termux/files/usr/bin/rclone"
        const val DEFAULT_WORKDIR = "/data/data/com.termux/files/home"
        const val DEFAULT_SCRIPTS_DIR = "/data/data/com.termux/files/home/scripts"
        const val DEFAULT_REMOTE_DESTINATION = "gdrive:/Z-CORE/Captures/"

        /**
         * Checks if the Termux host application is installed.
         */
        fun isTermuxInstalled(context: Context): Boolean {
            return try {
                context.packageManager.getPackageInfo(TERMUX_PACKAGE_NAME, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    /**
     * Options for customizing rclone execution parameters.
     */
    data class RcloneSyncOptions(
        val remoteDestination: String = DEFAULT_REMOTE_DESTINATION,
        val runInBackground: Boolean = true,
        val verbose: Boolean = true,
        val bandwidthLimit: String? = null, // e.g. "2M"
        val maxTransfers: Int = 4,
        val dryRun: Boolean = false,
        val logFile: String? = "$DEFAULT_WORKDIR/rclone_sync.log"
    )

    /**
     * Constructs a raw `com.termux.RUN_COMMAND` intent for running arbitrary executable script or bash command.
     */
    fun createTermuxCommandIntent(
        executablePath: String = DEFAULT_BASH_PATH,
        arguments: Array<String>,
        workDir: String = DEFAULT_WORKDIR,
        runInBackground: Boolean = true,
        sessionAction: String = "0" // 0: Fail quietly / close session when finished
    ): Intent {
        return Intent(ACTION_RUN_COMMAND).apply {
            setPackage(TERMUX_PACKAGE_NAME)
            putExtra(EXTRA_RUN_COMMAND_PATH, executablePath)
            putExtra(EXTRA_RUN_COMMAND_ARGUMENTS, arguments)
            putExtra(EXTRA_RUN_COMMAND_WORKDIR, workDir)
            putExtra(EXTRA_RUN_COMMAND_BACKGROUND, runInBackground)
            putExtra(EXTRA_RUN_COMMAND_SESSION_ACTION, sessionAction)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Constructs an intent to execute a specific Bash script located inside Termux (e.g. `rclone_auto_sync.sh`).
     *
     * @param scriptName Name or relative path of the script in Termux scripts directory (e.g., "rclone_bg_sync.sh").
     * @param scriptArgs Arguments passed to the script.
     * @param options Rclone sync options.
     */
    fun buildTermuxScriptIntent(
        scriptName: String,
        scriptArgs: List<String> = emptyList(),
        options: RcloneSyncOptions = RcloneSyncOptions()
    ): Intent {
        val scriptFullPath = if (scriptName.startsWith("/")) scriptName else "$DEFAULT_SCRIPTS_DIR/$scriptName"
        val bashArguments = mutableListOf(scriptFullPath).apply {
            addAll(scriptArgs)
        }.toTypedArray()

        Log.d(TAG, "Building Termux script intent for: $scriptFullPath with args: ${scriptArgs.joinToString(" ")}")

        return createTermuxCommandIntent(
            executablePath = DEFAULT_BASH_PATH,
            arguments = bashArguments,
            workDir = DEFAULT_WORKDIR,
            runInBackground = options.runInBackground
        )
    }

    /**
     * Constructs an Intent for an `rclone copy` background sync job for a specific file.
     */
    fun buildRcloneCopyFileIntent(
        localFilePath: String,
        options: RcloneSyncOptions = RcloneSyncOptions()
    ): Intent {
        val file = File(localFilePath)
        val remoteTarget = if (options.remoteDestination.endsWith("/")) {
            "${options.remoteDestination}${file.name}"
        } else {
            options.remoteDestination
        }

        val commandBuilder = StringBuilder("rclone copy \"$localFilePath\" \"$remoteTarget\"")

        if (options.verbose) commandBuilder.append(" -v")
        if (options.dryRun) commandBuilder.append(" --dry-run")
        options.bandwidthLimit?.let { commandBuilder.append(" --bwlimit $it") }
        options.logFile?.let { commandBuilder.append(" --log-file=\"$it\"") }

        val shellCommand = commandBuilder.toString()
        Log.d(TAG, "Constructed Rclone copy command intent: $shellCommand")

        return createTermuxCommandIntent(
            executablePath = DEFAULT_BASH_PATH,
            arguments = arrayOf("-c", shellCommand),
            workDir = DEFAULT_WORKDIR,
            runInBackground = options.runInBackground
        )
    }

    /**
     * Constructs an Intent for an `rclone sync` background job mirroring a local directory to Google Drive.
     */
    fun buildRcloneSyncDirectoryIntent(
        localDirectoryPath: String,
        options: RcloneSyncOptions = RcloneSyncOptions()
    ): Intent {
        val commandBuilder = StringBuilder("rclone sync \"$localDirectoryPath\" \"${options.remoteDestination}\"")

        commandBuilder.append(" --transfers ${options.maxTransfers}")
        if (options.verbose) commandBuilder.append(" --progress -v")
        if (options.dryRun) commandBuilder.append(" --dry-run")
        options.bandwidthLimit?.let { commandBuilder.append(" --bwlimit $it") }
        options.logFile?.let { commandBuilder.append(" --log-file=\"$it\"") }

        val shellCommand = commandBuilder.toString()
        Log.d(TAG, "Constructed Rclone sync directory intent: $shellCommand")

        return createTermuxCommandIntent(
            executablePath = DEFAULT_BASH_PATH,
            arguments = arrayOf("-c", shellCommand),
            workDir = DEFAULT_WORKDIR,
            runInBackground = options.runInBackground
        )
    }

    /**
     * Constructs an Intent to trigger a custom Termux bash sync script that handles retry logic & background notifications.
     */
    fun buildAudioRecordingSyncScriptIntent(
        recording: AudioRecording,
        options: RcloneSyncOptions = RcloneSyncOptions()
    ): Intent {
        val scriptContentCall = """
            if command -v rclone &> /dev/null; then
                rclone copy "${recording.localPath}" "${options.remoteDestination}${recording.fileName}" --verbose
            else
                echo "Rclone binary missing in Termux environment."
            fi
        """.trimIndent().replace("\n", " ")

        return createTermuxCommandIntent(
            executablePath = DEFAULT_BASH_PATH,
            arguments = arrayOf("-c", scriptContentCall),
            workDir = DEFAULT_WORKDIR,
            runInBackground = options.runInBackground
        )
    }

    /**
     * Dispatches a constructed Termux Intent via Android Service or Broadcast to trigger execution.
     *
     * @param intent The Termux Intent created via helper build methods.
     * @return Result indicating if the intent was successfully dispatched to Termux.
     */
    fun dispatchTermuxIntent(intent: Intent): Result<Unit> {
        if (!isTermuxInstalled(context)) {
            Log.w(TAG, "Termux app is not installed on this device.")
            return Result.failure(IllegalStateException("Termux (com.termux) n'est pas installé sur l'appareil."))
        }

        return try {
            context.startService(intent)
            Log.d(TAG, "Dispatched Termux Intent via startService successfully.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Failed startService, attempting sendBroadcast fallback for Termux Intent.", e)
            try {
                context.sendBroadcast(intent)
                Log.d(TAG, "Dispatched Termux Intent via sendBroadcast fallback successfully.")
                Result.success(Unit)
            } catch (broadcastEx: Exception) {
                Log.e(TAG, "Failed to deliver Termux intent via both service and broadcast.", broadcastEx)
                Result.failure(broadcastEx)
            }
        }
    }

    /**
     * Convenience method to directly dispatch an rclone sync job for an audio file to Termux.
     */
    fun dispatchRcloneSyncForRecording(
        recording: AudioRecording,
        options: RcloneSyncOptions = RcloneSyncOptions()
    ): Result<Unit> {
        val intent = buildRcloneCopyFileIntent(recording.localPath, options)
        return dispatchTermuxIntent(intent)
    }

    /**
     * Convenience method to execute a named Termux bash script for background sync.
     */
    fun dispatchTermuxSyncScript(
        scriptName: String,
        scriptArgs: List<String> = emptyList(),
        options: RcloneSyncOptions = RcloneSyncOptions()
    ): Result<Unit> {
        val intent = buildTermuxScriptIntent(scriptName, scriptArgs, options)
        return dispatchTermuxIntent(intent)
    }
}

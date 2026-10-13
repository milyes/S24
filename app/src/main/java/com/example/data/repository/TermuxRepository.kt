package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.example.data.model.CommandMacro

/**
 * Repository providing interface methods to interact with Termux and Termux:API
 * by sending shell intents and command execution broadcasts/services.
 */
class TermuxRepository(private val context: Context) {

    companion object {
        private const val TAG = "TermuxRepository"

        const val TERMUX_PACKAGE_NAME = "com.termux"
        const val TERMUX_API_PACKAGE_NAME = "com.termux.api"

        // Termux RUN_COMMAND intent specifications
        const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
        const val EXTRA_RUN_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
        const val EXTRA_RUN_COMMAND_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
        const val EXTRA_RUN_COMMAND_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
        const val EXTRA_RUN_COMMAND_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
        const val EXTRA_RUN_COMMAND_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"

        const val DEFAULT_TERMUX_BASH_PATH = "/data/data/com.termux/files/usr/bin/bash"
        const val DEFAULT_TERMUX_WORKDIR = "/data/data/com.termux/files/home"
    }

    /**
     * Checks if the main Termux package is installed on the host device.
     */
    fun isTermuxInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE_NAME, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Checks if the Termux:API companion application package is installed on the device.
     */
    fun isTermuxApiInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_API_PACKAGE_NAME, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Executes a [CommandMacro] by converting it into a Termux shell intent.
     */
    fun executeMacroIntent(
        macro: CommandMacro,
        runInBackground: Boolean = true
    ): Result<Unit> {
        Log.d(TAG, "Executing macro intent '${macro.name}' [Category: ${macro.category}]: ${macro.commandText}")
        return executeRawCommandIntent(
            commandText = macro.commandText,
            runInBackground = runInBackground
        )
    }

    /**
     * Sends a command execution intent to Termux (`com.termux.RUN_COMMAND`).
     *
     * @param commandText The shell command string to execute in Termux environment.
     * @param runInBackground True if command should execute silently in background, false for terminal window.
     * @param workDir Working directory in Termux sandbox.
     */
    fun executeRawCommandIntent(
        commandText: String,
        runInBackground: Boolean = true,
        workDir: String = DEFAULT_TERMUX_WORKDIR
    ): Result<Unit> {
        return try {
            val intent = Intent(ACTION_RUN_COMMAND).apply {
                setPackage(TERMUX_PACKAGE_NAME)
                putExtra(EXTRA_RUN_COMMAND_PATH, DEFAULT_TERMUX_BASH_PATH)
                putExtra(EXTRA_RUN_COMMAND_ARGUMENTS, arrayOf("-c", commandText))
                putExtra(EXTRA_RUN_COMMAND_WORKDIR, workDir)
                putExtra(EXTRA_RUN_COMMAND_BACKGROUND, runInBackground)
                putExtra(EXTRA_RUN_COMMAND_SESSION_ACTION, "0")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (isTermuxInstalled()) {
                context.startService(intent)
                Log.d(TAG, "Dispatched Termux RUN_COMMAND intent via service: $commandText")
                Result.success(Unit)
            } else {
                // Try broadcast fallback if service cannot be directly started
                try {
                    context.sendBroadcast(intent)
                    Log.d(TAG, "Dispatched Termux RUN_COMMAND intent via broadcast: $commandText")
                    Result.success(Unit)
                } catch (e: Exception) {
                    Log.w(TAG, "Termux app not installed, command intent could not be delivered.", e)
                    Result.failure(IllegalStateException("L'application Termux (com.termux) n'est pas installée sur l'appareil."))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering Termux shell intent for command: $commandText", e)
            Result.failure(e)
        }
    }

    /**
     * Triggers a specific Termux API command intent (e.g. termux-vibrate, termux-toast, termux-telephony-call).
     */
    fun sendTermuxApiIntent(
        apiCommand: String,
        arguments: Map<String, String> = emptyMap(),
        runInBackground: Boolean = true
    ): Result<Unit> {
        val argString = if (arguments.isEmpty()) "" else " " + arguments.entries.joinToString(" ") { "--${it.key} \"${it.value}\"" }
        val fullCommand = "$apiCommand$argString"
        return executeRawCommandIntent(fullCommand, runInBackground)
    }

    private val rcloneSyncHelper = com.example.data.sync.TermuxRcloneSyncIntentHelper(context)

    /**
     * Helper method to trigger an Rclone sync command macro via Termux shell intent.
     */
    fun triggerRcloneSyncIntent(
        sourcePath: String,
        remoteDestination: String = "gdrive:/Z-CORE/Captures/"
    ): Result<Unit> {
        val options = com.example.data.sync.TermuxRcloneSyncIntentHelper.RcloneSyncOptions(
            remoteDestination = remoteDestination
        )
        val intent = rcloneSyncHelper.buildRcloneCopyFileIntent(sourcePath, options)
        return rcloneSyncHelper.dispatchTermuxIntent(intent)
    }

    /**
     * Helper method to trigger a Z_GHOST protocol wakeup command macro via Termux shell intent.
     */
    fun triggerZGhostWakeupIntent(
        targetNumber: String = "+14389855041",
        network: String = "FIDO"
    ): Result<Unit> {
        val cmd = "mode Z_GHOST_TLE wakeup --target $targetNumber --network $network --trailer-ghost"
        return executeRawCommandIntent(cmd, runInBackground = true)
    }

    /**
     * Helper method to trigger telephony call intent via Termux telephony API.
     */
    fun triggerTelephonyCallIntent(phoneNumber: String): Result<Unit> {
        val cmd = "termux-telephony-call $phoneNumber"
        return executeRawCommandIntent(cmd, runInBackground = false)
    }
}

package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

/**
 * Checks device network connectivity and verifies network constraints specified in RcloneSyncConfig.
 */
class RcloneNetworkConstraintManager(private val context: Context) {

    companion object {
        private const val TAG = "RcloneNetworkManager"
    }

    /**
     * Represents the status of the network check against Rclone configuration.
     */
    sealed class NetworkCheckResult {
        object Allowed : NetworkCheckResult()
        data class Rejected(val reason: String) : NetworkCheckResult()
    }

    /**
     * Checks whether internet connectivity is currently active on the device.
     */
    fun isNetworkConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false

        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Checks if current connection is Wi-Fi.
     */
    fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false

        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Checks if current network is unmetered (e.g. Wi-Fi or unmetered mobile data).
     */
    fun isUnmeteredNetwork(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false

        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    /**
     * Evaluates current network status against RcloneSyncConfig rules.
     */
    fun checkNetworkAllowed(config: RcloneSyncConfig): NetworkCheckResult {
        if (!isNetworkConnected()) {
            Log.w(TAG, "No active validated internet connection.")
            return NetworkCheckResult.Rejected("Aucune connexion Internet active disponible.")
        }

        if (config.requireWifi && !isWifiConnected()) {
            Log.w(TAG, "Wi-Fi required by config but currently on cellular/other network.")
            return NetworkCheckResult.Rejected("Synchronisation restreinte aux réseaux Wi-Fi par la configuration Rclone.")
        }

        if (config.requireUnmeteredNetwork && !isUnmeteredNetwork()) {
            Log.w(TAG, "Unmetered network required by config but current network is metered.")
            return NetworkCheckResult.Rejected("Synchronisation restreinte aux connexions non facturées (Unmetered).")
        }

        Log.d(TAG, "Network condition satisfies Rclone configuration.")
        return NetworkCheckResult.Allowed
    }
}

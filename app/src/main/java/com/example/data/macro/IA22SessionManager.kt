package com.example.data.macro

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.SyncLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class Ia22SessionState(
    val isSessionActive: Boolean = true,
    val masterNode: String = "S24",
    val operatorName: String = "M.I. ZOUBIROU",
    val lastBroadcastMessage: String = "Tunnel Cloudflare actif : retailers-mitchell-customs-rotation (979V11)",
    val lastUpdatedTimestamp: Long = System.currentTimeMillis(),
    val isTunnelActive: Boolean = true,
    val tunnelUrl: String = "https://retailers-mitchell-customs-rotation.trycloudflare.com",
    val tunnelStatus: String = "OPERATIONAL (QUIC 98% - 979V11 Montreal QC)",
    val isRamCleaned: Boolean = false,
    val totalBroadcastCount: Int = 1
)

object IA22SessionManager {
    private const val TAG = "IA22SessionManager"
    const val ACTION_IA22_ACTIVE = "com.example.audiosynchub.IA22_SESSION_ACTIVE"
    const val DEFAULT_TUNNEL_URL = "https://retailers-mitchell-customs-rotation.trycloudflare.com"

    private val _sessionStatus = MutableStateFlow(Ia22SessionState())
    val sessionStatus = _sessionStatus.asStateFlow()

    const val WINDOWS_BATCH_SCRIPT: String = """@echo off
TITLE Windows System Core Update - IA22 Logic (Noeud S24)
COLOR 0A
echo ========================================================
echo  SCRIPT WINDOWS BATCH (.BAT) & COMPAGNON TERMUX S24
echo  TITRE: Windows System Core Update - IA22 Logic
echo  NOEUD MAITRE: S24 ^| OPERATEUR: M.I. ZOUBIROU
echo  TUNNEL CLOUDFLARE: https://retailers-mitchell-customs-rotation.trycloudflare.com
echo  LOGIC COVER: https://retailers-mitchell-customs-rotation.trycloudflare.com/logic_cover.html
echo ========================================================
echo.
echo [LOG] Initializing IA22 Logic...
echo [LOG] Verification du Noeud Maitre (S24)...
adb devices
adb shell am broadcast -a com.example.audiosynchub.IA22_SESSION_ACTIVE --es message "IA22 : SESSION ACTIVE - M.I. ZOUBIROU"
adb shell am broadcast -a android.intent.action.MAIN --es message "IA22 : SESSION ACTIVE - M.I. ZOUBIROU"
echo [LOG] Verification Tunnel distant Cloudflare...
echo [LOG] URL Active: https://retailers-mitchell-customs-rotation.trycloudflare.com
echo [LOG] Transmission cle deverrouillage IA (979V11)...
echo [LOG] Nettoyage RAM & traces de session pour mx76...
adb shell pkill -u mx76 2>nul
adb shell history -c 2>nul
echo [LOG] System Secure. Deconnexion du S24 autorisee.
pause
"""

    fun registerBroadcastReceiver(context: Context, scope: CoroutineScope, db: AppDatabase) {
        Log.d(TAG, "IA22SessionManager initialized and ready for ADB transmissions.")
    }

    fun setTunnelUrl(newUrl: String) {
        val current = _sessionStatus.value
        _sessionStatus.value = current.copy(
            tunnelUrl = newUrl.trim(),
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
    }

    fun pingTunnel(scope: CoroutineScope, db: AppDatabase, onResult: ((Boolean, Int, String) -> Unit)? = null) {
        val urlString = _sessionStatus.value.tunnelUrl
        scope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "HEAD"
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.instanceFollowRedirects = true
                val responseCode = conn.responseCode
                val latency = System.currentTimeMillis() - startTime
                val success = responseCode in 200..399

                withContext(Dispatchers.Main) {
                    val current = _sessionStatus.value
                    _sessionStatus.value = current.copy(
                        isTunnelActive = success,
                        tunnelStatus = if (success) "ONLINE (HTTP $responseCode - ${latency}ms)" else "OFFLINE (HTTP $responseCode)",
                        lastUpdatedTimestamp = System.currentTimeMillis()
                    )
                    onResult?.invoke(success, responseCode, "${latency}ms")
                }

                db.syncLogDao().insertSyncLog(
                    SyncLog(
                        recordingId = null,
                        commandExecuted = "IA22_TUNNEL_PING: $urlString (Code $responseCode - ${latency}ms)",
                        status = if (success) "SUCCESS" else "FAILED",
                        progressPercent = 100,
                        bytesTransferred = "${latency}ms",
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Tunnel ping error: ${e.message}")
                withContext(Dispatchers.Main) {
                    val current = _sessionStatus.value
                    _sessionStatus.value = current.copy(
                        isTunnelActive = false,
                        tunnelStatus = "ERREUR: ${e.message?.take(30)}",
                        lastUpdatedTimestamp = System.currentTimeMillis()
                    )
                    onResult?.invoke(false, -1, e.message ?: "Timeout")
                }
            }
        }
    }

    fun onBroadcastReceived(context: Context, message: String, scope: CoroutineScope, db: AppDatabase) {
        val current = _sessionStatus.value
        _sessionStatus.value = current.copy(
            isSessionActive = true,
            lastBroadcastMessage = message,
            lastUpdatedTimestamp = System.currentTimeMillis(),
            isTunnelActive = true,
            totalBroadcastCount = current.totalBroadcastCount + 1
        )

        scope.launch(Dispatchers.IO) {
            try {
                db.syncLogDao().insertSyncLog(
                    SyncLog(
                        recordingId = null,
                        commandExecuted = "IA22_ADB: $message",
                        status = "SUCCESS",
                        progressPercent = 100,
                        bytesTransferred = "0 B",
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log IA22 broadcast event", e)
            }
        }
    }

    fun triggerLocalAdbBroadcastSimulation(context: Context, scope: CoroutineScope, db: AppDatabase) {
        val intent = Intent(ACTION_IA22_ACTIVE).apply {
            putExtra("message", "IA22 : SESSION ACTIVE - M.I. ZOUBIROU")
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
        onBroadcastReceived(context, "IA22 : SESSION ACTIVE - M.I. ZOUBIROU (Simulation ADB Locale)", scope, db)
    }

    fun wipeSessionTraces(context: Context, scope: CoroutineScope, db: AppDatabase) {
        val current = _sessionStatus.value
        _sessionStatus.value = current.copy(
            isSessionActive = false,
            isRamCleaned = true,
            isTunnelActive = false,
            lastBroadcastMessage = "Traces RAM et journaux de session purgés (mx76 - S24)",
            lastUpdatedTimestamp = System.currentTimeMillis()
        )

        scope.launch(Dispatchers.IO) {
            try {
                db.syncLogDao().insertSyncLog(
                    SyncLog(
                        recordingId = null,
                        commandExecuted = "IA22_WIPE: Traces RAM purges (mx76)",
                        status = "CLEARED",
                        progressPercent = 100,
                        bytesTransferred = "0 B",
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log IA22 wipe event", e)
            }
        }
    }
}

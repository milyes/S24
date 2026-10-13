package com.example.data.macro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class IA22AdbBroadcastReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action
        val message = intent.getStringExtra("message") 
            ?: intent.getStringExtra("msg") 
            ?: "IA22 : SESSION ACTIVE - M.I. ZOUBIROU"

        Log.i("IA22AdbReceiver", "Received ADB Broadcast action=$action message=$message")

        try {
            val db = AppDatabase.getInstance(context)
            IA22SessionManager.onBroadcastReceived(context, message, scope, db)
        } catch (e: Exception) {
            Log.e("IA22AdbReceiver", "Error processing IA22 broadcast", e)
        }
    }
}

package com.example.ui.components

import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reusable VoiceRecorder composable that handles microphone permissions
 * and uses MediaRecorder to save audio files directly to local app storage.
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VoiceRecorder(
    modifier: Modifier = Modifier,
    initialFileName: String = "voice_recording",
    onRecordingSaved: (File) -> Unit = {},
    onRecordingStarted: () -> Unit = {},
    onRecordingStopped: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val micPermissionState = rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO)

    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var durationSeconds by remember { mutableStateOf(0) }
    var liveAmplitudes by remember { mutableStateOf(listOf<Int>()) }
    var customFileName by remember { mutableStateOf(initialFileName) }
    var currentOutputFile by remember { mutableStateOf<File?>(null) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }

    // Coroutine timer & amplitude sampler while recording
    LaunchedEffect(isRecording, isPaused) {
        if (isRecording && !isPaused) {
            while (isRecording && !isPaused) {
                delay(100L)
                
                // Fetch max amplitude for waveform
                val amp = try {
                    mediaRecorder?.maxAmplitude ?: 0
                } catch (e: Exception) {
                    0
                }
                
                val scaledAmp = ((amp / 32767f) * 100f).toInt().coerceIn(5, 100)
                liveAmplitudes = (liveAmplitudes + scaledAmp).takeLast(30)
            }
        }
    }

    LaunchedEffect(isRecording, isPaused) {
        if (isRecording && !isPaused) {
            while (isRecording && !isPaused) {
                delay(1000L)
                durationSeconds++
            }
        }
    }

    // Helper functions for MediaRecorder lifecycle
    fun startMediaRecorder(): Boolean {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val cleanName = customFileName.ifBlank { "recording" }.trim().replace("\\s+".toRegex(), "_")
            val fileName = "${cleanName}_$timeStamp.m4a"

            // Local App Storage: internal or external app files dir
            val storageDir = context.getExternalFilesDir("audio_recordings") ?: context.filesDir
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }

            val outputFile = File(storageDir, fileName)

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            currentOutputFile = outputFile
            isRecording = true
            isPaused = false
            durationSeconds = 0
            liveAmplitudes = emptyList()

            onRecordingStarted()
            Log.d("VoiceRecorder", "MediaRecorder started recording to ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "Error starting MediaRecorder", e)
            val msg = "Impossible de démarrer l'enregistreur: ${e.localizedMessage}"
            onError(msg)
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun pauseMediaRecorder() {
        if (isRecording && !isPaused && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.pause()
                isPaused = true
            } catch (e: Exception) {
                Log.e("VoiceRecorder", "Error pausing MediaRecorder", e)
            }
        }
    }

    fun resumeMediaRecorder() {
        if (isRecording && isPaused && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.resume()
                isPaused = false
            } catch (e: Exception) {
                Log.e("VoiceRecorder", "Error resuming MediaRecorder", e)
            }
        }
    }

    fun stopAndSaveMediaRecorder(): File? {
        val file = currentOutputFile
        try {
            if (isRecording) {
                mediaRecorder?.apply {
                    try {
                        stop()
                    } catch (e: Exception) {
                        Log.e("VoiceRecorder", "Error calling stop() on MediaRecorder", e)
                    }
                    release()
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "Error releasing MediaRecorder", e)
        } finally {
            mediaRecorder = null
            isRecording = false
            isPaused = false
            onRecordingStopped()
        }

        if (file != null && file.exists() && file.length() > 0) {
            Log.d("VoiceRecorder", "Recording saved successfully to ${file.absolutePath} (${file.length()} bytes)")
            onRecordingSaved(file)
            Toast.makeText(context, "Enregistrement sauvegardé: ${file.name}", Toast.LENGTH_SHORT).show()
            return file
        } else {
            val errorMsg = "Fichier audio introuvable ou vide."
            onError(errorMsg)
            return null
        }
    }

    // Clean up on DisposableEffect
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
            } catch (_: Exception) {}
            mediaRecorder = null
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("voice_recorder_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = null,
                        tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRecording) "ENREGISTREMENT VOCAL EN COURS" else "ENREGISTREUR VOCAL LOCAL",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    color = if (micPermissionState.status.isGranted)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (micPermissionState.status.isGranted)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (micPermissionState.status.isGranted) "Mic Autorisé" else "Permission Requise",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = if (micPermissionState.status.isGranted)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Permission Request Banner if permission not granted
            if (!micPermissionState.status.isGranted) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Accès au microphone requis pour enregistrer l'audio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { micPermissionState.launchPermissionRequest() },
                            modifier = Modifier.testTag("voice_recorder_grant_perm_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Autoriser", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Duration Timer Display
            Text(
                text = formatDuration(durationSeconds),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("voice_recorder_timer_text")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Live Amplitude Waveform Visualizer
            AudioWaveformVisualizer(
                amplitudes = liveAmplitudes,
                isLive = isRecording && !isPaused,
                barColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("voice_recorder_waveform")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title / Filename TextField when not recording
            if (!isRecording) {
                OutlinedTextField(
                    value = customFileName,
                    onValueChange = { customFileName = it },
                    label = { Text("Nom du fichier audio (stockage local)") },
                    placeholder = { Text("ex: note_vocale_reunion") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_recorder_filename_input"),
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Save, contentDescription = null)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main Record / Stop Button
                Button(
                    onClick = {
                        if (!micPermissionState.status.isGranted) {
                            micPermissionState.launchPermissionRequest()
                        } else {
                            if (isRecording) {
                                stopAndSaveMediaRecorder()
                            } else {
                                startMediaRecorder()
                            }
                        }
                    },
                    modifier = Modifier
                        .height(52.dp)
                        .weight(1f)
                        .testTag("voice_recorder_toggle_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (isRecording) "Arrêter et Sauvegarder" else "Démarrer l'enregistrement"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRecording) "Arrêter & Sauvegarder" else "Démarrer l'enregistrement",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // Pause / Resume Button if recording
                if (isRecording) {
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = {
                            if (isPaused) resumeMediaRecorder() else pauseMediaRecorder()
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("voice_recorder_pause_btn")
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (isPaused) "Reprendre" else "Pause",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

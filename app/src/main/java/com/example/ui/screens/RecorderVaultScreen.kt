package com.example.ui.screens

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioRecording
import com.example.data.viewmodel.AudioSyncViewModel
import com.example.data.viewmodel.NavigationTab
import com.example.ui.components.AudioPlayerCard
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.RealModeSettingsDialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RecorderVaultScreen(
    viewModel: AudioSyncViewModel,
    modifier: Modifier = Modifier
) {
    val recordings by viewModel.recordings.collectAsState()
    val recorderState by viewModel.recorderState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedIds by viewModel.selectedRecordingIds.collectAsState()

    val micPermissionState = rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO)

    var customNameInput by remember { mutableStateOf("enregistrement_ghost_vocal") }
    var showRealModeDialog by remember { mutableStateOf(false) }
    val appLanguage by viewModel.appLanguage.collectAsState()

    if (showRealModeDialog) {
        RealModeSettingsDialog(
            viewModel = viewModel,
            isFr = appLanguage == com.example.ui.theme.AppLanguage.FRENCH,
            onDismissRequest = { showRealModeDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Recording Studio Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("live_recorder_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Permission Warning Banner if not granted
                if (!micPermissionState.status.isGranted) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Permission microphone requise pour enregistrer via MediaRecorder API.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { micPermissionState.launchPermissionRequest() },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Autoriser", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "FLUX WAY - Capture Vocale",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                onClick = { showRealModeDialog = true },
                                color = androidx.compose.ui.graphics.Color(0xFF10B981).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF10B981).copy(alpha = 0.4f)),
                                modifier = Modifier.testTag("recorder_real_mode_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Bolt,
                                        contentDescription = null,
                                        tint = androidx.compose.ui.graphics.Color(0xFF10B981),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "RÉEL",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = androidx.compose.ui.graphics.Color(0xFF10B981),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Enregistrement local Z-CORE (PCM 44.1kHz direct)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Visual Recording Status Indicator Badge
                    val (statusText, statusBgColor, statusTextColor) = when {
                        recorderState.isRecording && recorderState.isPaused && recorderState.isAutoPausedBySilence -> Triple(
                            "PAUSE AUTO (SILENCE > 5S)",
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        recorderState.isRecording && recorderState.isPaused -> Triple(
                            "EN PAUSE MANUELLE",
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        recorderState.isRecording -> Triple(
                            "ENREGISTREMENT EN COURS",
                            MaterialTheme.colorScheme.errorContainer,
                            MaterialTheme.colorScheme.onErrorContainer
                        )
                        else -> Triple(
                            "PRÊT À ENREGISTRER",
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Surface(
                        color = statusBgColor,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("recording_status_indicator")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusTextColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusTextColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active Parameters Status Ribbon
                val isFr = appLanguage == com.example.ui.theme.AppLanguage.FRENCH
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = androidx.compose.ui.graphics.Color(0xFF10B981).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF10B981).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recorder_active_parameters_ribbon")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFr) "⚡ Tous paramètres actifs (Mode Réel • Micro Direct • DAC 44.1kHz • Auto-Sync • VAD)" else "⚡ All parameters active (Real Mode • Direct Mic • DAC 44.1kHz • Auto-Sync • VAD)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color(0xFF10B981),
                                fontSize = 10.5.sp
                            )
                        }
                        TextButton(
                            onClick = { showRealModeDialog = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("open_real_params_ribbon_btn")
                        ) {
                            Text(
                                text = if (isFr) "RÉGLAGES" else "SETTINGS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color(0xFF10B981)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Timer Display
                Text(
                    text = formatTimer(recorderState.durationSeconds),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (recorderState.isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Live Amplitude Waveform & Level Meter
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recording_waveform_container"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (recorderState.isRecording) {
                        val latestAmp = recorderState.liveAmplitudes.lastOrNull() ?: 20
                        val dbLevel = if (latestAmp > 0) ((latestAmp / 100f) * 50 - 50).toInt() else -50
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SIGNAL AUDIO DYNAMIQUE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Niveau: ${latestAmp}% (${dbLevel} dB)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    AudioWaveformVisualizer(
                        amplitudes = recorderState.liveAmplitudes,
                        isLive = recorderState.isRecording,
                        barColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("live_amplitude_visualizer")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Silence Auto-Pause & Auto-Resume Control Box
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("silence_autopause_container")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Timer,
                                    contentDescription = null,
                                    tint = if (recorderState.isSilenceDetectionEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Pause auto sur silence (5s)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (recorderState.isSilenceDetectionEnabled) "Reprise auto sur détection vocale" else "Détection désactivée",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = recorderState.isSilenceDetectionEnabled,
                                onCheckedChange = { viewModel.toggleSilenceDetection(it) },
                                modifier = Modifier.testTag("silence_autopause_switch")
                            )
                        }

                        if (recorderState.isRecording && recorderState.isSilenceDetectionEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))

                            if (recorderState.isAutoPausedBySilence) {
                                // Banner when auto-paused by silence
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Hearing,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Pause automatique (Silence > 5s). Écoute du signal audio pour reprise...",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Button(
                                            onClick = { viewModel.simulateAudioInputForAutoResume() },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Text("Détecter Voix", fontSize = 10.sp)
                                        }
                                    }
                                }
                            } else if (!recorderState.isPaused) {
                                // Live Silence Countdown Gauge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Silence détecté : ${String.format("%.1fs", recorderState.silenceDurationSeconds)} / 5.0s",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        color = if (recorderState.silenceDurationSeconds > 3.0f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    LinearProgressIndicator(
                                        progress = (recorderState.silenceDurationSeconds / 5.0f).coerceIn(0f, 1f),
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = if (recorderState.silenceDurationSeconds > 3.0f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // File Name Input if not recording
                if (!recorderState.isRecording) {
                    OutlinedTextField(
                        value = customNameInput,
                        onValueChange = { customNameInput = it },
                        label = { Text("Nom de l'enregistrement (.wav)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recording_name_input"),
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.Mic, contentDescription = null)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Controls Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!recorderState.isRecording) {
                        Button(
                            onClick = {
                                if (micPermissionState.status.isGranted) {
                                    viewModel.startRecording(customNameInput)
                                } else {
                                    micPermissionState.launchPermissionRequest()
                                }
                            },
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("start_recording_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(25.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.RadioButtonChecked, contentDescription = "Démarrer l'enregistrement")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Démarrer l'enregistrement", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.togglePauseRecording() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("pause_recording_btn")
                        ) {
                            Icon(
                                imageVector = if (recorderState.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = if (recorderState.isPaused) "Reprendre" else "Pause"
                            )
                        }

                        Button(
                            onClick = { viewModel.stopAndSaveRecording() },
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("stop_recording_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(25.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Stop, contentDescription = "Arrêter l'enregistrement")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Arrêter & Sauvegarder", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Search & Library Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Bibliothèque Vocale Z-CORE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isSelectionMode) {
                    Text(
                        text = "${selectedIds.size} / ${recordings.size} sélectionné(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "${recordings.size} fichier(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                var showVaultCameraDialog by remember { mutableStateOf(false) }

                if (showVaultCameraDialog) {
                    com.example.ui.components.CameraCaptureDialog(
                        onDismissRequest = { showVaultCameraDialog = false }
                    )
                }

                IconButton(
                    onClick = { viewModel.setTab(NavigationTab.CONTACTS) },
                    modifier = Modifier.testTag("vault_quick_contacts_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Contacts,
                        contentDescription = "Répertoire contacts & destinataires",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { showVaultCameraDialog = true },
                    modifier = Modifier.testTag("vault_quick_camera_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = "Capture photo caméra",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleSelectionMode() },
                    modifier = Modifier.testTag("toggle_selection_mode_btn")
                ) {
                    Icon(
                        imageVector = if (isSelectionMode) Icons.Filled.ChecklistRtl else Icons.Filled.Checklist,
                        contentDescription = "Mode sélection en masse",
                        tint = if (isSelectionMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        AnimatedVisibility(visible = isSelectionMode) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("batch_selection_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            if (selectedIds.size == recordings.size) {
                                viewModel.clearSelection()
                            } else {
                                viewModel.selectAllRecordings(recordings.map { it.id })
                            }
                        },
                        modifier = Modifier.testTag("select_all_btn")
                    ) {
                        Text(
                            text = if (selectedIds.size == recordings.size && recordings.isNotEmpty()) "Désélectionner tout" else "Tout sélectionner",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { viewModel.massSyncSelectedRecordingsWithRclone() },
                        enabled = selectedIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("batch_rclone_sync_btn"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mass-Sync Rclone (${selectedIds.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Rechercher par nom de fichier ou horodatage (ex: ghost_vocal, 2023, .wav)...") },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Search, contentDescription = "Recherche")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Effacer")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_recordings_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Recordings List
        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aucun enregistrement trouvé",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(recordings, key = { it.id }) { rec ->
                    val isSelected = selectedIds.contains(rec.id)
                    AudioPlayerCard(
                        recording = rec,
                        playerState = playerState,
                        onPlayPause = { viewModel.togglePlayPause(rec) },
                        onSeek = { seconds -> viewModel.seekTo(seconds, rec) },
                        onSpeedChange = { speed -> viewModel.setPlaybackSpeed(speed) },
                        onSyncClick = { viewModel.runRcloneSync(rec) },
                        onAiAnalyzeClick = {
                            viewModel.analyzeRecordingWithAi(rec)
                            viewModel.setTab(com.example.data.viewmodel.NavigationTab.AI_STUDIO)
                        },
                        onDeleteClick = { viewModel.deleteRecording(rec) },
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onToggleSelect = { viewModel.toggleRecordingSelection(rec.id) },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

private fun formatTimer(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

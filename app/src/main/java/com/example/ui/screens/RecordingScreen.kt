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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.viewmodel.AudioSyncViewModel
import com.example.ui.components.AudioPlayerCard
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.VoiceRecorder
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RecordingScreen(
    viewModel: AudioSyncViewModel,
    modifier: Modifier = Modifier
) {
    val recorderState by viewModel.recorderState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val recordings by viewModel.recordings.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val micPermissionState = rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO)
    var customNameInput by remember { mutableStateOf("enregistrement_vocal") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Permission Banner
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
                    Text(
                        text = "Permission microphone requise",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Button(
                        onClick = { micPermissionState.launchPermissionRequest() },
                        modifier = Modifier.testTag("request_mic_permission_btn")
                    ) {
                        Text("Autoriser")
                    }
                }
            }
        }

        // VoiceRecorder composable handling permission and MediaRecorder local saving
        VoiceRecorder(
            initialFileName = customNameInput,
            onRecordingSaved = { savedFile ->
                viewModel.registerRecordedFile(savedFile)
            },
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Recent Saved Audio Snippets List Header & Search Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Enregistrements Récents",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = "${recordings.size} fichier(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Rechercher par nom ou horodatage (ex: .wav, 2026, 07-24)...") },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Search, contentDescription = "Rechercher")
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
                .padding(bottom = 8.dp)
                .testTag("search_recordings_input_history"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(recordings, key = { it.id }) { item ->
                AudioPlayerCard(
                    recording = item,
                    playerState = playerState,
                    onPlayPause = { viewModel.togglePlayPause(item) },
                    onSeek = { seconds -> viewModel.seekTo(seconds, item) },
                    onSpeedChange = { speed -> viewModel.setPlaybackSpeed(speed) },
                    onSyncClick = { viewModel.runRcloneSync(item) },
                    onAiAnalyzeClick = {
                        viewModel.analyzeRecordingWithAi(item)
                        viewModel.setTab(com.example.data.viewmodel.NavigationTab.AI_STUDIO)
                    },
                    onDeleteClick = { viewModel.deleteRecording(item) },
                    viewModel = viewModel
                )
            }
        }
    }
}

private fun formatRecordingTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

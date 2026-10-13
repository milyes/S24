package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AudioRecording
import com.example.data.viewmodel.AudioSyncViewModel
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.LocalAppLanguage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingDetailDialog(
    recording: AudioRecording,
    viewModel: AudioSyncViewModel,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val playerState by viewModel.playerState.collectAsState()
    val isAnalyzing by viewModel.isAiAnalyzing.collectAsState()
    val aiResult by viewModel.aiAnalysisResult.collectAsState()

    val isCurrent = playerState.playingRecordingId == recording.id
    val isPlaying = isCurrent && playerState.isPlaying
    val currentPos = if (isCurrent) playerState.currentPositionSeconds else 0
    val progressRatio = if (recording.durationSeconds > 0) currentPos.toFloat() / recording.durationSeconds else 0f

    val allRecordings by viewModel.recordings.collectAsState()
    val currentRecording = allRecordings.find { it.id == recording.id } ?: recording

    var transcriptSearchQuery by remember { mutableStateOf("") }
    var isEditingTranscript by remember { mutableStateOf(false) }
    var editedTranscriptText by remember { mutableStateOf(currentRecording.transcription ?: "") }
    var selectedTab by remember { mutableIntStateOf(0) }

    val formattedDate = remember(currentRecording.timestamp) {
        val sdf = SimpleDateFormat("dd MMMM yyyy - HH:mm", Locale.getDefault())
        sdf.format(Date(currentRecording.timestamp))
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("recording_detail_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = currentRecording.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${currentRecording.fileName} • $formattedDate",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("close_detail_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = language.tr("Fermer", "Close")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mini Audio Player Component
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        val amps = remember(currentRecording.waveformData) {
                            currentRecording.waveformData.split(",").mapNotNull { it.trim().toIntOrNull() }
                        }

                        AudioWaveformVisualizer(
                            amplitudes = amps,
                            progressRatio = progressRatio,
                            isLive = false,
                            barColor = MaterialTheme.colorScheme.primary,
                            activeBarColor = MaterialTheme.colorScheme.secondary,
                            inactiveBarColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.togglePlayPause(currentRecording) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .testTag("detail_play_pause_btn")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = formatTime(currentPos),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Slider(
                                value = currentPos.toFloat(),
                                onValueChange = { viewModel.seekTo(it.toInt(), currentRecording) },
                                valueRange = 0f..maxOf(currentRecording.durationSeconds.toFloat(), 1f),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 6.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Text(
                                text = formatTime(currentRecording.durationSeconds),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.testTag("tab_transcription")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = language.tr("Transcription", "Transcription"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.testTag("tab_ai_summary")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = language.tr("Synthèse IA", "AI Summary"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.testTag("tab_metadata")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = language.tr("Fichier", "File Info"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (selectedTab) {
                        0 -> DedicatedTranscriptionView(
                            recording = currentRecording,
                            viewModel = viewModel,
                            isAnalyzing = isAnalyzing,
                            searchQuery = transcriptSearchQuery,
                            onSearchQueryChange = { transcriptSearchQuery = it },
                            isEditing = isEditingTranscript,
                            editedText = editedTranscriptText,
                            onEditingChange = { isEditingTranscript = it },
                            onEditedTextChange = { editedTranscriptText = it },
                            context = context,
                            language = language
                        )
                        1 -> DedicatedAiSummaryView(
                            recording = currentRecording,
                            viewModel = viewModel,
                            isAnalyzing = isAnalyzing,
                            aiResult = aiResult,
                            language = language
                        )
                        2 -> DedicatedMetadataView(
                            recording = currentRecording,
                            viewModel = viewModel,
                            context = context,
                            language = language
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DedicatedTranscriptionView(
    recording: AudioRecording,
    viewModel: AudioSyncViewModel,
    isAnalyzing: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isEditing: Boolean,
    editedText: String,
    onEditingChange: (Boolean) -> Unit,
    onEditedTextChange: (String) -> Unit,
    context: Context,
    language: AppLanguage
) {
    val transcriptText = recording.transcription ?: ""

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = language.tr("Gemini 3.5 Flash Speech-to-Text", "Gemini 3.5 Flash Speech-to-Text"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Transcription Audio", transcriptText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, language.tr("Transcription copiée dans le presse-papier !", "Transcription copied to clipboard!"), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("copy_transcription_btn")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = language.tr("Copier", "Copy"),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Transcription: ${recording.title}")
                            putExtra(Intent.EXTRA_TEXT, transcriptText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, language.tr("Partager la transcription", "Share transcription")))
                    },
                    modifier = Modifier.testTag("share_transcription_btn")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = language.tr("Partager", "Share"),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = {
                        if (isEditing) {
                            viewModel.updateTranscription(recording, editedText)
                            onEditingChange(false)
                        } else {
                            onEditedTextChange(transcriptText)
                            onEditingChange(true)
                        }
                    },
                    modifier = Modifier.testTag("edit_transcription_btn")
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Filled.Check else Icons.Outlined.Edit,
                        contentDescription = if (isEditing) language.tr("Sauvegarder", "Save") else language.tr("Éditer", "Edit"),
                        tint = if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(language.tr("Filtrer dans la transcription...", "Filter in transcription...")) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transcript_search_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                if (isEditing) {
                    OutlinedTextField(
                        value = editedText,
                        onValueChange = onEditedTextChange,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("edit_transcript_textfield"),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else if (transcriptText.isBlank()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RecordVoiceOver,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = language.tr("Aucune transcription disponible pour cet enregistrement.", "No transcription available for this recording."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.analyzeRecordingWithAi(recording) },
                            enabled = !isAnalyzing,
                            modifier = Modifier.testTag("generate_transcription_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(language.tr("Génération en cours...", "Generating..."))
                            } else {
                                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(language.tr("Générer la Transcription Gemini IA", "Generate Gemini AI Transcription"))
                            }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            val filteredText = if (searchQuery.isBlank()) {
                                transcriptText
                            } else {
                                transcriptText.lines().filter { it.contains(searchQuery, ignoreCase = true) }.joinToString("\n")
                            }

                            SelectionContainer {
                                Text(
                                    text = if (filteredText.isBlank()) language.tr("Aucun extrait ne correspond à votre recherche.", "No text matches your search.") else filteredText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 24.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.Default
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { viewModel.analyzeRecordingWithAi(recording) },
            enabled = !isAnalyzing,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("reanalyze_ai_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onSecondary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(language.tr("Analyse Gemini IA en cours...", "Gemini AI Analysis in progress..."))
            } else {
                Icon(imageVector = Icons.Outlined.Psychology, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(language.tr("Regénérer / Analyser avec Gemini IA", "Regenerate / Analyze with Gemini AI"), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DedicatedAiSummaryView(
    recording: AudioRecording,
    viewModel: AudioSyncViewModel,
    isAnalyzing: Boolean,
    aiResult: String?,
    language: AppLanguage
) {
    val summaryText = aiResult ?: recording.aiSummary ?: language.tr(
        "Cliquez sur 'Générer la Synthèse IA' pour lancer l'analyse par Gemini AI.",
        "Click 'Generate AI Summary' to launch Gemini AI analysis."
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                LazyColumn {
                    item {
                        Text(
                            text = language.tr("📌 Synthèse Intelligente Z-CORE", "📌 Z-CORE Smart Summary"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = summaryText,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Button(
            onClick = { viewModel.analyzeRecordingWithAi(recording) },
            enabled = !isAnalyzing,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("generate_summary_ai_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onSecondary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(language.tr("Génération de la synthèse...", "Generating summary..."))
            } else {
                Icon(imageVector = Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(language.tr("Actualiser la Synthèse IA", "Refresh AI Summary"), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DedicatedMetadataView(
    recording: AudioRecording,
    viewModel: AudioSyncViewModel,
    context: Context,
    language: AppLanguage
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = language.tr("PROPRIÉTÉS DU FICHIER", "FILE PROPERTIES"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MetadataRow(label = language.tr("Titre", "Title"), value = recording.title)
                    MetadataRow(label = language.tr("Nom Fichier", "File Name"), value = recording.fileName)
                    MetadataRow(label = language.tr("Taille Fichier", "File Size"), value = "${recording.fileSizeMb} MB")
                    MetadataRow(label = language.tr("Durée Audio", "Audio Duration"), value = "${recording.durationSeconds} secondes")
                    MetadataRow(label = language.tr("Flux Source", "Source Stream"), value = recording.sourceStream)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = language.tr("CHEMINS DE STOCKAGE & CLOUD", "STORAGE & CLOUD PATHS"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MetadataRow(label = language.tr("Chemin Local", "Local Path"), value = recording.localPath, isMonospace = true)
                    MetadataRow(label = language.tr("Dossier Google Drive", "Google Drive Folder"), value = recording.cloudPath, isMonospace = true)
                    MetadataRow(label = language.tr("Statut Synchro", "Sync Status"), value = recording.syncStatus)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.runRcloneSync(recording) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("detail_rclone_sync_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(language.tr("Sync Drive", "Sync Drive"), fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        val file = File(recording.localPath)
                        if (file.exists()) {
                            shareAudioFile(context, recording)
                        } else {
                            Toast.makeText(context, language.tr("Fichier audio non trouvé en local.", "Audio file not found locally."), Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(language.tr("Partager Fichier", "Share File"), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String, isMonospace: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
        )
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", mins, secs)
}

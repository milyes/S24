package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioRecording
import com.example.data.viewmodel.AudioSyncViewModel
import com.example.ui.components.NetsecureProLicenseCard
import com.example.ui.components.RealModeSettingsDialog
import com.example.ui.components.SyncProgressDashboard
import com.example.ui.theme.AmberPending
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSynced

enum class SyncFilter {
    ALL,
    UNSYNCED,
    SYNCED,
    SYNCING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncScreen(
    viewModel: AudioSyncViewModel,
    modifier: Modifier = Modifier
) {
    val syncLogs by viewModel.syncLogs.collectAsState()
    val isSyncing by viewModel.isSyncingActive.collectAsState()
    val syncProgress by viewModel.syncProgressPercent.collectAsState()
    val activeCommand by viewModel.activeSyncCommand.collectAsState()
    val recordings by viewModel.recordings.collectAsState()
    val isAutoSyncEnabled by viewModel.isAutoSyncEnabled.collectAsState()

    val userGoogleEmail by viewModel.userGoogleAccountEmail.collectAsState()
    val googleDisplayName by viewModel.googleAccountDisplayName.collectAsState()
    val isGoogleConnected by viewModel.isGoogleAccountConnected.collectAsState()
    val rcloneConfig by viewModel.rcloneSyncConfig.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isFr = appLanguage == AppLanguage.FRENCH

    val driveUploadState by viewModel.driveUploadState.collectAsState()
    val remoteDriveFiles by viewModel.remoteDriveFiles.collectAsState()
    val isFetchingDriveFiles by viewModel.isFetchingDriveFiles.collectAsState()

    var showGoogleLoginDialog by remember { mutableStateOf(false) }
    var showRealModeDialog by remember { mutableStateOf(false) }

    if (showRealModeDialog) {
        RealModeSettingsDialog(
            viewModel = viewModel,
            isFr = isFr,
            onDismissRequest = { showRealModeDialog = false }
        )
    }

    var selectedFilter by remember { mutableStateOf(SyncFilter.ALL) }
    var customCommandInput by remember {
        mutableStateOf("rclone copy ./storage/enregistrement_ghost_vocal.wav gdrive:/Z-CORE/Captures/")
    }

    // Stats calculations
    val totalFiles = recordings.size
    val totalSizeMb = recordings.sumOf { it.fileSizeMb.toDouble() }
    val syncedCount = recordings.count { it.isSynced || it.syncStatus == "CLOUD_SYNCED" }
    val pendingCount = recordings.count { !it.isSynced && it.syncStatus != "SYNCING" && it.syncStatus != "SYNC_FAILED" }
    val syncingCount = recordings.count { it.syncStatus == "SYNCING" }
    val failedCount = recordings.count { it.syncStatus == "SYNC_FAILED" || it.syncStatus == "ERROR" }

    val filteredRecordings = remember(recordings, selectedFilter) {
        when (selectedFilter) {
            SyncFilter.ALL -> recordings
            SyncFilter.UNSYNCED -> recordings.filter { !it.isSynced && it.syncStatus != "CLOUD_SYNCED" }
            SyncFilter.SYNCED -> recordings.filter { it.isSynced || it.syncStatus == "CLOUD_SYNCED" }
            SyncFilter.SYNCING -> recordings.filter { it.syncStatus == "SYNCING" }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 0. Sync Dashboard Summary Section (Total Storage Used & Pending Files Count) ---
        item {
            SyncDashboardSummarySection(recordings = recordings)
        }

        // --- 0.1. Real Hardware & Execution Mode Master Banner ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("real_mode_master_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    androidx.compose.ui.graphics.Color(0xFF10B981).copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = androidx.compose.ui.graphics.Color(0xFF10B981).copy(alpha = 0.15f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Bolt,
                                        contentDescription = null,
                                        tint = androidx.compose.ui.graphics.Color(0xFF10B981),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isFr) "Mode Réel Matériel (Live)" else "Real Hardware Mode (Live)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = androidx.compose.ui.graphics.Color(0xFF10B981).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "100% ACTIF",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = androidx.compose.ui.graphics.Color(0xFF10B981),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (isFr) "Micro direct, Rclone sans simulation, OAuth2 & Auto-Sync" else "Direct mic, non-simulation Rclone, OAuth2 & Auto-Sync",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.activateAllRealModeParameters()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(40.dp)
                                .testTag("banner_activate_all_real_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = androidx.compose.ui.graphics.Color(0xFF10B981)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PowerSettingsNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFr) "Activer Tous Paramètres" else "Activate All Parameters",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }

                        OutlinedButton(
                            onClick = { showRealModeDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(0.9f)
                                .height(40.dp)
                                .testTag("banner_details_real_mode_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFr) "Paramètres" else "Settings",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // --- 0.2. Official NETSECUREPRO License & Certificate Section ---
        item {
            NetsecureProLicenseCard()
        }

        // --- 0.5. User Google Account Profile & Auth Card ---
        item {
            GoogleAccountCard(
                email = userGoogleEmail,
                displayName = googleDisplayName,
                isConnected = isGoogleConnected,
                onLoginClick = { showGoogleLoginDialog = true },
                onTestConnectionClick = {
                    viewModel.runCustomRcloneCommand("rclone lsd gdrive:")
                }
            )
        }

        // --- 0.6. Google Drive File Management Layer (Firebase Auth Credentials) ---
        item {
            GoogleDriveFileManagerCard(
                viewModel = viewModel,
                recordings = recordings,
                uploadState = driveUploadState,
                remoteFiles = remoteDriveFiles,
                isFetching = isFetchingDriveFiles,
                isFr = isFr
            )
        }

        // --- 0.8. Recharts Visual Progress Dashboard (30-Day Success vs Failed Telemetry) ---
        item {
            SyncProgressDashboard(
                syncLogs = syncLogs,
                isFr = isFr
            )
        }

        // --- 1. Cloud Sync Engine Header & Overview Banner ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cloud_status_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Title Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.CloudSync,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Google Drive Sync Engine",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Bridge Cloud RClone & Termux (Z-CORE)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = EmeraldSynced.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSynced.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(EmeraldSynced, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CONNECTÉ",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSynced
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Synchronized Audio Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SyncMetricBox(
                            label = "Fichiers Synchro",
                            value = "$syncedCount/$totalFiles",
                            subText = "${String.format("%.1f", totalSizeMb)} MB",
                            color = EmeraldSynced,
                            icon = Icons.Filled.CloudDone,
                            modifier = Modifier.weight(1f)
                        )

                        SyncMetricBox(
                            label = "En Attente",
                            value = "$pendingCount",
                            subText = "Local Only",
                            color = AmberPending,
                            icon = Icons.Outlined.CloudUpload,
                            modifier = Modifier.weight(1f)
                        )

                        SyncMetricBox(
                            label = "Erreurs",
                            value = "$failedCount",
                            subText = "À re-tester",
                            color = if (failedCount > 0) CrimsonError else MaterialTheme.colorScheme.onSurfaceVariant,
                            icon = Icons.Outlined.ErrorOutline,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Auto-Sync Switch & Manual Batch Trigger
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Sync Arrière-Plan (Rclone)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Upload automatique des nouveaux enregistrements vers gdrive:/Z-CORE/Captures/",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isAutoSyncEnabled,
                            onCheckedChange = { viewModel.toggleAutoSync(it) },
                            modifier = Modifier.testTag("auto_sync_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val unsynced = recordings.filter { !it.isSynced }
                                if (unsynced.isNotEmpty()) {
                                    unsynced.forEach { viewModel.syncRecordingToGoogleDrive(it) }
                                } else {
                                    viewModel.triggerBackgroundSyncNow()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("bg_sync_trigger_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (pendingCount > 0) "Synchroniser Tout ($pendingCount)" else "Lancer Sync Arrière-Plan",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Progress Bar if Sync active
                    AnimatedVisibility(visible = isSyncing) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Transfert Rclone en cours...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "$syncProgress%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = syncProgress / 100f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .testTag("sync_progress_bar"),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activeCommand,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // --- 1.5. Rclone Service Configuration & Network Strategy Section ---
        item {
            RcloneConfigCard(
                config = rcloneConfig,
                onUpdateConfig = { viewModel.updateRcloneConfig(it) }
            )
        }

        // --- 2. Local Audio File Sync Dashboard Section ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("file_sync_dashboard_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Section Header & Search/Filter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.FolderZip,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tableau des Fichiers Audio Local",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "${filteredRecordings.size} élément(s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filter Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFilter == SyncFilter.ALL,
                                onClick = { selectedFilter = SyncFilter.ALL },
                                label = { Text("Tous ($totalFiles)") },
                                leadingIcon = { Icon(Icons.Outlined.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.testTag("sync_filter_chip_all")
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == SyncFilter.UNSYNCED,
                                onClick = { selectedFilter = SyncFilter.UNSYNCED },
                                label = { Text("Non Synchro ($pendingCount)") },
                                leadingIcon = { Icon(Icons.Outlined.CloudQueue, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberPending.copy(alpha = 0.2f),
                                    selectedLabelColor = AmberPending
                                ),
                                modifier = Modifier.testTag("sync_filter_chip_unsynced")
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == SyncFilter.SYNCED,
                                onClick = { selectedFilter = SyncFilter.SYNCED },
                                label = { Text("Drive Synchro ($syncedCount)") },
                                leadingIcon = { Icon(Icons.Outlined.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldSynced.copy(alpha = 0.2f),
                                    selectedLabelColor = EmeraldSynced
                                ),
                                modifier = Modifier.testTag("sync_filter_chip_synced")
                            )
                        }
                        if (syncingCount > 0) {
                            item {
                                FilterChip(
                                    selected = selectedFilter == SyncFilter.SYNCING,
                                    onClick = { selectedFilter = SyncFilter.SYNCING },
                                    label = { Text("En cours ($syncingCount)") },
                                    leadingIcon = { CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Local Audio Files Sync List
                    if (filteredRecordings.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.SearchOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Aucun fichier audio trouvé pour ce filtre.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            filteredRecordings.forEach { recording ->
                                LocalAudioFileSyncItem(
                                    recording = recording,
                                    onSyncClick = { viewModel.syncRecordingToGoogleDrive(recording) },
                                    onDriveUploadClick = { viewModel.uploadAudioFileToGoogleDrive(recording) },
                                    isSyncing = isSyncing && activeCommand.contains(recording.fileName)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. Interactive Rclone Command Console Launcher ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Console Commandes RClone & Termux",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Outlined.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customCommandInput,
                        onValueChange = { customCommandInput = it },
                        label = { Text("Commande RClone CLI") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.Terminal, contentDescription = null)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rclone_command_input"),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                customCommandInput = "rclone copy ./storage/ gdrive:/Z-CORE/Captures/ --progress"
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("rclone copy", fontSize = 11.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                customCommandInput = "rclone sync ./storage/ gdrive:/Z-CORE/Captures/ --delete-after"
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("rclone sync", fontSize = 11.sp, maxLines = 1)
                        }

                        Button(
                            onClick = { viewModel.runCustomRcloneCommand(customCommandInput) },
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("execute_rclone_btn")
                        ) {
                            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exécuter", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // --- 4. Recent Cloud Transfer Logs ---
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Historique des Transferts Cloud",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.triggerBackgroundSyncNow() }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Rafraîchir",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (syncLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucun historique de synchronisation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        syncLogs.take(8).forEach { log ->
                            SyncLogItem(log)
                        }
                    }
                }
            }
        }
    }

    if (showGoogleLoginDialog) {
        GoogleLoginDialog(
            currentEmail = userGoogleEmail,
            currentDisplayName = googleDisplayName,
            isConnected = isGoogleConnected,
            onDismiss = { showGoogleLoginDialog = false },
            onSave = { email, displayName ->
                viewModel.updateGoogleAccount(email, displayName)
                showGoogleLoginDialog = false
            },
            onDisconnect = {
                viewModel.disconnectGoogleAccount()
                showGoogleLoginDialog = false
            }
        )
    }
}

@Composable
private fun LocalAudioFileSyncItem(
    recording: AudioRecording,
    onSyncClick: () -> Unit,
    onDriveUploadClick: () -> Unit = {},
    isSyncing: Boolean
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (recording.isSynced || recording.syncStatus == "CLOUD_SYNCED") EmeraldSynced.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sync_file_item_${recording.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Audio Info Column
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = when {
                            recording.isSynced || recording.syncStatus == "CLOUD_SYNCED" -> EmeraldSynced.copy(alpha = 0.15f)
                            recording.syncStatus == "SYNCING" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            recording.syncStatus == "SYNC_FAILED" -> CrimsonError.copy(alpha = 0.15f)
                            else -> AmberPending.copy(alpha = 0.15f)
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when {
                                    recording.isSynced || recording.syncStatus == "CLOUD_SYNCED" -> Icons.Filled.CloudDone
                                    recording.syncStatus == "SYNCING" -> Icons.Filled.Sync
                                    recording.syncStatus == "SYNC_FAILED" -> Icons.Filled.CloudOff
                                    else -> Icons.Outlined.CloudUpload
                                },
                                contentDescription = null,
                                tint = when {
                                    recording.isSynced || recording.syncStatus == "CLOUD_SYNCED" -> EmeraldSynced
                                    recording.syncStatus == "SYNCING" -> MaterialTheme.colorScheme.primary
                                    recording.syncStatus == "SYNC_FAILED" -> CrimsonError
                                    else -> AmberPending
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = recording.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${recording.fileName} • ${recording.durationSeconds}s • ${recording.fileSizeMb} MB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Sync Action Button / Badge
                if (recording.isSynced || recording.syncStatus == "CLOUD_SYNCED") {
                    Surface(
                        color = EmeraldSynced.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = EmeraldSynced,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Drive Synchro",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSynced,
                                fontSize = 10.sp
                            )
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDriveUploadClick,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF4285F4)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF4285F4).copy(alpha = 0.5f)),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("direct_gdrive_upload_btn_${recording.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Drive Auth", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onSyncClick,
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("sync_single_file_btn_${recording.id}")
                        ) {
                            if (isSyncing || recording.syncStatus == "SYNCING") {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.8.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("En cours...", fontSize = 10.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rclone Sync", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Rclone Cloud Target Path Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudQueue,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = recording.cloudPath.ifBlank { "gdrive:/Z-CORE/Captures/${recording.fileName}" },
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SyncMetricBox(
    label: String,
    value: String,
    subText: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
fun SyncLogItem(log: com.example.data.model.SyncLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.commandExecuted,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${log.bytesTransferred} • Progression: ${log.progressPercent}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = when (log.status) {
                    "SUCCESS" -> EmeraldSynced.copy(alpha = 0.2f)
                    "IN_PROGRESS" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else -> CrimsonError.copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = log.status,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (log.status) {
                        "SUCCESS" -> EmeraldSynced
                        "IN_PROGRESS" -> MaterialTheme.colorScheme.primary
                        else -> CrimsonError
                    },
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun SyncDashboardSummarySection(
    recordings: List<AudioRecording>,
    modifier: Modifier = Modifier
) {
    val totalFiles = recordings.size
    val totalStorageMb = recordings.sumOf { it.fileSizeMb.toDouble() }

    val pendingRecordings = recordings.filter { !it.isSynced && it.syncStatus != "CLOUD_SYNCED" }
    val pendingCount = pendingRecordings.size
    val pendingStorageMb = pendingRecordings.sumOf { it.fileSizeMb.toDouble() }

    val syncedRecordings = recordings.filter { it.isSynced || it.syncStatus == "CLOUD_SYNCED" }
    val syncedCount = syncedRecordings.size
    val syncedStorageMb = syncedRecordings.sumOf { it.fileSizeMb.toDouble() }

    val syncedRatio = if (totalStorageMb > 0) (syncedStorageMb / totalStorageMb).toFloat() else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sync_dashboard_summary_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.FolderZip,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Résumé Stockage & Synchronisation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Aperçu général du volume audio et des transferts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "$totalFiles audio(s)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Highlight Cards Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Total Storage Used
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("total_storage_card")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Stockage Total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (totalStorageMb >= 1024) {
                                String.format("%.2f GB", totalStorageMb / 1024.0)
                            } else {
                                String.format("%.1f MB", totalStorageMb)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("total_storage_used_text")
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Occupation enregistrements",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }

                // 2. Count of Pending Files
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (pendingCount > 0) AmberPending.copy(alpha = 0.12f) else EmeraldSynced.copy(alpha = 0.12f),
                    border = BorderStroke(
                        1.dp,
                        if (pendingCount > 0) AmberPending.copy(alpha = 0.4f) else EmeraldSynced.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pending_sync_card")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (pendingCount > 0) Icons.Outlined.CloudQueue else Icons.Outlined.CloudDone,
                                contentDescription = null,
                                tint = if (pendingCount > 0) AmberPending else EmeraldSynced,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "En Attente Sync",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "$pendingCount fichier(s)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (pendingCount > 0) AmberPending else EmeraldSynced,
                            modifier = Modifier.testTag("pending_sync_count_text")
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "${String.format("%.1f", pendingStorageMb)} MB à uploader",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Storage Breakdown Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Statut de Synchronisation Storage",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(syncedRatio * 100).toInt()}% Synchronisé",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSynced
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Custom segmented ratio bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (syncedRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(syncedRatio.coerceAtLeast(0.01f))
                                .background(EmeraldSynced)
                        )
                    }
                    if ((1f - syncedRatio) > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight((1f - syncedRatio).coerceAtLeast(0.01f))
                                .background(AmberPending)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(EmeraldSynced, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Drive ($syncedCount - ${String.format("%.1f", syncedStorageMb)} MB)",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(AmberPending, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "En Attente ($pendingCount - ${String.format("%.1f", pendingStorageMb)} MB)",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleAccountCard(
    email: String,
    displayName: String,
    isConnected: Boolean,
    onLoginClick: () -> Unit,
    onTestConnectionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("google_account_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = "Compte Google",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isConnected) (if (displayName.isNotBlank()) displayName else "Utilisateur Google") else "Non Connecté",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (isConnected) EmeraldSynced.copy(alpha = 0.15f) else CrimsonError.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isConnected) "OAuth2 Actif" else "Déconnecté",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isConnected) EmeraldSynced else CrimsonError,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = if (isConnected && email.isNotBlank()) email else "Connectez un compte Google pour la sync Drive",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onLoginClick,
                    modifier = Modifier.testTag("manage_google_account_btn")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Gérer Compte",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onLoginClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("google_login_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Login,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isConnected) "Gérer / Switch Compte" else "Connexion Google",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onTestConnectionClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("test_google_drive_btn")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudDone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tester Token",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun GoogleLoginDialog(
    currentEmail: String,
    currentDisplayName: String,
    isConnected: Boolean,
    onDismiss: () -> Unit,
    onSave: (email: String, displayName: String) -> Unit,
    onDisconnect: () -> Unit
) {
    var emailInput by remember { mutableStateOf(currentEmail) }
    var displayNameInput by remember { mutableStateOf(currentDisplayName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connexion Compte Google Drive")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Renseignez le compte Google associé pour autoriser la synchronisation automatique RClone / Google Drive API (Z-CORE Captures).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Adresse Email Google") },
                    placeholder = { Text("1976ilies@gmail.com") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Email, contentDescription = null)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("google_email_input")
                )

                OutlinedTextField(
                    value = displayNameInput,
                    onValueChange = { displayNameInput = it },
                    label = { Text("Nom d'affichage (Optionnel)") },
                    placeholder = { Text("1976ilies") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Person, contentDescription = null)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OAuth2 Z-CORE SSL Protocol • FIPS-140 & RGPD Compliant. Accès restreint au dossier dédié.",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (emailInput.isNotBlank()) {
                        onSave(emailInput, displayNameInput)
                    }
                },
                modifier = Modifier.testTag("google_login_confirm_btn")
            ) {
                Text("Connexion Google")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isConnected) {
                    TextButton(onClick = onDisconnect) {
                        Text("Déconnexion", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Annuler")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RcloneConfigCard(
    config: com.example.data.sync.RcloneSyncConfig,
    onUpdateConfig: (com.example.data.sync.RcloneSyncConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    var remotePathInput by remember(config) { mutableStateOf(config.remotePath) }
    var bandwidthInput by remember(config) { mutableStateOf(config.bandwidthLimit ?: "") }
    var maxRetriesInput by remember(config) { mutableStateOf(config.maxRetries.toString()) }
    var requireWifiState by remember(config) { mutableStateOf(config.requireWifi) }
    var requireUnmeteredState by remember(config) { mutableStateOf(config.requireUnmeteredNetwork) }
    var dryRunState by remember(config) { mutableStateOf(config.dryRun) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rclone_config_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Stratégie Rclone & Réseau",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Limites bande passante, retries exponentiels, Wi-Fi",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Déplier la configuration"
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Divider(
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Remote Target Path Input
                    OutlinedTextField(
                        value = remotePathInput,
                        onValueChange = { remotePathInput = it },
                        label = { Text("Dossier Google Drive Cible") },
                        placeholder = { Text("/Z-CORE/Captures/") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Bandwidth Limit Input
                        OutlinedTextField(
                            value = bandwidthInput,
                            onValueChange = { bandwidthInput = it },
                            label = { Text("Limite Débit (--bwlimit)") },
                            placeholder = { Text("ex: 10M, 5M") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        )

                        // Max Retries Input
                        OutlinedTextField(
                            value = maxRetriesInput,
                            onValueChange = { maxRetriesInput = it },
                            label = { Text("Max Retries") },
                            placeholder = { Text("3") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Wi-Fi Only Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Wi-Fi Uniquement",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Suspend l'auto-sync sur les données cellulaires",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = requireWifiState,
                            onCheckedChange = { requireWifiState = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Unmetered Network Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Connexion Non Facturée (Unmetered)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Évite les surcoûts sur forfaits limités",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = requireUnmeteredState,
                            onCheckedChange = { requireUnmeteredState = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dry Run Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mode Simulation (--dry-run)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Simule la commande Rclone sans transférer de données",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = dryRunState,
                            onCheckedChange = { dryRunState = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Save Button
                    Button(
                        onClick = {
                            val retries = maxRetriesInput.toIntOrNull() ?: 3
                            val updatedConfig = config.copy(
                                remotePath = if (remotePathInput.isBlank()) "/Z-CORE/Captures/" else remotePathInput,
                                bandwidthLimit = if (bandwidthInput.isBlank()) null else bandwidthInput.trim(),
                                maxRetries = retries,
                                requireWifi = requireWifiState,
                                requireUnmeteredNetwork = requireUnmeteredState,
                                dryRun = dryRunState
                            )
                            onUpdateConfig(updatedConfig)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("save_rclone_config_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sauvegarder la Stratégie Sync Rclone", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleDriveFileManagerCard(
    viewModel: AudioSyncViewModel,
    recordings: List<AudioRecording>,
    uploadState: com.example.data.sync.DriveUploadState,
    remoteFiles: List<com.example.data.sync.DriveRemoteFile>,
    isFetching: Boolean,
    isFr: Boolean,
    modifier: Modifier = Modifier
) {
    var targetFolder by remember { mutableStateOf(com.example.data.sync.GoogleDriveFileManager.DEFAULT_DRIVE_FOLDER) }
    val authState by viewModel.authService.authState.collectAsState()
    val authUserEmail = remember(authState) { viewModel.authService.getCurrentUserEmail() }
    val authUserName = remember(authState) { viewModel.authService.getCurrentUserDisplayName() }

    LaunchedEffect(Unit) {
        viewModel.fetchRemoteDriveFiles(targetFolder)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("google_drive_file_manager_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(
            1.dp,
            Color(0xFF4285F4).copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF4285F4).copy(alpha = 0.15f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.CloudUpload,
                                contentDescription = null,
                                tint = Color(0xFF4285F4),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isFr) "Gestionnaire Fichiers Google Drive" else "Google Drive File Manager",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFF4285F4).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "FIREBASE AUTH",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = Color(0xFF4285F4),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Upload direct API REST v3 • Compte: $authUserEmail",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.fetchRemoteDriveFiles(targetFolder) },
                    modifier = Modifier.testTag("refresh_drive_files_btn")
                ) {
                    if (isFetching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF4285F4)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Actualiser",
                            tint = Color(0xFF4285F4)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Live Upload Progress Tracker
            if (uploadState.isUploading) {
                Surface(
                    color = Color(0xFF4285F4).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF4285F4).copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Téléversement en cours: ${uploadState.currentFileName ?: "Audio"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4285F4)
                            )
                            Text(
                                text = "${uploadState.progressPercent}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4285F4)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { uploadState.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF4285F4),
                            trackColor = Color(0xFF4285F4).copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${uploadState.statusText} • ${uploadState.bytesTransferredText}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            } else if (uploadState.lastUploadedFile != null) {
                Surface(
                    color = EmeraldSynced.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldSynced, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dernier fichier '${uploadState.lastUploadedFile.fileName}' téléversé avec succès !",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldSynced,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (uploadState.errorMessage != null) {
                Surface(
                    color = CrimsonError.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Filled.ErrorOutline, contentDescription = null, tint = CrimsonError, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Erreur Drive: ${uploadState.errorMessage}",
                            style = MaterialTheme.typography.bodySmall,
                            color = CrimsonError
                        )
                    }
                }
            }

            // Target Folder Selector
            OutlinedTextField(
                value = targetFolder,
                onValueChange = { targetFolder = it },
                label = { Text("Dossier Google Drive") },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Folder, contentDescription = null, tint = Color(0xFF4285F4))
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("drive_target_folder_input"),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row: Upload All / Sync Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val unsynced = recordings.filter { !it.isSynced }
                        if (unsynced.isNotEmpty()) {
                            unsynced.forEach { viewModel.uploadAudioFileToGoogleDrive(it, targetFolder) }
                        } else if (recordings.isNotEmpty()) {
                            viewModel.uploadAudioFileToGoogleDrive(recordings.first(), targetFolder)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4285F4)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("upload_all_to_gdrive_btn")
                ) {
                    Icon(imageVector = Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFr) "Téléverser Tout (${recordings.count { !it.isSynced }})" else "Upload All Unsynced",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.fetchRemoteDriveFiles(targetFolder) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.8f)
                        .height(44.dp)
                        .testTag("list_remote_drive_btn")
                ) {
                    Icon(imageVector = Icons.Outlined.FolderShared, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isFr) "Explorer (${remoteFiles.size})" else "Explore (${remoteFiles.size})",
                        fontSize = 12.sp
                    )
                }
            }

            // Remote Google Drive Files List
            if (remoteFiles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Fichiers distants dans '$targetFolder' (${remoteFiles.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    remoteFiles.take(5).forEach { file ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AudioFile,
                                        contentDescription = null,
                                        tint = Color(0xFF4285F4),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${String.format("%.2f", file.sizeBytes / (1024.0 * 1024.0))} MB • ID: ${file.id.take(12)}...",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 9.sp
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.deleteRemoteDriveFile(file) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Supprimer du Drive",
                                        tint = CrimsonError,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


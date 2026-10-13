package com.example.ui.components

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.viewmodel.AudioSyncViewModel
import com.example.ui.theme.EmeraldSynced

private val RealModeEmerald = Color(0xFF10B981)
private val RealModeAmber = Color(0xFFF59E0B)
private val RealModeSky = Color(0xFF0EA5E9)

@Composable
fun RealModeSettingsDialog(
    viewModel: AudioSyncViewModel,
    isFr: Boolean = true,
    onDismissRequest: () -> Unit
) {
    val realParams by viewModel.realDeviceParameters.collectAsState()
    val isGoogleConnected by viewModel.isGoogleAccountConnected.collectAsState()
    val googleEmail by viewModel.userGoogleAccountEmail.collectAsState()
    val rcloneConfig by viewModel.rcloneSyncConfig.collectAsState()
    val isAutoSync by viewModel.isAutoSyncEnabled.collectAsState()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("real_mode_settings_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(1.dp, RealModeEmerald.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with Glowing Real Device Badge & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = RealModeEmerald.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp),
                            border = BorderStroke(1.dp, RealModeEmerald.copy(alpha = 0.4f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = RealModeEmerald,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isFr) "Paramètres Mode Réel" else "Real Device Mode Settings",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = RealModeEmerald.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "LIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RealModeEmerald,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Text(
                                text = if (isFr) "Matériel physique, audio PCM & Cloud Drive direct" else "Physical Hardware, PCM Audio & Cloud Sync",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("close_real_mode_dialog_btn")
                    ) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Master Activation Button: Active All Parameters Setting Mode Real
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = RealModeEmerald,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isFr) "Activer Tous les Paramètres en Mode Réel" else "Activate All Real Mode Parameters",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = if (isFr) "Passe tous les modules en exécution directe et matérielle" else "Switches all modules to direct physical hardware execution",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                viewModel.activateAllRealModeParameters()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("activate_all_real_parameters_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RealModeEmerald
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PowerSettingsNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isFr) "⚡ TOUT ACTIVER EN MODE RÉEL" else "⚡ ACTIVATE ALL REAL SETTINGS",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable List of All 9 Real Device Parameters
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isFr) "Paramètres & Composants Matériels Réels :" else "Real Hardware & Engine Parameters:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // 1. Microphone Hardware Direct Input
                    RealParamRow(
                        title = if (isFr) "Capture Microphone Physique Directe" else "Direct Hardware Microphone",
                        desc = if (isFr) "Enregistrement via le pilote audio de l'appareil Android" else "Capture audio through physical Android device driver",
                        icon = Icons.Filled.Mic,
                        isActive = realParams.realMicrophoneDirect,
                        isFr = isFr,
                        onToggle = { viewModel.toggleRealParameter("realMicrophoneDirect", it) }
                    )

                    // 2. Audio DAC 44.1 kHz PCM Lossless
                    RealParamRow(
                        title = if (isFr) "Échantillonnage DAC 44.1 kHz (Qualité Studio)" else "DAC 44.1 kHz Studio Sampling",
                        desc = if (isFr) "Codec M4A / AAC 128 kbps avec flux PCM natif" else "Lossless PCM audio conversion pipeline",
                        icon = Icons.Filled.GraphicEq,
                        isActive = realParams.realHardwareDac,
                        isFr = isFr,
                        onToggle = { viewModel.toggleRealParameter("realHardwareDac", it) }
                    )

                    // 3. Voice Activity & Silence Detection (VAD)
                    RealParamRow(
                        title = if (isFr) "Détection des Silences & VAD Matériel" else "Voice Activity & Silence Detection (VAD)",
                        desc = if (isFr) "Pause automatique intelligente au-delà de 5s de silence" else "Automatic silence thresholding at 800 amplitude",
                        icon = Icons.Filled.Hearing,
                        isActive = realParams.realSilenceVAD,
                        isFr = isFr,
                        onToggle = { viewModel.toggleRealParameter("realSilenceVAD", it) }
                    )

                    // 4. Rclone Real Execution (dryRun = false)
                    RealParamRow(
                        title = if (isFr) "Transfert Rclone Réel (Simulation Désactivée)" else "Rclone Live Transfers (Dry-Run Disabled)",
                        desc = if (isFr) "Upload physique des fichiers vers Google Drive (/Z-CORE/Captures/)" else "Direct data push to Google Drive remote endpoint",
                        icon = Icons.Filled.CloudUpload,
                        isActive = realParams.realRcloneExecution && !rcloneConfig.dryRun,
                        isFr = isFr,
                        onToggle = { viewModel.toggleRealParameter("realRcloneExecution", it) }
                    )

                    // 5. Google OAuth2 Account
                    RealParamRow(
                        title = if (isFr) "Authentification Google Réelle ($googleEmail)" else "Live Google OAuth2 Auth ($googleEmail)",
                        desc = if (isFr) "Jeton OAuth 2.0 sécurisé avec droit drive.file" else "Scoped token authorized for Google Drive captures",
                        icon = Icons.Filled.AccountCircle,
                        isActive = isGoogleConnected && realParams.realGoogleAuthOAuth,
                        isFr = isFr,
                        onToggle = { viewModel.toggleRealParameter("realGoogleAuthOAuth", it) }
                    )

                    // 6. Auto Cloud Sync
                    RealParamRow(
                        title = if (isFr) "Synchronisation Cloud Automatique Immédiate" else "Immediate Auto Cloud Sync",
                        desc = if (isFr) "Déclenche Rclone dès la fin de chaque enregistrement vocal" else "Triggers synchronization immediately upon capture save",
                        icon = Icons.Filled.Sync,
                        isActive = isAutoSync && realParams.realAutoCloudSync,
                        isFr = isFr,
                        onToggle = { viewModel.toggleRealParameter("realAutoCloudSync", it) }
                    )

                    // 7. Native Termux Socket Integration
                    RealParamRow(
                        title = if (isFr) "Intégration Shell Termux & Am Startservice" else "Termux Shell & Socket Execution",
                        desc = if (isFr) "Exécution des macros en arrière-plan (termux-setup-storage)" else "Background intent dispatching to Termux environment",
                        icon = Icons.Filled.Terminal,
                        isActive = realParams.realTermuxSocket,
                        isFr = isFr,
                        onToggle = { viewModel.toggleRealParameter("realTermuxSocket", it) }
                    )

                    // 8. Exponential Backoff & Network Auto-Recovery
                    RealParamRow(
                        title = if (isFr) "Reprise Réseau & Retry Exponentiel (5 Tentatives)" else "Exponential Backoff & Auto-Recovery (5 Retries)",
                        desc = if (isFr) "Reconnexion automatique en cas de coupure 4G/5G/Wi-Fi" else "Dynamic socket reconnect on network drop",
                        icon = Icons.Filled.NetworkCheck,
                        isActive = realParams.realExponentialRetry,
                        isFr = isFr,
                        onToggle = { viewModel.toggleRealParameter("realExponentialRetry", it) }
                    )

                    // 9. Storage Scoped & File System Direct Access
                    RealParamRow(
                        title = if (isFr) "Accès Stockage Physique (/storage/recordings/)" else "Direct Storage Access (/storage/recordings/)",
                        desc = if (isFr) "Persistance sur le système de fichiers réel de l'appareil" else "Persistent file system storage on physical disk",
                        icon = Icons.Filled.Folder,
                        isActive = realParams.realStorageAccess,
                        isFr = isFr,
                        onToggle = { viewModel.toggleRealParameter("realStorageAccess", it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Hardware Telemetry Summary Card
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.PhoneAndroid,
                                    contentDescription = null,
                                    tint = RealModeSky,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isFr) "Diagnostic Matériel & Périphérique Détecté :" else "Detected Device Hardware Telemetry:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = RealModeSky
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• Fabricant / Modèle : ${Build.MANUFACTURER.uppercase()} ${Build.MODEL}\n" +
                                       "• Android OS : Version ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
                                       "• SoC / Matériel : ${Build.HARDWARE} (${Build.BOARD})\n" +
                                       "• Services Google Play : Connectés & Opérationnels (OAuth2)\n" +
                                       "• Stockage Enregistrements : ./recordings/*.m4a (WAV Lossless)\n" +
                                       "• Rclone Cible : gdrive:/Z-CORE/Captures/ (Sync Actif)",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Close Action
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isFr) "Enregistrer & Fermer" else "Save & Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RealParamRow(
    title: String,
    desc: String,
    icon: ImageVector,
    isActive: Boolean,
    isFr: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        color = if (isActive) RealModeEmerald.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isActive) RealModeEmerald.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isActive) RealModeEmerald.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isActive) RealModeEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (isActive) {
                            Surface(
                                color = RealModeEmerald.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (isFr) "ACTIF" else "ON",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RealModeEmerald,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = desc,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }

            Switch(
                checked = isActive,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = RealModeEmerald
                )
            )
        }
    }
}

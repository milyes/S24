package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.viewmodel.AudioSyncViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IA22SessionCard(
    viewModel: AudioSyncViewModel,
    isFr: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sessionState by viewModel.ia22SessionStatus.collectAsState()
    var showScriptDialog by remember { mutableStateOf(false) }
    var showEditUrlDialog by remember { mutableStateOf(false) }
    var editUrlInput by remember { mutableStateOf(sessionState.tunnelUrl) }
    var isPinging by remember { mutableStateOf(false) }

    val formattedTime = remember(sessionState.lastUpdatedTimestamp) {
        val sdf = SimpleDateFormat("HH:mm:ss - dd/MM/yyyy", Locale.getDefault())
        sdf.format(Date(sessionState.lastUpdatedTimestamp))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ia22_session_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (sessionState.isSessionActive) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Node Badge and Status Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (sessionState.isSessionActive)
                                    Color(0xFF10B981).copy(alpha = 0.15f)
                                else
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Terminal,
                            contentDescription = null,
                            tint = if (sessionState.isSessionActive) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (isFr) "Nœud Maître IA22 (S24)" else "IA22 Master Node (S24)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Opérateur: ${sessionState.operatorName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Live Active Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (sessionState.isSessionActive)
                        Color(0xFF10B981).copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (sessionState.isSessionActive) Color(0xFF10B981) else MaterialTheme.colorScheme.outline
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (sessionState.isSessionActive) Color(0xFF10B981) else Color.Gray)
                        )
                        Text(
                            text = if (sessionState.isSessionActive)
                                (if (isFr) "ACTIF" else "ACTIVE")
                            else
                                (if (isFr) "EN ATTENTE" else "STANDBY"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (sessionState.isSessionActive) Color(0xFF10B981) else Color.Gray
                        )
                    }
                }
            }

            // Cloudflare Tunnel Live Card Section
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (sessionState.isTunnelActive) Color(0xFF00E5FF).copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudSync,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Tunnel Cloudflare (979V11)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (sessionState.isTunnelActive) Color(0xFF10B981).copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (sessionState.isTunnelActive) "● LIVE (QUIC 98%)" else "INACTIF",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (sessionState.isTunnelActive) Color(0xFF10B981) else Color.Gray
                            )
                        }
                    }

                    // URL Box
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sessionState.tunnelUrl,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF00E5FF),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Cloudflare Tunnel URL", sessionState.tunnelUrl)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, if (isFr) "URL copiée !" else "URL copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copier", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Text(
                        text = "Statut : ${sessionState.tunnelStatus}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Quick links and Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                isPinging = true
                                viewModel.pingIa22Tunnel { success, code, latency ->
                                    isPinging = false
                                    val msg = if (success) {
                                        if (isFr) "Tunnel en ligne (HTTP $code - $latency) !" else "Tunnel online (HTTP $code - $latency)!"
                                    } else {
                                        if (isFr) "Échec ping tunnel ($code)" else "Tunnel ping failed ($code)"
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            if (isPinging) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.NetworkPing, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                            Spacer(Modifier.width(4.dp))
                            Text("Ping", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("${sessionState.tunnelUrl}/logic_cover.html"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Erreur ouverture: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Logic Cover", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("${sessionState.tunnelUrl}/index_usb.html"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Erreur ouverture: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("USB Live", fontSize = 11.sp)
                        }

                        IconButton(
                            onClick = {
                                editUrlInput = sessionState.tunnelUrl
                                showEditUrlDialog = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Modifier", modifier = Modifier.size(16.dp))
                        }
                    }

                    // Gatekeeper Mot-Clé Sovereign
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.Key, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                                Text(
                                    text = "Gatekeeper: 979V11 (Loi 25 / ANPDP v11)",
                                    fontSize = 11.sp,
                                    color = Color(0xFFE2E8F0),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "COPIER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF),
                                modifier = Modifier.clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Gatekeeper Key", "979V11"))
                                    Toast.makeText(context, "Clé 979V11 copiée !", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // Session Details Section
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isFr) "Dernier message reçu :" else "Last message received:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Text(
                        text = sessionState.lastBroadcastMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Medium,
                        color = if (sessionState.isSessionActive) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.triggerIa22AdbBroadcast()
                        Toast.makeText(context, if (isFr) "Simulation ADB transmise !" else "ADB simulation sent!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isFr) "Simuler ADB" else "Simulate ADB", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = {
                        viewModel.wipeIa22SessionTraces()
                        Toast.makeText(context, if (isFr) "Traces de session purgées !" else "Session traces cleared!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isFr) "Purger Traces" else "Wipe Traces", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Batch Script Viewer button
            FilledTonalButton(
                onClick = { showScriptDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isFr) "Voir Script Windows Batch (.BAT)" else "View Windows Batch Script (.BAT)",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    if (showEditUrlDialog) {
        AlertDialog(
            onDismissRequest = { showEditUrlDialog = false },
            title = {
                Text(if (isFr) "Modifier Tunnel Cloudflare" else "Edit Cloudflare Tunnel")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isFr) "Entrez l'URL publique Cloudflare active :" else "Enter the active public Cloudflare URL:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = editUrlInput,
                        onValueChange = { editUrlInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("https://xxx.trycloudflare.com") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editUrlInput.isNotBlank()) {
                            viewModel.setIa22TunnelUrl(editUrlInput.trim())
                            Toast.makeText(context, if (isFr) "URL Tunnel mise à jour !" else "Tunnel URL updated!", Toast.LENGTH_SHORT).show()
                            showEditUrlDialog = false
                        }
                    }
                ) {
                    Text(if (isFr) "Enregistrer" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditUrlDialog = false }) {
                    Text(if (isFr) "Annuler" else "Cancel")
                }
            }
        )
    }

    if (showScriptDialog) {
        AlertDialog(
            onDismissRequest = { showScriptDialog = false },
            title = {
                Text(
                    text = if (isFr) "Script Windows Batch (.BAT) - Nœud S24" else "Windows Batch Script (.BAT) - S24 Node",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isFr) 
                            "Ce script Windows automatise le déclenchement ADB sur votre S24 :"
                        else 
                            "This Windows script automates ADB triggers on your S24:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = viewModel.getIa22WindowsBatchScript(),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.padding(12.dp),
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("IA22 Windows Batch Script", viewModel.getIa22WindowsBatchScript())
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, if (isFr) "Script copié dans le presse-papiers !" else "Script copied to clipboard!", Toast.LENGTH_SHORT).show()
                        showScriptDialog = false
                    }
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isFr) "Copier" else "Copy")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScriptDialog = false }) {
                    Text(if (isFr) "Fermer" else "Close")
                }
            }
        )
    }
}

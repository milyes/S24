package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.viewmodel.AudioSyncViewModel
import com.example.ui.components.NetsecureProLicenseCard
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiStudioScreen(
    viewModel: AudioSyncViewModel,
    modifier: Modifier = Modifier
) {
    val recordings by viewModel.recordings.collectAsState()
    val isAnalyzing by viewModel.isAiAnalyzing.collectAsState()
    val aiResult by viewModel.aiAnalysisResult.collectAsState()

    var selectedRecordingId by remember { mutableStateOf<Long?>(null) }
    var promptInput by remember { mutableStateOf("Analyse la structure et les points clés de cette capture vocale Z-CORE.") }

    val selectedRecording = recordings.find { it.id == selectedRecordingId } ?: recordings.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Gemini AI Audio Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Analyse, transcription & génération de scripts vocaux",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Official NETSECUREPRO IA SIM & eSIM License Card
        NetsecureProLicenseCard(isInitiallyExpanded = false)

        Spacer(modifier = Modifier.height(16.dp))

        // Target File Selection
        if (recordings.isNotEmpty()) {
            Text(
                text = "Sélectionner un fichier audio Z-CORE:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            var dropdownExpanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedRecording?.let { "${it.title} (${it.fileName})" } ?: "Aucun fichier",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("ai_file_select_dropdown")
                )

                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    recordings.forEach { rec ->
                        DropdownMenuItem(
                            text = { Text("${rec.title} - ${rec.fileName}") },
                            onClick = {
                                selectedRecordingId = rec.id
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Prompt Input Field
        OutlinedTextField(
            value = promptInput,
            onValueChange = { promptInput = it },
            label = { Text("Instruction Gemini AI") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_prompt_input"),
            minLines = 2,
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                selectedRecording?.let { viewModel.analyzeRecordingWithAi(it, promptInput) }
            },
            enabled = !isAnalyzing && selectedRecording != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("generate_ai_btn"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onSecondary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyse Gemini en cours...")
            } else {
                Icon(imageVector = Icons.Outlined.Psychology, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Générer la Synthèse IA", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Result Card
        Text(
            text = "Résultat de la Synthèse IA:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                val currentText = aiResult ?: selectedRecording?.aiSummary ?: "Cliquez sur 'Générer la Synthèse IA' pour lancer l'analyse intelligente de votre fichier audio."

                LazyColumn {
                    item {
                        Text(
                            text = currentText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}

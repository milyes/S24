package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.macro.PredefinedTermuxScript
import com.example.data.macro.ScriptArgument
import com.example.data.macro.TermuxExecutionResult
import com.example.data.macro.TermuxScriptCategory
import com.example.data.viewmodel.AudioSyncViewModel
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredefinedScriptsSection(
    viewModel: AudioSyncViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val isFr = language == AppLanguage.FRENCH

    val scripts by viewModel.predefinedScripts.collectAsState()
    val lastResult by viewModel.lastTermuxExecutionResult.collectAsState()
    val isTermuxInstalled by viewModel.isTermuxAppInstalled.collectAsState()
    val isTermuxApiInstalled by viewModel.isTermuxApiAppInstalled.collectAsState()

    var selectedCategoryFilter by remember { mutableStateOf<TermuxScriptCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Dialog state for customizing script arguments before intent execution
    var activeScriptForArgs by remember { mutableStateOf<PredefinedTermuxScript?>(null) }
    var activeScriptCustomArgs by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Dialog state for full script source preview
    var previewingScript by remember { mutableStateOf<PredefinedTermuxScript?>(null) }

    val filteredScripts = remember(scripts, selectedCategoryFilter, searchQuery) {
        scripts.filter { script ->
            val matchesCategory = selectedCategoryFilter == null || script.category == selectedCategoryFilter
            val matchesSearch = searchQuery.isBlank() ||
                    script.titleFr.contains(searchQuery, ignoreCase = true) ||
                    script.titleEn.contains(searchQuery, ignoreCase = true) ||
                    script.descriptionFr.contains(searchQuery, ignoreCase = true) ||
                    script.descriptionEn.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Status & Termux Environment Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("termux_intent_env_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFr) "Moteur d'Intents Termux (RUN_COMMAND)" else "Termux Intent Engine (RUN_COMMAND)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isTermuxInstalled) Color(0xFF10B981).copy(alpha = 0.18f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (isTermuxInstalled) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                        RoundedCornerShape(3.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isTermuxInstalled) "com.termux OK" else if (isFr) "Termux Non Détecté" else "Termux Not Detected",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isTermuxInstalled) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isFr)
                        "Déclenchement direct de scripts bash via les intents Android sans friction."
                    else
                        "Direct dispatch of bash scripts via Android Intent system seamlessly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                // Last Execution Result Feedback Badge
                lastResult?.let { res ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (res.success) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (res.success) Icons.Filled.CheckCircle else Icons.Filled.Error,
                                contentDescription = null,
                                tint = if (res.success) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${res.scriptTitle} (${res.formattedTime})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = res.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar for Scripts
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(if (isFr) "Rechercher un script pré-défini..." else "Search predefined script...") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("predefined_script_search_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Category Filter Chips
        ScrollableTabRow(
            selectedTabIndex = when (selectedCategoryFilter) {
                null -> 0
                TermuxScriptCategory.RCLONE_CLOUD -> 1
                TermuxScriptCategory.AUDIO_PROCESSING -> 2
                TermuxScriptCategory.STORAGE_SYSTEM -> 3
                TermuxScriptCategory.TELEMETRY_GHOST -> 4
                TermuxScriptCategory.DIAGNOSTICS -> 5
                TermuxScriptCategory.TERMUX_API -> 6
            },
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            FilterChip(
                selected = selectedCategoryFilter == null,
                onClick = { selectedCategoryFilter = null },
                label = { Text(if (isFr) "Tous (${scripts.size})" else "All (${scripts.size})") },
                modifier = Modifier.padding(end = 6.dp)
            )

            TermuxScriptCategory.values().forEach { cat ->
                FilterChip(
                    selected = selectedCategoryFilter == cat,
                    onClick = { selectedCategoryFilter = cat },
                    label = { Text(if (isFr) cat.labelFr else cat.labelEn) },
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Predefined Scripts List
        if (filteredScripts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isFr) "Aucun script correspondant." else "No matching scripts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredScripts, key = { it.id }) { script ->
                    PredefinedScriptCard(
                        script = script,
                        isFr = isFr,
                        onTriggerBackgroundIntent = {
                            viewModel.executePredefinedScript(script, emptyMap(), runInBackground = true)
                        },
                        onTriggerTerminalIntent = {
                            viewModel.executePredefinedScript(script, emptyMap(), runInBackground = false)
                        },
                        onConfigureArgs = {
                            val initial = mutableMapOf<String, String>()
                            script.arguments.forEach { initial[it.key] = it.defaultValue }
                            activeScriptCustomArgs = initial
                            activeScriptForArgs = script
                        },
                        onPreviewCode = {
                            previewingScript = script
                        },
                        onCopyCode = {
                            val code = viewModel.getScriptGeneratedCode(script, emptyMap())
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Script Bash", code)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, if (isFr) "Script copié dans le presse-papiers !" else "Script copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Configure Script Arguments Modal Dialog
    activeScriptForArgs?.let { script ->
        ConfigureScriptArgsDialog(
            script = script,
            isFr = isFr,
            currentArgs = activeScriptCustomArgs,
            onArgsChanged = { activeScriptCustomArgs = it },
            onExecute = { bgMode ->
                viewModel.executePredefinedScript(script, activeScriptCustomArgs, runInBackground = bgMode)
                activeScriptForArgs = null
            },
            onDismiss = { activeScriptForArgs = null }
        )
    }

    // Preview Full Script Source Dialog
    previewingScript?.let { script ->
        val code = viewModel.getScriptGeneratedCode(script, emptyMap())
        AlertDialog(
            onDismissRequest = { previewingScript = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFr) script.titleFr else script.titleEn,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = if (isFr) "Code source Bash exécuté via Intent :" else "Bash source code executed via Intent:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                    ) {
                        Text(
                            text = code,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF4ADE80),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Script Bash", code)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, if (isFr) "Script copié !" else "Script copied!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isFr) "Copier" else "Copy")
                }
            },
            dismissButton = {
                TextButton(onClick = { previewingScript = null }) {
                    Text(if (isFr) "Fermer" else "Close")
                }
            }
        )
    }
}

@Composable
fun PredefinedScriptCard(
    script: PredefinedTermuxScript,
    isFr: Boolean,
    onTriggerBackgroundIntent: () -> Unit,
    onTriggerTerminalIntent: () -> Unit,
    onConfigureArgs: () -> Unit,
    onPreviewCode: () -> Unit,
    onCopyCode: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("script_card_${script.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Category Badge + Title + Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isFr) script.category.labelFr else script.category.labelEn,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFr) script.titleFr else script.titleEn,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPreviewCode,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = "Voir le code",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onCopyCode,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copier",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                text = if (isFr) script.descriptionFr else script.descriptionEn,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 12.sp
            )

            // Arguments summary if any
            if (script.arguments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isFr)
                            "${script.arguments.size} paramètre(s) configurable(s)"
                        else
                            "${script.arguments.size} configurable parameter(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row: Trigger via Android Intent
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary: Run Background Intent
                Button(
                    onClick = onTriggerBackgroundIntent,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(38.dp)
                        .testTag("trigger_intent_bg_btn_${script.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isFr) "Exécuter Intent" else "Run Intent",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // If script has arguments: Configure button
                if (script.arguments.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onConfigureArgs,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("configure_args_btn_${script.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isFr) "Régler" else "Args",
                            fontSize = 11.sp
                        )
                    }
                }

                // Terminal window mode intent
                OutlinedButton(
                    onClick = onTriggerTerminalIntent,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("trigger_intent_term_btn_${script.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Terminal,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isFr) "Terminal" else "Terminal",
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ConfigureScriptArgsDialog(
    script: PredefinedTermuxScript,
    isFr: Boolean,
    currentArgs: Map<String, String>,
    onArgsChanged: (Map<String, String>) -> Unit,
    onExecute: (runInBackground: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var localArgs by remember { mutableStateOf(currentArgs.toMutableMap()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isFr) "Paramètres : ${script.titleFr}" else "Parameters: ${script.titleEn}",
                    style = MaterialTheme.typography.titleSmall
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isFr)
                        "Personnalisez les variables avant d'envoyer l'intent Termux :"
                    else
                        "Customize variables before dispatching Termux Intent:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                script.arguments.forEach { argDef ->
                    val currentValue = localArgs[argDef.key] ?: argDef.defaultValue

                    Column {
                        Text(
                            text = if (isFr) argDef.labelFr else argDef.labelEn,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        OutlinedTextField(
                            value = currentValue,
                            onValueChange = { newVal ->
                                val updated = HashMap(localArgs)
                                updated[argDef.key] = newVal
                                localArgs = updated
                                onArgsChanged(updated)
                            },
                            placeholder = { Text(argDef.defaultValue) },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("arg_input_${argDef.key}"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Text(
                            text = if (isFr) argDef.descriptionFr else argDef.descriptionEn,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 1.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onExecute(false) }
                ) {
                    Text(if (isFr) "Terminal" else "Terminal", fontSize = 11.sp)
                }

                Button(
                    onClick = { onExecute(true) }
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isFr) "Lancer (Arrière-Plan)" else "Run (Background)", fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isFr) "Annuler" else "Cancel")
            }
        }
    )
}

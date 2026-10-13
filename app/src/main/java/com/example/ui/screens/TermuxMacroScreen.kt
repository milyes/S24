package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommandMacro
import com.example.data.viewmodel.AudioSyncViewModel
import com.example.ui.components.IA22SessionCard
import com.example.ui.components.PredefinedScriptsSection
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermuxMacroScreen(
    viewModel: AudioSyncViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val macros by viewModel.macros.collectAsState()

    var activeSectionTab by remember { mutableStateOf(0) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showTermuxGuideDialog by remember { mutableStateOf(false) }

    // State for Ad-hoc Quick Command execution
    var customShellCommand by remember { mutableStateOf("") }

    // Dialog state for Creating / Editing Macro
    var showMacroDialog by remember { mutableStateOf(false) }
    var editingMacro by remember { mutableStateOf<CommandMacro?>(null) }
    var macroNameInput by remember { mutableStateOf("") }
    var macroCommandInput by remember { mutableStateOf("") }
    var macroCategoryInput by remember { mutableStateOf("TERMUX") }
    var macroDescriptionInput by remember { mutableStateOf("") }

    fun openCreateMacroDialog(prefilledCommand: String = "") {
        editingMacro = null
        macroNameInput = ""
        macroCommandInput = prefilledCommand
        macroCategoryInput = "TERMUX"
        macroDescriptionInput = ""
        showMacroDialog = true
    }

    fun openEditMacroDialog(macro: CommandMacro) {
        editingMacro = macro
        macroNameInput = macro.name
        macroCommandInput = macro.commandText
        macroCategoryInput = macro.category
        macroDescriptionInput = macro.description
        showMacroDialog = true
    }

    val filteredMacros = remember(macros, selectedCategory, searchQuery) {
        macros.filter { macro ->
            val matchesCategory = (selectedCategory == "ALL" || macro.category == selectedCategory)
            val matchesSearch = searchQuery.isBlank() ||
                    macro.name.contains(searchQuery, ignoreCase = true) ||
                    macro.commandText.contains(searchQuery, ignoreCase = true) ||
                    macro.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = language.tr("Éditeur & Macro Shell Termux / ADB", "Termux / ADB Shell Macro Editor"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = language.tr("Créez, éditez, sauvegardez et exécutez vos commandes shell", "Create, edit, save and execute your shell commands"),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { openCreateMacroDialog() },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_macro_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = language.tr("Créer Macro", "Create Macro"),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(language.tr("Créer", "New"), fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { showTermuxGuideDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(language.tr("Guide", "Guide"), fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Target Phone Number Display Badge
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhoneAndroid,
                                contentDescription = "Terminal mobile cible",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Terminal Cible Trailer Ghost",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "Réseau Fido",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Cellulaire: +1 438 985-5041",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Target Phone", "+14389855041 (Réseau Fido - Trailer Ghost)")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Numéro & Réseau Fido copié: +1 438 985-5041", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copier le numéro",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Tab Switcher between Predefined Scripts and Custom Macros / Console
        TabRow(
            selectedTabIndex = activeSectionTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = activeSectionTab == 0,
                onClick = { activeSectionTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = language.tr("Scripts Pré-définis (Intents)", "Predefined Scripts (Intents)"),
                            fontWeight = if (activeSectionTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                },
                modifier = Modifier.testTag("tab_predefined_scripts")
            )
            Tab(
                selected = activeSectionTab == 1,
                onClick = { activeSectionTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "IA22 & Windows S24",
                            fontWeight = if (activeSectionTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                },
                modifier = Modifier.testTag("tab_ia22_session")
            )
            Tab(
                selected = activeSectionTab == 2,
                onClick = { activeSectionTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = language.tr("Macros & Console Shell", "Macros & Shell Console"),
                            fontWeight = if (activeSectionTab == 2) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                },
                modifier = Modifier.testTag("tab_custom_macros")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (activeSectionTab) {
            0 -> {
                // === PREDEFINED SCRIPTS (INTENT SYSTEM) ===
                PredefinedScriptsSection(
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
            }
            1 -> {
                // === IA22 & WINDOWS S24 MASTER NODE ===
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    item {
                        IA22SessionCard(
                            viewModel = viewModel,
                            isFr = language == com.example.ui.theme.AppLanguage.FRENCH
                        )
                    }
                    item {
                        // IA22 Macros Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Commandes IA22 Disponibles (Nœud S24)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Ces macros s'intègrent directement au script batch Windows exécuté sur la machine hôte.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                val ia22Macros = macros.filter { it.category == "IA22" || it.name.contains("IA22", ignoreCase = true) }
                                if (ia22Macros.isEmpty()) {
                                    Text(
                                        text = "Aucune macro IA22 personnalisée trouvée.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    ia22Macros.forEach { macro ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = macro.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = macro.commandText,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                                FilledTonalButton(
                                                    onClick = { viewModel.macroManager.executeMacro(macro) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Exécuter", fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                // === CUSTOM MACROS & AD-HOC CONSOLE ===
                Column(modifier = Modifier.weight(1f)) {
                // Ad-Hoc Shell Command Execution Console Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = language.tr("⚡ Exécution Rapide de Commande Shell", "⚡ Quick Shell Command Execution"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customShellCommand,
                                onValueChange = { customShellCommand = it },
                                placeholder = { Text(language.tr("Saisir une commande shell (ex: rclone ls gdrive:)", "Enter shell command (ex: rclone ls gdrive:)")) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("adhoc_command_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    if (customShellCommand.isNotBlank()) {
                                        viewModel.executeRawShellCommand(customShellCommand)
                                    } else {
                                        Toast.makeText(context, language.tr("Veuillez entrer une commande.", "Please enter a command."), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .height(56.dp)
                                    .testTag("adhoc_command_run_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = language.tr("Exécuter", "Run")
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(language.tr("Run", "Run"), fontWeight = FontWeight.Bold)
                            }
                        }

                        if (customShellCommand.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = { openCreateMacroDialog(customShellCommand) },
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .testTag("save_adhoc_as_macro_btn")
                            ) {
                                Icon(Icons.Outlined.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(language.tr("Enregistrer comme Macro", "Save as Macro"), fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar & Category Filters Row
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(language.tr("Rechercher un macro par nom ou commande...", "Search macro by name or command...")) },
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
                        .testTag("macro_search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                ScrollableTabRow(
                    selectedTabIndex = when (selectedCategory) {
                        "ALL" -> 0
                        "RCLONE" -> 1
                        "TERMUX" -> 2
                        "ADB" -> 3
                        "Z_GHOST" -> 4
                        "CUSTOM" -> 5
                        else -> 0
                    },
                    edgePadding = 0.dp,
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    val categories = listOf("ALL", "RCLONE", "TERMUX", "ADB", "Z_GHOST", "CUSTOM")
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(if (cat == "ALL") language.tr("Tous", "All") else cat) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Macros List with explicit Run Button & Edit actions
                if (filteredMacros.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.Code,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = language.tr("Aucun macro trouvé", "No macros found"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredMacros, key = { it.id }) { macro ->
                            MacroCard(
                                macro = macro,
                                language = language,
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Command", macro.commandText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, language.tr("Commande copiée: ${macro.name}", "Command copied: ${macro.name}"), Toast.LENGTH_SHORT).show()
                                },
                                onExecute = { viewModel.executeMacroViaTermuxIntent(macro) },
                                onEdit = { openEditMacroDialog(macro) },
                                onFavoriteToggle = { viewModel.toggleFavoriteMacro(macro) },
                                onDelete = { viewModel.deleteMacro(macro) }
                            )
                        }
                    }
                }
            }
        }
    }
}

    // Macro Create / Edit Dialog
    if (showMacroDialog) {
        val isEditing = editingMacro != null

        AlertDialog(
            onDismissRequest = { showMacroDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isEditing) Icons.Outlined.Edit else Icons.Outlined.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isEditing)
                            language.tr("Éditer la Macro", "Edit Macro")
                        else
                            language.tr("Créer une Macro Custom", "Create Custom Macro")
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = macroNameInput,
                        onValueChange = { macroNameInput = it },
                        label = { Text(language.tr("Nom de la macro", "Macro name")) },
                        placeholder = { Text("ex: Rclone Sync Auto") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("macro_name_input")
                    )

                    OutlinedTextField(
                        value = macroCommandInput,
                        onValueChange = { macroCommandInput = it },
                        label = { Text(language.tr("Commande Shell Termux", "Termux Shell Command")) },
                        placeholder = { Text("ex: rclone copy ./storage/ gdrive:/") },
                        singleLine = false,
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("macro_command_input")
                    )

                    OutlinedTextField(
                        value = macroCategoryInput,
                        onValueChange = { macroCategoryInput = it },
                        label = { Text(language.tr("Catégorie (RCLONE, TERMUX, ADB, Z_GHOST, CUSTOM)", "Category")) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("macro_category_input")
                    )

                    OutlinedTextField(
                        value = macroDescriptionInput,
                        onValueChange = { macroDescriptionInput = it },
                        label = { Text(language.tr("Description (Optionnel)", "Description (Optional)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (macroNameInput.isNotBlank() && macroCommandInput.isNotBlank()) {
                            if (isEditing && editingMacro != null) {
                                viewModel.updateMacro(
                                    editingMacro!!.copy(
                                        name = macroNameInput.trim(),
                                        commandText = macroCommandInput.trim(),
                                        category = macroCategoryInput.ifBlank { "TERMUX" }.trim().uppercase(),
                                        description = macroDescriptionInput.trim()
                                    )
                                )
                            } else {
                                viewModel.saveCustomMacro(
                                    name = macroNameInput,
                                    commandText = macroCommandInput,
                                    category = macroCategoryInput.ifBlank { "TERMUX" },
                                    description = macroDescriptionInput
                                )
                            }
                            showMacroDialog = false
                        } else {
                            Toast.makeText(context, language.tr("Veuillez saisir un nom et une commande.", "Please enter a name and a command."), Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("save_macro_confirm_btn")
                ) {
                    Text(if (isEditing) language.tr("Mettre à jour", "Update") else language.tr("Sauvegarder", "Save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showMacroDialog = false }) {
                    Text(language.tr("Annuler", "Cancel"))
                }
            }
        )
    }

    // Termux Info Guide Dialog
    if (showTermuxGuideDialog) {
        AlertDialog(
            onDismissRequest = { showTermuxGuideDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(language.tr("Télécharger Termux Officiel", "Download Official Termux"))
                }
            },
            text = {
                Column {
                    Text(
                        text = language.tr(
                            "Important: Ne téléchargez PAS Termux depuis Google Play Store (version obsolète depuis 2020).\n",
                            "Important: Do NOT download Termux from Google Play Store (outdated since 2020).\n"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sources officielles recommandées:\n" +
                                "1. F-Droid (Recommandé):\n   f-droid.org/packages/com.termux\n\n" +
                                "2. GitHub Officiel Releases:\n   github.com/termux/termux-app/releases\n\n" +
                                "Après installation, installez le paquet API avec:\n" +
                                "pkg update && pkg install termux-api",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTermuxGuideDialog = false }) {
                    Text(language.tr("Fermer", "Close"))
                }
            }
        )
    }
}

@Composable
fun MacroCard(
    macro: CommandMacro,
    language: com.example.ui.theme.AppLanguage,
    onCopy: () -> Unit,
    onExecute: () -> Unit,
    onEdit: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("macro_card_${macro.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            text = macro.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = macro.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("edit_macro_btn_${macro.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = language.tr("Éditer Macro", "Edit Macro"),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (macro.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favori",
                            tint = if (macro.isFavorite) MaterialTheme.colorScheme.primary else TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Supprimer Macro",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (macro.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = macro.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
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
                        text = macro.commandText,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Prominent RUN button with text label
                        Button(
                            onClick = onExecute,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("execute_macro_intent_btn_${macro.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = language.tr("Run", "Run"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("copy_macro_btn_${macro.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = language.tr("Copier", "Copy"),
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Contact
import com.example.data.viewmodel.AudioSyncViewModel
import com.example.ui.theme.AppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: AudioSyncViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contacts by viewModel.contacts.collectAsState()
    val searchQuery by viewModel.contactsSearchQuery.collectAsState()
    val starredOnly by viewModel.filterStarredOnly.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isFr = appLanguage == AppLanguage.FRENCH

    var showAddDialog by remember { mutableStateOf(false) }
    var showImportCsvDialog by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<Contact?>(null) }
    var contactToDelete by remember { mutableStateOf<Contact?>(null) }

    val filteredContacts = remember(contacts, searchQuery, starredOnly) {
        contacts.filter { contact ->
            val matchesStarred = !starredOnly || contact.isStarred
            val matchesSearch = searchQuery.isBlank() ||
                    contact.displayName.contains(searchQuery, ignoreCase = true) ||
                    contact.phoneNumber.contains(searchQuery, ignoreCase = true) ||
                    contact.organization.contains(searchQuery, ignoreCase = true) ||
                    contact.addressFormatted.contains(searchQuery, ignoreCase = true) ||
                    contact.city.contains(searchQuery, ignoreCase = true) ||
                    contact.notes.contains(searchQuery, ignoreCase = true)
            matchesStarred && matchesSearch
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("contacts_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // --- Header Section ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contacts_header_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
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
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Contacts,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isFr) "Contacts & Destinataires" else "Contacts & Recipients",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isFr) "Répertoire, partage vocal & liens CanadaPages" else "Directory, voice sharing & CanadaPages profiles",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Badge count
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "${contacts.size}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons: Add Contact & Import CSV
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("add_contact_btn")
                        ) {
                            Icon(imageVector = Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isFr) "Nouveau" else "New Contact", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showImportCsvDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(42.dp)
                                .testTag("import_csv_btn")
                        ) {
                            Icon(imageVector = Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isFr) "Importer CSV" else "Import CSV", fontSize = 12.sp)
                        }

                        IconButton(
                            onClick = { viewModel.restoreDefaultContact() },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("restore_default_contact_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Restore,
                                contentDescription = "Restaurer Nesrin Ouis",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // --- Search & Filters ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateContactsSearchQuery(it) },
                    placeholder = { Text(if (isFr) "Rechercher Nesrin, téléphone, ville..." else "Search name, phone, city...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateContactsSearchQuery("") }) {
                                Icon(imageVector = Icons.Filled.Clear, contentDescription = "Effacer")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("contacts_search_input")
                )

                FilterChip(
                    selected = starredOnly,
                    onClick = { viewModel.toggleFilterStarredOnly() },
                    label = { Text(if (isFr) "★ Favoris" else "★ Starred") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (starredOnly) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = if (starredOnly) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("filter_starred_chip")
                )
            }
        }

        // --- Contacts List ---
        if (filteredContacts.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PersonOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isFr) "Aucun contact correspondant" else "No matching contacts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isFr) "Importez le contact Nesrin Ouis ou ajoutez un contact." else "Import Nesrin Ouis contact or add a new one.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.restoreDefaultContact() },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isFr) "Restaurer Nesrin Ouis" else "Restore Nesrin Ouis")
                        }
                    }
                }
            }
        } else {
            items(filteredContacts, key = { it.id }) { contact ->
                ContactCard(
                    contact = contact,
                    onToggleStarred = { viewModel.toggleContactStarred(contact) },
                    onEdit = { editingContact = contact },
                    onDelete = { contactToDelete = contact },
                    onCall = { launchDialer(context, contact.phoneNumber) },
                    onSms = { launchSms(context, contact.phoneNumber) },
                    onMap = { launchMap(context, contact.addressFormatted.ifBlank { "${contact.street} ${contact.city}" }) },
                    onOpenProfile = { launchUrl(context, contact.profileUrl) },
                    onShareAudio = { shareAudioMemo(context, contact) },
                    isFr = isFr
                )
            }
        }
    }

    // --- Add/Edit Contact Dialog ---
    if (showAddDialog || editingContact != null) {
        val isEditing = editingContact != null
        ContactEditDialog(
            initialContact = editingContact,
            isFr = isFr,
            onDismiss = {
                showAddDialog = false
                editingContact = null
            },
            onSave = { contact ->
                if (isEditing) {
                    viewModel.updateContact(contact)
                } else {
                    viewModel.addContact(contact)
                }
                showAddDialog = false
                editingContact = null
            }
        )
    }

    // --- Import Google Contacts CSV Dialog ---
    if (showImportCsvDialog) {
        ImportGoogleContactsCsvDialog(
            isFr = isFr,
            onDismiss = { showImportCsvDialog = false },
            onImport = { csvText ->
                viewModel.importContactsFromCsv(csvText)
                showImportCsvDialog = false
            }
        )
    }

    // --- Confirm Delete Dialog ---
    if (contactToDelete != null) {
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            icon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(if (isFr) "Supprimer ce contact ?" else "Delete this contact?") },
            text = {
                Text(
                    if (isFr) "Voulez-vous vraiment supprimer '${contactToDelete?.displayName}' ?"
                    else "Are you sure you want to delete '${contactToDelete?.displayName}'?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        contactToDelete?.let { viewModel.deleteContact(it) }
                        contactToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isFr) "Supprimer" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToDelete = null }) {
                    Text(if (isFr) "Annuler" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun ContactCard(
    contact: Contact,
    onToggleStarred: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCall: () -> Unit,
    onSms: () -> Unit,
    onMap: () -> Unit,
    onOpenProfile: () -> Unit,
    onShareAudio: () -> Unit,
    isFr: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            1.dp,
            if (contact.isStarred) Color(0xFFFFB300).copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("contact_card_${contact.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Avatar, Name, Star & Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Circle with Initials
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (contact.isStarred) Color(0xFFFFB300).copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.initials,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (contact.isStarred) Color(0xFFD97706) else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = contact.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (contact.isStarred) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Favori",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (contact.organization.isNotBlank() && contact.organization != contact.displayName) {
                        Text(
                            text = contact.organization,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else if (contact.labels.isNotBlank()) {
                        Text(
                            text = contact.labels.replace(":::", "•"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }

                IconButton(onClick = onToggleStarred) {
                    Icon(
                        imageVector = if (contact.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Basculer favori",
                        tint = if (contact.isStarred) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Modifier",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            // Phone Details & Direct Action Chips
            if (contact.phoneNumber.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = contact.phoneNumber,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = contact.phoneLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilledTonalIconButton(
                                onClick = onCall,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Call,
                                    contentDescription = "Appeler",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            FilledTonalIconButton(
                                onClick = onSms,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Sms,
                                    contentDescription = "Envoyer SMS",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Address Section
            if (contact.addressFormatted.isNotBlank() || contact.street.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = contact.addressFormatted.ifBlank {
                                        "${contact.street}, ${contact.city} ${contact.region} ${contact.postalCode} ${contact.country}".trim()
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (contact.city.isNotBlank() || contact.country.isNotBlank()) {
                                    Text(
                                        text = "${contact.city} • ${contact.region} • ${contact.country}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        FilledTonalIconButton(
                            onClick = onMap,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Map,
                                contentDescription = "Ouvrir dans Maps",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Notes & CanadaPages Profile URL Section
            if (contact.notes.isNotBlank() || contact.profileUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        if (contact.notes.isNotBlank()) {
                            Text(
                                text = contact.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }

                        if (contact.profileUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = onOpenProfile,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.OpenInBrowser,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isFr) "Voir profil CanadaPages.com" else "View CanadaPages.com Profile",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Quick Action: Send Voice Memo / Recording Link to Contact
            OutlinedButton(
                onClick = onShareAudio,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("share_audio_contact_${contact.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.RecordVoiceOver,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isFr) "Partager Enregistrement Audio avec ce Contact" else "Share Audio Recording to this Contact",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ContactEditDialog(
    initialContact: Contact?,
    isFr: Boolean,
    onDismiss: () -> Unit,
    onSave: (Contact) -> Unit
) {
    var firstName by remember { mutableStateOf(initialContact?.firstName ?: "") }
    var lastName by remember { mutableStateOf(initialContact?.lastName ?: "") }
    var organization by remember { mutableStateOf(initialContact?.organization ?: "") }
    var phone by remember { mutableStateOf(initialContact?.phoneNumber ?: "") }
    var phoneLabel by remember { mutableStateOf(initialContact?.phoneLabel ?: "Mobile") }
    var address by remember { mutableStateOf(initialContact?.addressFormatted ?: "") }
    var street by remember { mutableStateOf(initialContact?.street ?: "") }
    var city by remember { mutableStateOf(initialContact?.city ?: "") }
    var region by remember { mutableStateOf(initialContact?.region ?: "QC") }
    var postalCode by remember { mutableStateOf(initialContact?.postalCode ?: "") }
    var country by remember { mutableStateOf(initialContact?.country ?: "Canada") }
    var notes by remember { mutableStateOf(initialContact?.notes ?: "") }
    var profileUrl by remember { mutableStateOf(initialContact?.profileUrl ?: "") }
    var isStarred by remember { mutableStateOf(initialContact?.isStarred ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialContact != null) {
                    if (isFr) "Modifier le contact" else "Edit Contact"
                } else {
                    if (isFr) "Nouveau contact" else "New Contact"
                }
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text(if (isFr) "Prénom / Nom Complet *" else "First Name / Full Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(if (isFr) "Numéro de téléphone *" else "Phone Number *") },
                        placeholder = { Text("+1 438-985-5041") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = organization,
                        onValueChange = { organization = it },
                        label = { Text(if (isFr) "Organisation" else "Organization") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(if (isFr) "Adresse complète" else "Full Address") },
                        placeholder = { Text("4347 Rue De Venise Saint-Leonard QC H1S1G2 Canada") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text(if (isFr) "Ville" else "City") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = postalCode,
                            onValueChange = { postalCode = it },
                            label = { Text(if (isFr) "Code Postal" else "Postal Code") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = profileUrl,
                        onValueChange = { profileUrl = it },
                        label = { Text(if (isFr) "Lien Profil Web / CanadaPages" else "Web / CanadaPages Profile URL") },
                        placeholder = { Text("https://www.canadapages.com/...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(if (isFr) "Notes" else "Notes") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isStarred,
                            onCheckedChange = { isStarred = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isFr) "Marquer comme contact favori (★)" else "Mark as Starred (★)")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (firstName.isBlank() && phone.isBlank()) return@Button
                    val contact = initialContact?.copy(
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        organization = organization.trim(),
                        phoneNumber = phone.trim(),
                        phoneLabel = phoneLabel.trim(),
                        addressFormatted = address.trim(),
                        street = street.trim(),
                        city = city.trim(),
                        region = region.trim(),
                        postalCode = postalCode.trim(),
                        country = country.trim(),
                        notes = notes.trim(),
                        profileUrl = profileUrl.trim(),
                        isStarred = isStarred
                    ) ?: Contact(
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        organization = organization.trim(),
                        phoneNumber = phone.trim(),
                        phoneLabel = phoneLabel.trim(),
                        addressFormatted = address.trim(),
                        street = street.trim(),
                        city = city.trim(),
                        region = region.trim(),
                        postalCode = postalCode.trim(),
                        country = country.trim(),
                        notes = notes.trim(),
                        profileUrl = profileUrl.trim(),
                        isStarred = isStarred
                    )
                    onSave(contact)
                }
            ) {
                Text(if (isFr) "Enregistrer" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isFr) "Annuler" else "Cancel")
            }
        }
    )
}

@Composable
private fun ImportGoogleContactsCsvDialog(
    isFr: Boolean,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    val sampleCsv = remember {
        """First Name,Middle Name,Last Name,Phonetic First Name,Phonetic Middle Name,Phonetic Last Name,Name Prefix,Name Suffix,Nickname,File As,Organization Name,Organization Title,Organization Department,Birthday,Notes,Photo,Labels,Phone 1 - Label,Phone 1 - Value,Address 1 - Label,Address 1 - Formatted,Address 1 - Street,Address 1 - City,Address 1 - PO Box,Address 1 - Region,Address 1 - Postal Code,Address 1 - Country,Address 1 - Extended Address
Nesrin Ouis,,,,,,,,,,Nesrin Ouis,,,,Here is the link to Nesrin Ouis profile on CanadaPages.com. https://www.canadapages.com/wp/nesrin-ouis-saintleonard-qc-4383809395/,,* myContacts ::: * starred,Mobile,+1 438-985-5041,Home,4347 Rue De Venise Saint-Leonard QC H1S1G2 Canada,4347 Rue De Venise,,,QC,H1S1G2,Canada,Saint-Leonard"""
    }
    var csvText by remember { mutableStateOf(sampleCsv) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isFr) "Importer CSV Google Contacts" else "Import Google Contacts CSV")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isFr)
                        "Collez le texte CSV exporté depuis Google Contacts ou saisissez vos lignes de contacts ci-dessous :"
                    else
                        "Paste Google Contacts CSV text below to parse and import contacts:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = csvText,
                    onValueChange = { csvText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                    placeholder = { Text("First Name,Last Name,Phone 1 - Value...") }
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isFr) "Le format inclut automatiquement le profil de Nesrin Ouis." else "Pre-loaded with Nesrin Ouis profile.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (csvText.isNotBlank()) {
                        onImport(csvText)
                    }
                }
            ) {
                Text(if (isFr) "Importer" else "Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isFr) "Annuler" else "Cancel")
            }
        }
    )
}

// Helper Intent launchers
private fun launchDialer(context: Context, phone: String) {
    if (phone.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.replace(" ", "")}"))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Impossible d'ouvrir le composeur d'appel: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun launchSms(context: Context, phone: String) {
    if (phone.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${phone.replace(" ", "")}")).apply {
            putExtra("sms_body", "Bonjour Nesrin, voici le lien de synchronisation Audio Sync Hub.")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Impossible d'ouvrir l'application SMS: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun launchMap(context: Context, address: String) {
    if (address.isBlank()) return
    try {
        val uri = Uri.parse("geo:0,0?q=" + Uri.encode(address))
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Impossible d'ouvrir la carte: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun launchUrl(context: Context, url: String) {
    if (url.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Impossible d'ouvrir le lien: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareAudioMemo(context: Context, contact: Contact) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Audio Sync Hub - Partage pour ${contact.displayName}")
            putExtra(
                Intent.EXTRA_TEXT,
                "Bonjour ${contact.displayName},\n\nVoici le dossier de captures vocales sécurisé synchronisé via Google Drive / Z-CORE Audio Sync Hub.\n\nContact Destinataire: ${contact.phoneNumber}\nAdresse: ${contact.addressFormatted}"
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "Partager avec ${contact.displayName}"))
    } catch (e: Exception) {
        Toast.makeText(context, "Erreur de partage: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

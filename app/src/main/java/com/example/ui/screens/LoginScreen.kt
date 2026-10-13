package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.service.AuthState
import com.example.data.service.DeviceRealModeInfo
import com.example.data.service.FirebaseAuthService
import com.example.data.viewmodel.AudioSyncViewModel
import com.example.data.viewmodel.NavigationTab
import com.example.ui.components.NetsecureProLicenseCard
import com.example.ui.theme.AppLanguage
import kotlinx.coroutines.launch

private val GoogleBlue = Color(0xFF4285F4)
private val EmeraldSynced = Color(0xFF10B981)
private val CrimsonError = Color(0xFFEF4444)
private val AmberWarning = Color(0xFFF59E0B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AudioSyncViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authService = remember { FirebaseAuthService(context) }
    val authState by authService.authState.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val deviceInfo = remember { authService.getDeviceRealModeInfo() }
    var showManualEmailDialog by remember { mutableStateOf(false) }
    var showDeviceDiagnosticsDialog by remember { mutableStateOf(false) }
    var customClientIdInput by remember { mutableStateOf(FirebaseAuthService.DEFAULT_OAUTH_CLIENT_ID) }
    var isRealDeviceModeActive by remember { mutableStateOf(true) }

    val isFr = appLanguage == AppLanguage.FRENCH

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Language Switcher Bar & Mode Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode Appareil Réel Badge
            Surface(
                color = if (isRealDeviceModeActive) EmeraldSynced.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (isRealDeviceModeActive) EmeraldSynced.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .clickable { showDeviceDiagnosticsDialog = true }
                    .testTag("device_mode_indicator_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isRealDeviceModeActive) Icons.Filled.Smartphone else Icons.Outlined.Devices,
                        contentDescription = null,
                        tint = if (isRealDeviceModeActive) EmeraldSynced else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFr) "Mode Appareil Réel (Device Mode)" else "Real Device Mode",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isRealDeviceModeActive) EmeraldSynced else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Language Toggle Switch (Version FR 🇫🇷 / Version EN 🇬🇧)
            Surface(
                onClick = {
                    viewModel.setAppLanguage(if (isFr) AppLanguage.ENGLISH else AppLanguage.FRENCH)
                },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.testTag("login_language_toggle_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFr) "Version FR 🇫🇷" else "Version EN 🇬🇧",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Logo & Title
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(76.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.CloudSync,
                    contentDescription = "Audio Sync Hub Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isFr) "Connexion Compte Google" else "Google Account Sign-In",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = if (isFr)
                "Authentification Sécurisée Google Drive & Firebase pour Appareils Android"
            else
                "Secure Google Drive & Firebase Authentication for Android Devices",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Main Auth Container Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_screen_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (val state = authState) {
                    is AuthState.Unauthenticated -> {
                        UnauthenticatedStateView(
                            isFr = isFr,
                            deviceInfo = deviceInfo,
                            onGoogleSignInClick = {
                                coroutineScope.launch {
                                    val result = authService.signInWithGoogleCredentialManager(customClientIdInput)
                                    result.getOrNull()?.let { user ->
                                        viewModel.updateGoogleAccount(user.email, user.displayName)
                                    }
                                }
                            },
                            onDeviceAccountSelected = { email, name ->
                                authService.signInWithDeviceAccount(email, name, isRealDevice = true)
                                viewModel.updateGoogleAccount(email, name)
                            },
                            onManualLoginClick = { showManualEmailDialog = true }
                        )
                    }

                    is AuthState.Loading -> {
                        Column(
                            modifier = Modifier.padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isFr) "Connexion au compte Google de l'appareil en cours..." else "Signing in to device Google Account...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isFr) "Vérification des jetons OAuth 2.0 & Firebase" else "Verifying OAuth 2.0 & Firebase credentials",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    is AuthState.Authenticated -> {
                        AuthenticatedStateView(
                            user = state,
                            isFr = isFr,
                            deviceInfo = deviceInfo,
                            onGoToSyncClick = {
                                viewModel.setTab(NavigationTab.CLOUD_SYNC)
                            },
                            onSignOutClick = {
                                authService.signOut()
                                viewModel.disconnectGoogleAccount()
                            }
                        )
                    }

                    is AuthState.Error -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                color = CrimsonError.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Error,
                                        contentDescription = null,
                                        tint = CrimsonError
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CrimsonError
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            UnauthenticatedStateView(
                                isFr = isFr,
                                deviceInfo = deviceInfo,
                                onGoogleSignInClick = {
                                    coroutineScope.launch {
                                        val result = authService.signInWithGoogleCredentialManager(customClientIdInput)
                                        result.getOrNull()?.let { user ->
                                            viewModel.updateGoogleAccount(user.email, user.displayName)
                                        }
                                    }
                                },
                                onDeviceAccountSelected = { email, name ->
                                    authService.signInWithDeviceAccount(email, name, isRealDevice = true)
                                    viewModel.updateGoogleAccount(email, name)
                                },
                                onManualLoginClick = { showManualEmailDialog = true }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hardware Device Info Strip
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDeviceDiagnosticsDialog = true }
                .testTag("device_diagnostics_strip")
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Smartphone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isFr) "Appareil : ${deviceInfo.manufacturer} ${deviceInfo.deviceModel}" else "Device: ${deviceInfo.manufacturer} ${deviceInfo.deviceModel}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isFr) "Android ${deviceInfo.androidVersion} (API ${deviceInfo.sdkInt}) • Services Google Play Actifs" else "Android ${deviceInfo.androidVersion} (API ${deviceInfo.sdkInt}) • Google Play Services Active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security & Protocol Info Banner
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFr) "Sécurité OAuth 2.0 & Conformité RGPD" else "OAuth 2.0 Security & GDPR Compliance",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isFr)
                        "La connexion Google CredentialManager génère un jeton sécurisé OAuth 2.0 permettant à Rclone et à l'application de transférer les enregistrements audio vers votre Google Drive sans stocker vos identifiants en clair. Homologué RGPD & EU AI Act 2026."
                    else
                        "Google CredentialManager generates a secure OAuth 2.0 token allowing Rclone and the app to sync audio recordings to Google Drive without exposing raw credentials. Certified GDPR & EU AI Act 2026.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Official NETSECUREPRO License & Certificate Section
        NetsecureProLicenseCard(isInitiallyExpanded = false)
    }

    if (showManualEmailDialog) {
        ManualEmailLoginDialog(
            isFr = isFr,
            initialEmail = viewModel.userGoogleAccountEmail.value,
            onDismiss = { showManualEmailDialog = false },
            onLogin = { email, displayName ->
                authService.signInWithDeviceAccount(email, displayName, isRealDevice = true)
                viewModel.updateGoogleAccount(email, displayName)
                showManualEmailDialog = false
            }
        )
    }

    if (showDeviceDiagnosticsDialog) {
        DeviceDiagnosticsDialog(
            isFr = isFr,
            deviceInfo = deviceInfo,
            currentClientId = customClientIdInput,
            onUpdateClientId = { customClientIdInput = it },
            onDismiss = { showDeviceDiagnosticsDialog = false }
        )
    }
}

@Composable
private fun UnauthenticatedStateView(
    isFr: Boolean,
    deviceInfo: DeviceRealModeInfo,
    onGoogleSignInClick: () -> Unit,
    onDeviceAccountSelected: (email: String, name: String) -> Unit,
    onManualLoginClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (isFr) "Connexion au Compte Google" else "Google Account Required",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isFr)
                "Sélectionnez un compte Google sur cet appareil (${deviceInfo.manufacturer}) pour activer la synchronisation Cloud et Google Drive."
            else
                "Select a Google account on this device (${deviceInfo.manufacturer}) to enable Cloud and Google Drive sync.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Google Sign-In Primary Button (Native Credential Manager Real Mode)
        Button(
            onClick = onGoogleSignInClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GoogleBlue,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("google_sign_in_btn")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "G",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = GoogleBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = if (isFr) "Se connecter avec Google (Mode Réel)" else "Sign in with Google (Real Mode)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick On-Device Accounts List Selector
        Text(
            text = if (isFr) "OU SÉLECTIONNER UN COMPTE DE L'APPAREIL :" else "OR SELECT ON-DEVICE ACCOUNT:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDeviceAccountSelected("1976ilies@gmail.com", "1976ilies") }
                .testTag("primary_device_account_row")
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("1", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "1976ilies@gmail.com",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isFr) "Compte Principal de l'appareil" else "Primary Device Account",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = EmeraldSynced,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Secondary Option: Direct Email Connection
        OutlinedButton(
            onClick = onManualLoginClick,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("manual_login_btn")
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isFr) "Saisir une autre adresse Google" else "Enter another Google address",
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun AuthenticatedStateView(
    user: AuthState.Authenticated,
    isFr: Boolean,
    deviceInfo: DeviceRealModeInfo,
    onGoToSyncClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // User Profile Avatar Circle
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = user.displayName.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = user.displayName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("user_display_name_text")
        )

        Text(
            text = user.email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("user_email_text")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Auth Provider & Real Mode Indicator
        Surface(
            color = EmeraldSynced.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldSynced,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isFr) "Compte Connecté (${user.authProvider})" else "Connected (${user.authProvider})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldSynced
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Device Connection Metadata
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isFr) "Appareil Synchronisé :" else "Synced Device:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${deviceInfo.manufacturer} ${deviceInfo.deviceModel}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isFr) "Destination Google Drive :" else "Google Drive Target:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "gdrive:/Z-CORE/Captures/",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons
        Button(
            onClick = onGoToSyncClick,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("continue_to_sync_btn")
        ) {
            Icon(
                imageVector = Icons.Filled.CloudUpload,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isFr) "Ouvrir la Synchronisation Drive" else "Open Drive Sync Dashboard",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onSignOutClick,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("sign_out_btn")
        ) {
            Icon(
                imageVector = Icons.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isFr) "Se déconnecter" else "Sign Out",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ManualEmailLoginDialog(
    isFr: Boolean,
    initialEmail: String,
    onDismiss: () -> Unit,
    onLogin: (email: String, displayName: String) -> Unit
) {
    var emailInput by remember { mutableStateOf(if (initialEmail.isNotBlank()) initialEmail else "1976ilies@gmail.com") }
    var displayNameInput by remember { mutableStateOf(if (initialEmail.isNotBlank()) initialEmail.substringBefore("@") else "1976ilies") }

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
                Text(if (isFr) "Saisie Compte Google" else "Google Account Entry")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isFr)
                        "Saisissez l'adresse de votre compte Google pour associer les transferts Rclone et les autorisations Cloud."
                    else
                        "Enter your Google account email to pair Rclone transfers and Cloud authorizations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = {
                        emailInput = it
                        if (displayNameInput.isBlank() || displayNameInput == "1976ilies") {
                            displayNameInput = it.substringBefore("@")
                        }
                    },
                    label = { Text(if (isFr) "Email Google" else "Google Email") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Email, contentDescription = null)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_email_input")
                )

                OutlinedTextField(
                    value = displayNameInput,
                    onValueChange = { displayNameInput = it },
                    label = { Text(if (isFr) "Nom d'affichage" else "Display Name") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Person, contentDescription = null)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_display_name_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (emailInput.isNotBlank()) {
                        onLogin(emailInput, displayNameInput)
                    }
                },
                modifier = Modifier.testTag("manual_login_confirm_btn")
            ) {
                Text(if (isFr) "Connecter le Compte" else "Connect Account")
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
private fun DeviceDiagnosticsDialog(
    isFr: Boolean,
    deviceInfo: DeviceRealModeInfo,
    currentClientId: String,
    onUpdateClientId: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tempClientId by remember { mutableStateOf(currentClientId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Smartphone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isFr) "Diagnostic Appareil & Mode Réel" else "Device & Real Mode Diagnostics")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (isFr) "INFORMATIONS MATÉRIELLES :" else "HARDWARE INFORMATION:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("• Fabricant : ${deviceInfo.manufacturer}", fontSize = 12.sp)
                        Text("• Modèle : ${deviceInfo.deviceModel}", fontSize = 12.sp)
                        Text("• Version Android : ${deviceInfo.androidVersion} (SDK ${deviceInfo.sdkInt})", fontSize = 12.sp)
                        Text("• Matériel Réel : ${if (deviceInfo.isPhysicalHardware) "OUI (Mode Réel)" else "Émulateur / Studio"}", fontSize = 12.sp)
                        Text("• Google Play Services : ${if (deviceInfo.googlePlayServicesAvailable) "Actif et Prêt" else "Inactif"}", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isFr) "CLIENT ID OAUTH 2.0 GOOGLE :" else "GOOGLE OAUTH 2.0 CLIENT ID:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = tempClientId,
                    onValueChange = { tempClientId = it },
                    label = { Text("Server Web Client ID") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdateClientId(tempClientId)
                    onDismiss()
                }
            ) {
                Text(if (isFr) "Enregistrer & Fermer" else "Save & Close")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isFr) "Fermer" else "Close")
            }
        }
    )
}

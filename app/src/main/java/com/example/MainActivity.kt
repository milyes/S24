package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.viewmodel.AudioSyncViewModel
import com.example.data.viewmodel.NavigationTab
import com.example.ui.screens.AiStudioScreen
import com.example.ui.screens.CloudSyncScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.RecorderVaultScreen
import com.example.ui.screens.TermuxMacroScreen
import com.example.ui.screens.LiveStreamScreen
import com.example.ui.theme.AudioSyncHubTheme
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.LocalAppLanguage
import androidx.compose.runtime.CompositionLocalProvider

import com.example.ui.components.PlayStoreListingDialog
import com.example.ui.components.CameraCaptureDialog
import com.example.ui.components.ThemePickerDialog
import com.example.ui.components.RealModeSettingsDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    com.example.data.service.FirebaseAuthService.ensureFirebaseAppInitialized(applicationContext)
    enableEdgeToEdge()
    setContent {
      val viewModel: AudioSyncViewModel = viewModel()
      val themeMode by viewModel.appThemeMode.collectAsState()
      val useDynamicColor by viewModel.isDynamicColorEnabled.collectAsState()
      val appLanguage by viewModel.appLanguage.collectAsState()

      CompositionLocalProvider(LocalAppLanguage provides appLanguage) {
        AudioSyncHubTheme(
          themeMode = themeMode,
          useDynamicColor = useDynamicColor
        ) {
          AudioSyncApp(viewModel = viewModel)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSyncApp(viewModel: AudioSyncViewModel = viewModel()) {
  val currentTab by viewModel.currentTab.collectAsState()
  val themeMode by viewModel.appThemeMode.collectAsState()
  val isDynamicColorEnabled by viewModel.isDynamicColorEnabled.collectAsState()
  val appLanguage by viewModel.appLanguage.collectAsState()

  var showPlayStoreDialog by remember { mutableStateOf(false) }
  var showCameraDialog by remember { mutableStateOf(false) }
  var showThemeDialog by remember { mutableStateOf(false) }
  var showRealModeDialog by remember { mutableStateOf(false) }

  if (showRealModeDialog) {
    RealModeSettingsDialog(
      viewModel = viewModel,
      isFr = appLanguage == AppLanguage.FRENCH,
      onDismissRequest = { showRealModeDialog = false }
    )
  }

  if (showPlayStoreDialog) {
    PlayStoreListingDialog(
      onDismissRequest = { showPlayStoreDialog = false }
    )
  }

  if (showCameraDialog) {
    CameraCaptureDialog(
      onDismissRequest = { showCameraDialog = false }
    )
  }

  if (showThemeDialog) {
    ThemePickerDialog(
      currentThemeMode = themeMode,
      isDynamicColorEnabled = isDynamicColorEnabled,
      currentLanguage = appLanguage,
      onThemeModeSelected = { viewModel.setAppThemeMode(it) },
      onDynamicColorToggled = { viewModel.toggleDynamicColor(it) },
      onLanguageSelected = { viewModel.setAppLanguage(it) },
      onDismissRequest = { showThemeDialog = false }
    )
  }

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Audio Sync Hub",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "Z-CORE Voice & Google Drive Sync Infrastructure",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        actions = {
          // Real Device Mode Status & Configuration Quick Button
          Surface(
            onClick = { showRealModeDialog = true },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            color = androidx.compose.ui.graphics.Color(0xFF10B981).copy(alpha = 0.2f),
            border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF10B981).copy(alpha = 0.5f)),
            modifier = Modifier
              .padding(end = 4.dp)
              .testTag("top_bar_real_mode_btn")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
              verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color(0xFF10B981),
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = if (appLanguage == AppLanguage.FRENCH) "MODE RÉEL" else "REAL MODE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color(0xFF10B981),
                fontSize = 10.5.sp
              )
            }
          }

          Surface(
            onClick = {
              val nextLang = if (appLanguage == AppLanguage.FRENCH) AppLanguage.ENGLISH else AppLanguage.FRENCH
              viewModel.setAppLanguage(nextLang)
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            modifier = Modifier
              .padding(end = 4.dp)
              .testTag("top_bar_language_btn")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
              Text(
                text = if (appLanguage == AppLanguage.FRENCH) "FR 🇫🇷" else "EN 🇬🇧",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
          }

          IconButton(
            onClick = { viewModel.setTab(NavigationTab.LOGIN) },
            modifier = Modifier.testTag("top_bar_account_btn")
          ) {
            Icon(
              imageVector = if (currentTab == NavigationTab.LOGIN) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
              contentDescription = "Compte Google / Connexion",
              tint = MaterialTheme.colorScheme.primary
            )
          }

          IconButton(
            onClick = { showThemeDialog = true },
            modifier = Modifier.testTag("top_bar_theme_btn")
          ) {
            Icon(
              imageVector = Icons.Filled.Contrast,
              contentDescription = "Changer le thème",
              tint = MaterialTheme.colorScheme.primary
            )
          }

          IconButton(
            onClick = { viewModel.setTab(NavigationTab.LIVE_STREAM) },
            modifier = Modifier.testTag("top_bar_live_tv_btn")
          ) {
            Icon(
              imageVector = if (currentTab == NavigationTab.LIVE_STREAM) Icons.Filled.LiveTv else Icons.Outlined.LiveTv,
              contentDescription = "Flux Direct BEIN 1 HF",
              tint = if (currentTab == NavigationTab.LIVE_STREAM) androidx.compose.ui.graphics.Color(0xFFDC2626) else MaterialTheme.colorScheme.primary
            )
          }

          IconButton(
            onClick = { showCameraDialog = true },
            modifier = Modifier.testTag("top_bar_camera_btn")
          ) {
            Icon(
              imageVector = Icons.Filled.PhotoCamera,
              contentDescription = "Ouvrir la caméra",
              tint = MaterialTheme.colorScheme.primary
            )
          }

          IconButton(
            onClick = { showPlayStoreDialog = true },
            modifier = Modifier.testTag("top_bar_play_store_btn")
          ) {
            Icon(
              imageVector = Icons.Filled.Shop,
              contentDescription = "Google Play Store Listing",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },

    bottomBar = {
      NavigationBar(
        modifier = Modifier.testTag("bottom_navigation_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        windowInsets = WindowInsets.navigationBars
      ) {
        NavigationBarItem(
          selected = currentTab == NavigationTab.RECORDER_VAULT,
          onClick = { viewModel.setTab(NavigationTab.RECORDER_VAULT) },
          icon = {
            Icon(
              imageVector = if (currentTab == NavigationTab.RECORDER_VAULT) Icons.Filled.Mic else Icons.Outlined.Mic,
              contentDescription = "Capture Vocale"
            )
          },
          label = { Text(if (appLanguage == AppLanguage.FRENCH) "Enregistrements" else "Recordings") },
          modifier = Modifier.testTag("tab_recorder")
        )

        NavigationBarItem(
          selected = currentTab == NavigationTab.CLOUD_SYNC,
          onClick = { viewModel.setTab(NavigationTab.CLOUD_SYNC) },
          icon = {
            Icon(
              imageVector = if (currentTab == NavigationTab.CLOUD_SYNC) Icons.Filled.CloudSync else Icons.Outlined.CloudUpload,
              contentDescription = "Google Drive Sync"
            )
          },
          label = { Text(if (appLanguage == AppLanguage.FRENCH) "Synchro Drive" else "Drive Sync") },
          modifier = Modifier.testTag("tab_cloud_sync")
        )

        NavigationBarItem(
          selected = currentTab == NavigationTab.CONTACTS,
          onClick = { viewModel.setTab(NavigationTab.CONTACTS) },
          icon = {
            Icon(
              imageVector = if (currentTab == NavigationTab.CONTACTS) Icons.Filled.Contacts else Icons.Outlined.Contacts,
              contentDescription = "Contacts & Destinataires"
            )
          },
          label = { Text(if (appLanguage == AppLanguage.FRENCH) "Contacts" else "Contacts") },
          modifier = Modifier.testTag("tab_contacts")
        )

        NavigationBarItem(
          selected = currentTab == NavigationTab.LIVE_STREAM,
          onClick = { viewModel.setTab(NavigationTab.LIVE_STREAM) },
          icon = {
            BadgedBox(
              badge = {
                Badge(containerColor = androidx.compose.ui.graphics.Color(0xFFDC2626)) {
                  Text("LIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
              }
            ) {
              Icon(
                imageVector = if (currentTab == NavigationTab.LIVE_STREAM) Icons.Filled.LiveTv else Icons.Outlined.LiveTv,
                contentDescription = "Direct BEIN 1 HF"
              )
            }
          },
          label = { Text("BEIN 1 HF") },
          modifier = Modifier.testTag("tab_live_stream")
        )

        NavigationBarItem(
          selected = currentTab == NavigationTab.TERMUX_MACROS,
          onClick = { viewModel.setTab(NavigationTab.TERMUX_MACROS) },
          icon = {
            Icon(
              imageVector = if (currentTab == NavigationTab.TERMUX_MACROS) Icons.Filled.Terminal else Icons.Outlined.Terminal,
              contentDescription = "Macros Termux / ADB"
            )
          },
          label = { Text(if (appLanguage == AppLanguage.FRENCH) "Macros" else "Macros") },
          modifier = Modifier.testTag("tab_macros")
        )

        NavigationBarItem(
          selected = currentTab == NavigationTab.AI_STUDIO,
          onClick = { viewModel.setTab(NavigationTab.AI_STUDIO) },
          icon = {
            Icon(
              imageVector = if (currentTab == NavigationTab.AI_STUDIO) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
              contentDescription = "Gemini AI"
            )
          },
          label = { Text(if (appLanguage == AppLanguage.FRENCH) "Gemini IA" else "Gemini AI") },
          modifier = Modifier.testTag("tab_ai_studio")
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .consumeWindowInsets(innerPadding)
        .imePadding()
    ) {
      AnimatedContent(
        targetState = currentTab,
        label = "tab_transition"
      ) { tab ->
        when (tab) {
          NavigationTab.LOGIN -> LoginScreen(viewModel = viewModel)
          NavigationTab.RECORDER_VAULT -> RecorderVaultScreen(viewModel = viewModel)
          NavigationTab.CLOUD_SYNC -> CloudSyncScreen(viewModel = viewModel)
          NavigationTab.CONTACTS -> ContactsScreen(viewModel = viewModel)
          NavigationTab.LIVE_STREAM -> LiveStreamScreen(viewModel = viewModel)
          NavigationTab.TERMUX_MACROS -> TermuxMacroScreen(viewModel = viewModel)
          NavigationTab.AI_STUDIO -> AiStudioScreen(viewModel = viewModel)
        }
      }
    }
  }
}

package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.AppThemeMode

@Composable
fun ThemePickerDialog(
    currentThemeMode: AppThemeMode,
    isDynamicColorEnabled: Boolean,
    currentLanguage: AppLanguage = AppLanguage.FRENCH,
    onThemeModeSelected: (AppThemeMode) -> Unit,
    onDynamicColorToggled: (Boolean) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit = {},
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("theme_picker_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (currentLanguage == AppLanguage.FRENCH) "Paramètres & Apparence" else "Settings & Appearance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (currentLanguage == AppLanguage.FRENCH) "Thème, Langue & Couleurs" else "Theme, Language & Colors",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("close_theme_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // LANGUAGE SELECTION SECTION
                Text(
                    text = if (currentLanguage == AppLanguage.FRENCH) "LANGUE DE L'APPLICATION / LANGUAGE" else "APP LANGUAGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // French Choice Card
                    Surface(
                        onClick = { onLanguageSelected(AppLanguage.FRENCH) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (currentLanguage == AppLanguage.FRENCH) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = if (currentLanguage == AppLanguage.FRENCH) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("language_option_fr")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🇫🇷", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Français",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (currentLanguage == AppLanguage.FRENCH) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = "France (FR)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // English Choice Card
                    Surface(
                        onClick = { onLanguageSelected(AppLanguage.ENGLISH) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (currentLanguage == AppLanguage.ENGLISH) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = if (currentLanguage == AppLanguage.ENGLISH) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("language_option_en")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🇬🇧", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "English",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (currentLanguage == AppLanguage.ENGLISH) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = "United Kingdom",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (currentLanguage == AppLanguage.FRENCH) "MODE D'AFFICHAGE" else "DISPLAY MODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Dark Option
                ThemeOptionItem(
                    title = "Sombre Z-CORE (Recommandé)",
                    subtitle = "Interface optimisée pour enregistrement vocal & confort visuel",
                    icon = Icons.Outlined.DarkMode,
                    isSelected = currentThemeMode == AppThemeMode.DARK,
                    onClick = { onThemeModeSelected(AppThemeMode.DARK) },
                    testTag = "theme_option_dark"
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Light Option
                ThemeOptionItem(
                    title = "Clair (M3 Light)",
                    subtitle = "Haute visibilité pour environnements lumineux",
                    icon = Icons.Outlined.LightMode,
                    isSelected = currentThemeMode == AppThemeMode.LIGHT,
                    onClick = { onThemeModeSelected(AppThemeMode.LIGHT) },
                    testTag = "theme_option_light"
                )

                Spacer(modifier = Modifier.height(6.dp))

                // System Option
                ThemeOptionItem(
                    title = "Système",
                    subtitle = "S'adapte automatiquement aux réglages Android",
                    icon = Icons.Outlined.SettingsSuggest,
                    isSelected = currentThemeMode == AppThemeMode.SYSTEM,
                    onClick = { onThemeModeSelected(AppThemeMode.SYSTEM) },
                    testTag = "theme_option_system"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(16.dp))

                // Dynamic Color Option (Android 12+)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Couleurs Dynamiques",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                "Harmonise l'UI avec le fond d'écran (Material You)"
                            } else {
                                "Nécessite Android 12+ (Inactif)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isDynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                        onCheckedChange = { onDynamicColorToggled(it) },
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                        modifier = Modifier.testTag("dynamic_color_switch")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apply_theme_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Appliquer & Fermer", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
        }
    }
}

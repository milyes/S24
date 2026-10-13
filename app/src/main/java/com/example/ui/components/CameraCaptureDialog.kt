package com.example.ui.components

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.sync.RcloneTermuxBackgroundSyncService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CapturedPhoto(
    val file: File,
    val timestamp: String,
    val sizeBytes: Long
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraCaptureDialog(
    onDismissRequest: () -> Unit,
    onPhotoCaptured: (File) -> Unit = {}
) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentPhotoFile by remember { mutableStateOf<File?>(null) }
    var photoTitleInput by remember { mutableStateOf("capture_camera_zcore") }
    var capturedPhotosList by remember { mutableStateOf(getStoredPhotos(context)) }
    var selectedPhotoPreview by remember { mutableStateOf<File?>(null) }

    // Launcher for full resolution photo with FileProvider URI
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoFile != null) {
            val file = currentPhotoFile!!
            Toast.makeText(context, "Photo enregistrée: ${file.name}", Toast.LENGTH_SHORT).show()
            onPhotoCaptured(file)
            capturedPhotosList = getStoredPhotos(context)
            selectedPhotoPreview = file
            // Trigger rclone auto sync
            RcloneTermuxBackgroundSyncService.enqueueSync(context)
        }
    }

    // Fallback Launcher for quick bitmap capture
    val takePicturePreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            val file = saveBitmapToVault(context, bitmap, photoTitleInput)
            if (file != null) {
                currentPhotoFile = file
                Toast.makeText(context, "Aperçu de photo sauvegardé !", Toast.LENGTH_SHORT).show()
                onPhotoCaptured(file)
                capturedPhotosList = getStoredPhotos(context)
                selectedPhotoPreview = file
                // Trigger background sync
                RcloneTermuxBackgroundSyncService.enqueueSync(context)
            }
        }
    }

    fun launchCamera() {
        val photoFile = createPhotoFile(context, photoTitleInput)
        currentPhotoFile = photoFile
        val photoUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
        try {
            takePictureLauncher.launch(photoUri)
        } catch (e: Exception) {
            // Fallback to preview launcher if file provider intent fails
            takePicturePreviewLauncher.launch(null)
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .padding(16.dp)
                .testTag("camera_capture_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
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
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoCamera,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Caméra Z-CORE Captures",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Prise de vue & Instant Auto-Sync",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("close_camera_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Camera Permission check
                if (!cameraPermissionState.status.isGranted) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoCamera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Permission Caméra requise",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Autorisez l'accès à la caméra pour capturer des photos et synchroniser vos clichés.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                            Button(
                                onClick = { cameraPermissionState.launchPermissionRequest() },
                                modifier = Modifier.testTag("grant_camera_permission_btn")
                            ) {
                                Text("Autoriser la Caméra")
                            }
                        }
                    }
                } else {
                    var showLiveCamera by remember { mutableStateOf(true) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showLiveCamera) "Flux Caméra Direct (CameraX)" else "Option Intente Système",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = { showLiveCamera = !showLiveCamera },
                            modifier = Modifier.testTag("toggle_live_camera_mode_btn")
                        ) {
                            Text(if (showLiveCamera) "Appareil Système" else "Live CameraX")
                        }
                    }

                    if (showLiveCamera) {
                        CameraPreview(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            onImageCaptured = { file ->
                                currentPhotoFile = file
                                onPhotoCaptured(file)
                                capturedPhotosList = getStoredPhotos(context)
                                selectedPhotoPreview = file
                                Toast.makeText(context, "Photo capturée: ${file.name}", Toast.LENGTH_SHORT).show()
                                RcloneTermuxBackgroundSyncService.enqueueSync(context)
                            },
                            onError = { err ->
                                Toast.makeText(context, "Erreur CameraX: ${err.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        // Title Input Box
                        OutlinedTextField(
                            value = photoTitleInput,
                            onValueChange = { photoTitleInput = it },
                            label = { Text("Nom / Préfixe du cliché") },
                            leadingIcon = { Icon(Icons.Outlined.Title, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("photo_title_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Take Photo Main Action Button via System Intent
                        Button(
                            onClick = { launchCamera() },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("capture_photo_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Lancer Caméra Système",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Photos Gallery Section
                    Text(
                        text = "Clichés enregistrés (${capturedPhotosList.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (capturedPhotosList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.NoPhotography,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Aucun cliché capturé pour l'instant",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(capturedPhotosList) { photo ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedPhotoPreview = photo.file }
                                        .testTag("photo_item_${photo.file.name}")
                                ) {
                                    Box(contentAlignment = Alignment.BottomStart) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Image,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = photo.file.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                maxLines = 2,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Preview Modal overlay if a photo is selected
                    selectedPhotoPreview?.let { file ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
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
                                        text = file.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${file.length() / 1024} KB • Prêt pour Drive Sync",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            RcloneTermuxBackgroundSyncService.enqueueSync(context)
                                            Toast.makeText(context, "Transfert Cloud déclenché pour ${file.name}", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.testTag("sync_photo_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CloudUpload,
                                            contentDescription = "Sync Cloud",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            file.delete()
                                            selectedPhotoPreview = null
                                            capturedPhotosList = getStoredPhotos(context)
                                            Toast.makeText(context, "Photo supprimée", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.testTag("delete_photo_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "Supprimer",
                                            tint = MaterialTheme.colorScheme.error
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
}

private fun createPhotoFile(context: Context, titlePrefix: String): File {
    val storageDir = File(context.filesDir, "pictures_vault").apply { if (!exists()) mkdirs() }
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val cleanPrefix = if (titlePrefix.isBlank()) "IMG_ZCORE" else titlePrefix.replace(" ", "_")
    return File(storageDir, "${cleanPrefix}_$timeStamp.jpg")
}

private fun saveBitmapToVault(context: Context, bitmap: Bitmap, titlePrefix: String): File? {
    return try {
        val file = createPhotoFile(context, titlePrefix)
        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
        fos.flush()
        fos.close()
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun getStoredPhotos(context: Context): List<CapturedPhoto> {
    val storageDir = File(context.filesDir, "pictures_vault")
    if (!storageDir.exists()) return emptyList()

    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return storageDir.listFiles()
        ?.filter { it.extension.lowercase() in listOf("jpg", "jpeg", "png") }
        ?.sortedByDescending { it.lastModified() }
        ?.map { file ->
            CapturedPhoto(
                file = file,
                timestamp = dateFormat.format(Date(file.lastModified())),
                sizeBytes = file.length()
            )
        } ?: emptyList()
}

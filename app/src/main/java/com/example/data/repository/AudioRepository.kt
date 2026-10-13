package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.AudioRecording
import com.example.data.model.CommandMacro
import com.example.data.model.SyncLog
import com.example.data.service.GeminiAudioAnalysisResult
import com.example.data.service.GeminiAudioAnalysisService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRepository(
    private val db: AppDatabase,
    private val context: Context? = null
) {

    private val geminiAudioService by lazy {
        context?.let { GeminiAudioAnalysisService(it) }
    }

    val allRecordings: Flow<List<AudioRecording>> = db.audioRecordingDao().getAllRecordings()
    val allSyncLogs: Flow<List<SyncLog>> = db.syncLogDao().getAllSyncLogs()
    val allMacros: Flow<List<CommandMacro>> = db.commandMacroDao().getAllMacros()

    fun searchRecordings(query: String): Flow<List<AudioRecording>> {
        val dateFormatIso = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateFormatFr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateFormatShort = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val queryLower = query.lowercase().trim()

        return db.audioRecordingDao().getAllRecordings().map { list ->
            list.filter { item ->
                val dateStr1 = dateFormatIso.format(Date(item.timestamp)).lowercase()
                val dateStr2 = dateFormatFr.format(Date(item.timestamp)).lowercase()
                val dateStr3 = dateFormatShort.format(Date(item.timestamp)).lowercase()
                val timestampRaw = item.timestamp.toString()

                item.fileName.lowercase().contains(queryLower) ||
                item.title.lowercase().contains(queryLower) ||
                item.localPath.lowercase().contains(queryLower) ||
                dateStr1.contains(queryLower) ||
                dateStr2.contains(queryLower) ||
                dateStr3.contains(queryLower) ||
                timestampRaw.contains(queryLower)
            }
        }
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        if (db.audioRecordingDao().getCount() == 0) {
            val initialRecordings = listOf(
                AudioRecording(
                    title = "Flux WAY Vocal Capture",
                    fileName = "enregistrement_ghost_vocal.wav",
                    durationSeconds = 60,
                    fileSizeMb = 10.4f,
                    localPath = "./storage/enregistrement_ghost_vocal.wav",
                    cloudPath = "gdrive:/Z-CORE/Captures/enregistrement_ghost_vocal.wav",
                    isSynced = false,
                    syncStatus = "LOCAL_ONLY",
                    sourceStream = "Flux WAY (Z-CORE)",
                    timestamp = System.currentTimeMillis() - 3600000,
                    transcription = "Capture vocale d'une minute issue du flux WAY. Écoutable et prêt pour transfert cloud vers gdrive:/Z-CORE/Captures/.",
                    aiSummary = "Enregistrement vocal de 60s capturé en local sur l'instance Z-CORE. Prêt à être transféré via rclone vers Google Drive.",
                    waveformData = "20,45,80,65,30,90,70,85,95,60,40,75,85,90,65,50,80,95,70,40"
                ),
                AudioRecording(
                    title = "Archives Vocales 2023",
                    fileName = "enregistrement2023-02-06 07-11-38.wav",
                    durationSeconds = 185,
                    fileSizeMb = 31.2f,
                    localPath = "./storage/archives/enregistrement2023-02-06.wav",
                    cloudPath = "gdrive:/Archives/2023/enregistrement2023-02-06 07-11-38.wav",
                    isSynced = true,
                    syncStatus = "CLOUD_SYNCED",
                    sourceStream = "Drive Archive",
                    timestamp = System.currentTimeMillis() - 86400000000L,
                    transcription = "Archive audio historique de février 2023 déjà présente sur le Google Drive principal.",
                    aiSummary = "Fichier audio archivé de 3m05s. Sauvegarde confirmée sur le stockage cloud distant.",
                    waveformData = "15,30,40,50,45,60,70,65,80,75,60,50,40,30,25,35,45,55,60,50"
                )
            )
            for (rec in initialRecordings) {
                db.audioRecordingDao().insertRecording(rec)
            }
        }

        if (db.commandMacroDao().getCount() == 0) {
            val initialMacros = listOf(
                CommandMacro(
                    name = "Sync Ghost Vocal to Drive",
                    category = "RCLONE",
                    commandText = "rclone copy ./storage/enregistrement_ghost_vocal.wav gdrive:/Z-CORE/Captures/",
                    description = "Copie chiffrée de l'enregistrement Z-CORE vers Google Drive."
                ),
                CommandMacro(
                    name = "Sync Root Drive Folder",
                    category = "RCLONE",
                    commandText = "rclone copy ./storage/enregistrement_ghost_vocal.wav gdrive:/",
                    description = "Envoie le fichier directement à la racine de Google Drive."
                ),
                CommandMacro(
                    name = "Setup Termux API Tools",
                    category = "TERMUX",
                    commandText = "pkg update && pkg install termux-api",
                    description = "Installe les extensions Termux API pour gérer le matériel et l'affichage."
                ),
                CommandMacro(
                    name = "Acquire Wake Lock",
                    category = "TERMUX",
                    commandText = "termux-wake-lock",
                    description = "Empêche l'écran de se mettre en veille pendant les transferts."
                ),
                CommandMacro(
                    name = "Release Wake Lock",
                    category = "TERMUX",
                    commandText = "termux-wake-unlock",
                    description = "Permet à l'écran de se remettre en veille normalement."
                ),
                CommandMacro(
                    name = "ADB Wake Screen",
                    category = "ADB",
                    commandText = "adb shell input keyevent 26",
                    description = "Simule l'appui sur le bouton d'alimentation pour réveiller le téléphone."
                ),
                CommandMacro(
                    name = "ADB Swipe Unlock",
                    category = "ADB",
                    commandText = "adb shell input swipe 500 1500 500 500 200",
                    description = "Simule un glissement vers le haut pour afficher l'écran de déverrouillage."
                ),
                CommandMacro(
                    name = "Z_GHOST Trailer Wakeup (Fido)",
                    category = "Z_GHOST",
                    commandText = "mode Z_GHOST_TLE wakeup --target +14389855041 --network FIDO --trailer-ghost",
                    description = "Commande de réveil à distance Z_GHOST Trailer via le réseau cellulaire Fido (+1 438 985-5041)."
                ),
                CommandMacro(
                    name = "Appel Trailer Ghost (Réseau Fido)",
                    category = "Z_GHOST",
                    commandText = "termux-telephony-call +14389855041",
                    description = "Déclenche un appel cellulaire direct vers le terminal Trailer Ghost sur le réseau Fido."
                )
            )
            db.commandMacroDao().insertAll(initialMacros)
        }

        if (db.syncLogDao().getCount() == 0) {
            val seededLogs = mutableListOf<SyncLog>()
            val now = System.currentTimeMillis()
            val dayMillis = 86400000L

            // Deterministic distribution of 30 days sync metrics
            val dailyAttemptsPattern = listOf(
                // 30 days ago to 1 day ago
                Pair(4, 0), Pair(6, 1), Pair(5, 0), Pair(8, 0), Pair(3, 1),
                Pair(7, 0), Pair(9, 1), Pair(4, 0), Pair(6, 0), Pair(8, 1),
                Pair(5, 0), Pair(11, 1), Pair(7, 0), Pair(6, 0), Pair(9, 2),
                Pair(5, 0), Pair(8, 0), Pair(12, 1), Pair(6, 0), Pair(7, 1),
                Pair(10, 0), Pair(8, 0), Pair(14, 1), Pair(9, 0), Pair(11, 1),
                Pair(13, 0), Pair(10, 1), Pair(15, 0), Pair(12, 1), Pair(8, 0)
            )

            dailyAttemptsPattern.forEachIndexed { index, (successCount, failCount) ->
                val daysAgo = 29 - index
                val baseTime = now - (daysAgo * dayMillis)

                // Add successful attempts
                for (s in 0 until successCount) {
                    val timeOffset = (s * 3600000L * 2) + ((s * 17) % 30) * 60000L
                    val mbSize = (4.2 + (s * 1.8) % 15.0)
                    seededLogs.add(
                        SyncLog(
                            recordingId = if (s % 2 == 0) 1L else 2L,
                            commandExecuted = when (s % 3) {
                                0 -> "rclone copy ./storage/enregistrement_ghost_vocal.wav gdrive:/Z-CORE/Captures/ --progress"
                                1 -> "rclone sync ./storage/ gdrive:/Z-CORE/Captures/ --fast-list"
                                else -> "rclone copy ./storage/archives/ gdrive:/Archives/2026/ --checksum"
                            },
                            status = "SUCCESS",
                            progressPercent = 100,
                            bytesTransferred = "${String.format(Locale.US, "%.1f", mbSize)} MB",
                            timestamp = baseTime + timeOffset,
                            errorMessage = null
                        )
                    )
                }

                // Add failed attempts if any
                for (f in 0 until failCount) {
                    val timeOffset = ((f + 3) * 3600000L * 3) + 15 * 60000L
                    seededLogs.add(
                        SyncLog(
                            recordingId = 1L,
                            commandExecuted = "rclone copy ./storage/enregistrement_ghost_vocal.wav gdrive:/Z-CORE/Captures/",
                            status = "FAILED",
                            progressPercent = 35 + (f * 15),
                            bytesTransferred = "1.2 MB",
                            timestamp = baseTime + timeOffset,
                            errorMessage = when (f % 3) {
                                0 -> "Erreur HTTP 504 Gateway Timeout (Réseau distant instable)"
                                1 -> "Jeton OAuth Google temporairement expiré (rafraîchissement automatique)"
                                else -> "Connexion socket interrompue pendant l'upload"
                            }
                        )
                    )
                }
            }

            db.syncLogDao().insertAll(seededLogs)
        }
    }

    suspend fun insertRecording(recording: AudioRecording): Long = withContext(Dispatchers.IO) {
        db.audioRecordingDao().insertRecording(recording)
    }

    suspend fun updateRecording(recording: AudioRecording) = withContext(Dispatchers.IO) {
        db.audioRecordingDao().updateRecording(recording)
    }

    suspend fun deleteRecording(id: Long) = withContext(Dispatchers.IO) {
        db.audioRecordingDao().deleteRecording(id)
    }

    suspend fun logSyncOperation(
        recordingId: Long?,
        command: String,
        status: String,
        progress: Int,
        bytes: String,
        errorMsg: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val log = SyncLog(
            recordingId = recordingId,
            commandExecuted = command,
            status = status,
            progressPercent = progress,
            bytesTransferred = bytes,
            errorMessage = errorMsg
        )
        db.syncLogDao().insertSyncLog(log)
    }

    suspend fun toggleFavoriteMacro(id: Long, currentFav: Boolean) = withContext(Dispatchers.IO) {
        db.commandMacroDao().toggleFavorite(id, !currentFav)
    }

    suspend fun analyzeAudioWithGemini(
        recording: AudioRecording,
        userPrompt: String? = null,
        apiKey: String
    ): GeminiAudioAnalysisResult = withContext(Dispatchers.IO) {
        val service = geminiAudioService
        if (service != null) {
            val result = service.analyzeAudioFile(recording, userPrompt, apiKey)
            if (result.isSuccess && result.transcription.isNotBlank() && result.transcription != recording.transcription) {
                db.audioRecordingDao().updateRecording(recording.copy(
                    transcription = result.transcription,
                    aiSummary = result.summary
                ))
            } else if (result.isSuccess) {
                db.audioRecordingDao().updateRecording(recording.copy(aiSummary = result.summary))
            }
            result
        } else {
            GeminiAudioAnalysisResult(
                summary = "Analyse Locale Z-CORE: '${recording.title}' est une capture audio de ${recording.durationSeconds}s (${recording.fileSizeMb}MB). Format .wav prêt pour diffusion ou transfert cloud RClone.",
                transcription = recording.transcription ?: "",
                isSuccess = false
            )
        }
    }

    suspend fun generateAiSummary(recording: AudioRecording, apiKey: String): String = withContext(Dispatchers.IO) {
        val result = analyzeAudioWithGemini(recording, null, apiKey)
        result.summary
    }
}

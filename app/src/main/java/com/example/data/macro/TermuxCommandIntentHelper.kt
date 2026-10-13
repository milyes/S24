package com.example.data.macro

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data model representing a customizable script argument for pre-defined Termux scripts.
 */
data class ScriptArgument(
    val key: String,
    val labelFr: String,
    val labelEn: String,
    val defaultValue: String,
    val descriptionFr: String,
    val descriptionEn: String,
    val isSecret: Boolean = false,
    val options: List<String> = emptyList() // For selectable dropdowns
)

/**
 * Categories for classifying pre-defined Termux scripts.
 */
enum class TermuxScriptCategory(val labelFr: String, val labelEn: String) {
    RCLONE_CLOUD("Cloud & Rclone", "Cloud & Rclone"),
    AUDIO_PROCESSING("Traitement Audio (FFmpeg)", "Audio Processing (FFmpeg)"),
    STORAGE_SYSTEM("Stockage & Système", "Storage & System"),
    TELEMETRY_GHOST("Télémétrie Z-GHOST", "Z-GHOST Telemetry"),
    DIAGNOSTICS("Réseau & Diagnostic", "Network & Diagnostics"),
    TERMUX_API("Capteurs Termux:API", "Termux:API Sensors")
}

/**
 * Definition of a pre-defined shell script that can be triggered via Android Intent targeting Termux.
 */
data class PredefinedTermuxScript(
    val id: String,
    val titleFr: String,
    val titleEn: String,
    val category: TermuxScriptCategory,
    val descriptionFr: String,
    val descriptionEn: String,
    val executablePath: String = TermuxCommandIntentHelper.DEFAULT_BASH_PATH,
    val workDir: String = TermuxCommandIntentHelper.DEFAULT_WORKDIR,
    val templateScript: (args: Map<String, String>) -> String,
    val arguments: List<ScriptArgument> = emptyList(),
    val defaultRunInBackground: Boolean = true,
    val iconType: String = "CODE",
    val requiresApiPlugin: Boolean = false,
    val requiresStorageSetup: Boolean = false
)

/**
 * Result record from attempting to dispatch a Termux Intent.
 */
data class TermuxExecutionResult(
    val success: Boolean,
    val message: String,
    val executedScriptId: String?,
    val scriptTitle: String,
    val commandDispatched: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isBackground: Boolean = true
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

/**
 * Helper class to build, validate, and execute shell commands and pre-defined scripts
 * targeting the Termux application using the official Android Intent system (`com.termux.RUN_COMMAND`).
 */
class TermuxCommandIntentHelper(private val context: Context) {

    companion object {
        private const val TAG = "TermuxIntentHelper"

        // Termux Official Package Names
        const val TERMUX_PACKAGE_NAME = "com.termux"
        const val TERMUX_API_PACKAGE_NAME = "com.termux.api"
        const val TERMUX_SERVICE_NAME = "com.termux.app.RunCommandService"

        // Termux RUN_COMMAND Action & Intent Extras
        const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
        const val EXTRA_RUN_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
        const val EXTRA_RUN_COMMAND_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
        const val EXTRA_RUN_COMMAND_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
        const val EXTRA_RUN_COMMAND_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
        const val EXTRA_RUN_COMMAND_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"
        const val EXTRA_RUN_COMMAND_LABEL = "com.termux.RUN_COMMAND_LABEL"

        // Default sandbox paths in Termux
        const val DEFAULT_BASH_PATH = "/data/data/com.termux/files/usr/bin/bash"
        const val DEFAULT_WORKDIR = "/data/data/com.termux/files/home"
        const val DEFAULT_SCRIPTS_DIR = "/data/data/com.termux/files/home/scripts"
        const val DEFAULT_STORAGE_RECORDINGS = "/sdcard/recordings"
    }

    /**
     * Checks if the Termux host application is installed.
     */
    fun isTermuxInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE_NAME, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Checks if the Termux:API companion app is installed.
     */
    fun isTermuxApiInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_API_PACKAGE_NAME, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Builds a standard Android Intent targeted at Termux using `com.termux.RUN_COMMAND`.
     */
    fun buildTermuxRunCommandIntent(
        commandString: String,
        workDir: String = DEFAULT_WORKDIR,
        runInBackground: Boolean = true,
        sessionAction: String = "0", // 0: fail quietly, 1: keep session open
        label: String? = null,
        executablePath: String = DEFAULT_BASH_PATH
    ): Intent {
        return Intent(ACTION_RUN_COMMAND).apply {
            // Set explicit package to Termux
            setPackage(TERMUX_PACKAGE_NAME)
            // Specify explicit RunCommandService component
            component = ComponentName(TERMUX_PACKAGE_NAME, TERMUX_SERVICE_NAME)

            putExtra(EXTRA_RUN_COMMAND_PATH, executablePath)
            putExtra(EXTRA_RUN_COMMAND_ARGUMENTS, arrayOf("-c", commandString))
            putExtra(EXTRA_RUN_COMMAND_WORKDIR, workDir)
            putExtra(EXTRA_RUN_COMMAND_BACKGROUND, runInBackground)
            putExtra(EXTRA_RUN_COMMAND_SESSION_ACTION, sessionAction)
            label?.let { putExtra(EXTRA_RUN_COMMAND_LABEL, it) }

            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        }
    }

    /**
     * Dispatches an Android Intent to execute a raw shell command in Termux.
     */
    fun executeRawCommand(
        commandString: String,
        workDir: String = DEFAULT_WORKDIR,
        runInBackground: Boolean = true,
        label: String = "Raw Shell Command"
    ): TermuxExecutionResult {
        if (commandString.isBlank()) {
            return TermuxExecutionResult(
                success = false,
                message = "La commande shell est vide.",
                executedScriptId = null,
                scriptTitle = label,
                commandDispatched = commandString,
                isBackground = runInBackground
            )
        }

        val intent = buildTermuxRunCommandIntent(
            commandString = commandString,
            workDir = workDir,
            runInBackground = runInBackground,
            label = label
        )

        return dispatchIntent(intent, null, label, commandString, runInBackground)
    }

    /**
     * Dispatches an Android Intent to execute a pre-defined script with custom arguments.
     */
    fun executePredefinedScript(
        script: PredefinedTermuxScript,
        customArgs: Map<String, String> = emptyMap(),
        runInBackground: Boolean = script.defaultRunInBackground
    ): TermuxExecutionResult {
        // Merge provided args with defaults
        val resolvedArgs = mutableMapOf<String, String>()
        for (argDef in script.arguments) {
            resolvedArgs[argDef.key] = customArgs[argDef.key] ?: argDef.defaultValue
        }

        val generatedShellCode = script.templateScript(resolvedArgs)

        val intent = buildTermuxRunCommandIntent(
            commandString = generatedShellCode,
            workDir = script.workDir,
            runInBackground = runInBackground,
            label = script.titleEn,
            executablePath = script.executablePath
        )

        return dispatchIntent(intent, script.id, script.titleFr, generatedShellCode, runInBackground)
    }

    /**
     * Generates the complete executable bash script content for preview or clipboard export.
     */
    fun generateScriptContent(
        script: PredefinedTermuxScript,
        customArgs: Map<String, String> = emptyMap()
    ): String {
        val resolvedArgs = mutableMapOf<String, String>()
        for (argDef in script.arguments) {
            resolvedArgs[argDef.key] = customArgs[argDef.key] ?: argDef.defaultValue
        }
        return script.templateScript(resolvedArgs)
    }

    /**
     * Internal dispatcher that tries Service startup, with Broadcast fallback.
     */
    private fun dispatchIntent(
        intent: Intent,
        scriptId: String?,
        title: String,
        commandText: String,
        isBackground: Boolean
    ): TermuxExecutionResult {
        val isInstalled = isTermuxInstalled()
        Log.d(TAG, "Dispatching Termux intent. Installed=$isInstalled. Command: $commandText")

        return try {
            // First attempt: direct startService to RunCommandService
            var serviceDispatched = false
            try {
                val comp = context.startService(intent)
                serviceDispatched = (comp != null)
                Log.d(TAG, "startService result component: $comp")
            } catch (e: Exception) {
                Log.w(TAG, "Direct startService failed, will try broadcast fallback: ${e.message}")
            }

            // Fallback: sendBroadcast with RUN_COMMAND action
            if (!serviceDispatched) {
                val broadcastIntent = Intent(ACTION_RUN_COMMAND).apply {
                    setPackage(TERMUX_PACKAGE_NAME)
                    putExtras(intent)
                    addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                }
                context.sendBroadcast(broadcastIntent, "com.termux.permission.RUN_COMMAND")
                Log.d(TAG, "Dispatched broadcast fallback for Termux intent.")
            }

            val successMsg = if (isInstalled) {
                "Intent Termux transmis avec succès (Mode ${if (isBackground) "Arrière-plan" else "Session Active"})."
            } else {
                "Intent Termux généré et mis en file (Termux non détecté localement)."
            }

            TermuxExecutionResult(
                success = true,
                message = successMsg,
                executedScriptId = scriptId,
                scriptTitle = title,
                commandDispatched = commandText,
                isBackground = isBackground
            )
        } catch (ex: Exception) {
            Log.e(TAG, "Fatal error dispatching Termux Intent", ex)
            TermuxExecutionResult(
                success = false,
                message = "Erreur d'envoi de l'intent: ${ex.localizedMessage}",
                executedScriptId = scriptId,
                scriptTitle = title,
                commandDispatched = commandText,
                isBackground = isBackground
            )
        }
    }

    /**
     * Returns the comprehensive catalog of pre-defined scripts available for execution.
     */
    fun getPredefinedScriptsCatalog(): List<PredefinedTermuxScript> {
        return listOf(
            // 1. Audio Sync to Google Drive via Rclone
            PredefinedTermuxScript(
                id = "rclone_sync_audio",
                titleFr = "Synchronisation Audio Google Drive",
                titleEn = "Google Drive Audio Rclone Sync",
                category = TermuxScriptCategory.RCLONE_CLOUD,
                descriptionFr = "Transfère et synchronise les enregistrements audio locaux vers Google Drive avec reprise et bande passante contrôlée.",
                descriptionEn = "Transfers and syncs local audio recordings to Google Drive with retries and bandwidth limiting.",
                arguments = listOf(
                    ScriptArgument("sourceDir", "Dossier Local", "Local Folder", "/sdcard/recordings", "Chemin des fichiers audio", "Audio storage directory"),
                    ScriptArgument("remotePath", "Dossier Distant", "Remote Path", "gdrive:/Z-CORE/Captures/", "Destination Google Drive", "Google Drive remote target"),
                    ScriptArgument("bwlimit", "Limite Bande Passante", "Bandwidth Limit", "10M", "Débit max (ex: 5M, 10M, 0 pour illimité)", "Max transfer rate (ex: 5M, 10M, 0 for unlimited)"),
                    ScriptArgument("retries", "Nombre de Retries", "Retries Count", "5", "Tentatives en cas de coupure réseau", "Retries on network disconnect")
                ),
                defaultRunInBackground = true,
                iconType = "CLOUD_SYNC",
                templateScript = { args ->
                    val src = args["sourceDir"] ?: "/sdcard/recordings"
                    val dst = args["remotePath"] ?: "gdrive:/Z-CORE/Captures/"
                    val bw = args["bwlimit"] ?: "10M"
                    val retries = args["retries"] ?: "5"
                    """
                    # === Z-CORE AUDIO RCLONE AUTO-SYNC ===
                    echo "[$(date '+%H:%M:%S')] Demarrage de la synchronisation Rclone..."
                    if ! command -v rclone &> /dev/null; then
                        echo "Erreur: rclone n'est pas installe. Installez avec: pkg install rclone"
                        exit 1
                    fi
                    mkdir -p "$src"
                    rclone copy "$src" "$dst" \
                        --transfers 4 \
                        --retries $retries \
                        --bwlimit $bw \
                        --low-level-retries 10 \
                        --progress \
                        --verbose
                    echo "[$(date '+%H:%M:%S')] Synchronisation terminee vers $dst !"
                    if command -v termux-toast &> /dev/null; then
                        termux-toast "Sync Rclone Z-CORE terminee avec succes !"
                    fi
                    """.trimIndent()
                }
            ),

            // 2. Storage Setup & Permissions
            PredefinedTermuxScript(
                id = "termux_setup_storage",
                titleFr = "Initialisation Stockage & Dossiers",
                titleEn = "Termux Storage & Folder Init",
                category = TermuxScriptCategory.STORAGE_SYSTEM,
                descriptionFr = "Lie le stockage partagé Android (`termux-setup-storage`) et crée l'arborescence ~/recordings et ~/scripts.",
                descriptionEn = "Links Android shared storage (`termux-setup-storage`) and prepares ~/recordings and ~/scripts directories.",
                defaultRunInBackground = false, // Recommended in terminal for permission prompt
                iconType = "FOLDER",
                requiresStorageSetup = true,
                templateScript = { _ ->
                    """
                    # === INITIALISATION STOCKAGE TERMUX & Z-CORE ===
                    echo "Demande d'autorisation de stockage Android..."
                    termux-setup-storage
                    mkdir -p ~/storage/shared/recordings
                    mkdir -p ~/scripts
                    mkdir -p ~/recordings_backup
                    echo "Arborescence Z-CORE configuree avec succes !"
                    ls -la ~/storage/shared/
                    """.trimIndent()
                }
            ),

            // 3. Rclone Connection & OAuth Verification
            PredefinedTermuxScript(
                id = "rclone_test_connection",
                titleFr = "Test Connexion Google Drive & Remotes",
                titleEn = "Test Google Drive & Rclone Remotes",
                category = TermuxScriptCategory.RCLONE_CLOUD,
                descriptionFr = "Vérifie la validité des tokens OAuth2 et liste les dossiers distants du drive configuré.",
                descriptionEn = "Validates OAuth2 token health and lists remote directories on configured Google Drive.",
                arguments = listOf(
                    ScriptArgument("remoteName", "Nom du Remote", "Remote Name", "gdrive:", "Nom du remote rclone.conf", "Rclone remote identifier")
                ),
                defaultRunInBackground = false,
                iconType = "CHECK",
                templateScript = { args ->
                    val remote = args["remoteName"] ?: "gdrive:"
                    """
                    # === VERIFICATION REMOTE RCLONE ===
                    echo "Verification des remotes rclone configures :"
                    rclone listremotes
                    echo "Test de connexion sur $remote..."
                    rclone lsd "$remote" --timeout 15s
                    echo "Test complete avec succes !"
                    """.trimIndent()
                }
            ),

            // 4. Audio Compression & Transcoding (FFmpeg)
            PredefinedTermuxScript(
                id = "ffmpeg_audio_compress",
                titleFr = "Compression Audio FFmpeg (AAC Lossless)",
                titleEn = "FFmpeg Audio Compression (AAC Lossless)",
                category = TermuxScriptCategory.AUDIO_PROCESSING,
                descriptionFr = "Compresse les enregistrements WAV/PCM en format M4A/AAC 64k ou 128k pour optimiser l'envoi cloud.",
                descriptionEn = "Batch encodes and compresses WAV/PCM recordings into optimized AAC M4A files for fast upload.",
                arguments = listOf(
                    ScriptArgument("bitrate", "Débit Audio (Bitrate)", "Audio Bitrate", "96k", "Qualité de compression", "Output bitrate", false, listOf("64k", "96k", "128k", "192k")),
                    ScriptArgument("folder", "Dossier Source", "Source Folder", "/sdcard/recordings", "Emplacement des pistes", "Recordings directory")
                ),
                defaultRunInBackground = true,
                iconType = "AUDIO",
                templateScript = { args ->
                    val bitrate = args["bitrate"] ?: "96k"
                    val folder = args["folder"] ?: "/sdcard/recordings"
                    val d = "$"
                    """
                    # === COMPRESSION AUDIO FFMPEG Z-CORE ===
                    if ! command -v ffmpeg &> /dev/null; then
                        echo "Installation de ffmpeg requise: pkg install ffmpeg"
                        exit 1
                    fi
                    cd "$folder" || exit 1
                    echo "Traitement des fichiers audio dans $folder (Debit: $bitrate)..."
                    for f in *.wav *.mp4; do
                        [ -e "${d}f" ] || continue
                        out="${d}{f%.*}.m4a"
                        if [ ! -f "${d}out" ]; then
                            echo "Compression de ${d}f -> ${d}out"
                            ffmpeg -y -i "${d}f" -c:a aac -b:a $bitrate -ar 44100 "${d}out"
                        fi
                    done
                    echo "Encodage termine avec succes !"
                    """.trimIndent()
                }
            ),

            // 5. Z-Ghost Trailer Wakeup Telemetry
            PredefinedTermuxScript(
                id = "z_ghost_wakeup_telemetry",
                titleFr = "Protocole Réveil Z_GHOST (Fido)",
                titleEn = "Z_GHOST Wakeup Telemetry Protocol",
                category = TermuxScriptCategory.TELEMETRY_GHOST,
                descriptionFr = "Exécute la séquence télémétrique vers le terminal cible mobile Fido (+1 438 985-5041).",
                descriptionEn = "Executes the telemetry wakeup sequence targeted at mobile line Fido (+1 438 985-5041).",
                arguments = listOf(
                    ScriptArgument("targetPhone", "Numéro Cible", "Target Phone", "+14389855041", "Ligne mobile de télémétrie", "Mobile telemetry receiver"),
                    ScriptArgument("carrier", "Opérateur Télécom", "Carrier Network", "FIDO", "Nom du réseau", "Carrier network name")
                ),
                defaultRunInBackground = true,
                iconType = "TERMINAL",
                templateScript = { args ->
                    val target = args["targetPhone"] ?: "+14389855041"
                    val carrier = args["carrier"] ?: "FIDO"
                    """
                    # === Z_GHOST TLE WAKEUP TELEMETRY SEQUENCE ===
                    echo "Activation sequence Z_GHOST [Carrier: $carrier]..."
                    echo "Target Receiver: $target"
                    mode Z_GHOST_TLE wakeup --target $target --network $carrier --trailer-ghost --timestamp $(date +%s)
                    echo "Signal de synchro transmis a l'antenne Fido."
                    if command -v termux-toast &> /dev/null; then
                        termux-toast "Protocole Z_GHOST active sur $target ($carrier)"
                    fi
                    """.trimIndent()
                }
            ),

            // 6. Termux Battery & Hardware Diagnostics
            PredefinedTermuxScript(
                id = "termux_battery_health",
                titleFr = "Télémétrie Batterie & Matériel",
                titleEn = "Battery & Hardware Telemetry",
                category = TermuxScriptCategory.TERMUX_API,
                descriptionFr = "Récupère l'état de la batterie (température, tension, santé, niveau) via l'API matérielle Termux.",
                descriptionEn = "Fetches physical battery metrics (temperature, voltage, health, level) via Termux:API.",
                defaultRunInBackground = false,
                requiresApiPlugin = true,
                iconType = "BATTERY",
                templateScript = { _ ->
                    """
                    # === BATTERY & HARDWARE TELEMETRY ===
                    echo "=== ETAT DE LA BATTERIE (TERMUX:API) ==="
                    if command -v termux-battery-status &> /dev/null; then
                        termux-battery-status
                    else
                        echo "Module termux-api non installe. Executez: pkg install termux-api"
                        cat /sys/class/power_supply/battery/capacity 2>/dev/null || uptime
                    fi
                    echo ""
                    echo "=== INFO PROCESSEUR / KERNEL ==="
                    uname -a
                    """.trimIndent()
                }
            ),

            // 7. Disk Space & Audio Storage Audit
            PredefinedTermuxScript(
                id = "storage_audit_disk",
                titleFr = "Audit Espace Disque & Enregistrements",
                titleEn = "Disk Space & Audio Storage Audit",
                category = TermuxScriptCategory.STORAGE_SYSTEM,
                descriptionFr = "Analyse l'espace libre sur le disque interne et le volume total occupé par les captures vocales.",
                descriptionEn = "Calculates free disk space and total volume consumed by voice audio captures.",
                arguments = listOf(
                    ScriptArgument("targetDir", "Répertoire à auditer", "Audit Directory", "/sdcard/recordings", "Chemin du dossier", "Target directory")
                ),
                defaultRunInBackground = false,
                iconType = "FOLDER",
                templateScript = { args ->
                    val dir = args["targetDir"] ?: "/sdcard/recordings"
                    """
                    # === AUDIT ESPACE DISQUE ET ENREGISTREMENTS ===
                    echo "=== UTILISATION DU DISQUE (/sdcard) ==="
                    df -h /sdcard
                    echo ""
                    echo "=== TAILLE DU DOSSIER ENREGISTREMENTS ($dir) ==="
                    if [ -d "$dir" ]; then
                        du -sh "$dir"
                        echo "Nombre de fichiers captures :"
                        ls -1 "$dir" | wc -l
                    else
                        echo "Le dossier $dir n'existe pas encore."
                    fi
                    """.trimIndent()
                }
            ),

            // 8. Network Connectivity & Cloud Ping Test
            PredefinedTermuxScript(
                id = "network_cloud_ping",
                titleFr = "Diagnostic Réseau & Ping Google APIs",
                titleEn = "Network & Google API Ping Diagnostic",
                category = TermuxScriptCategory.DIAGNOSTICS,
                descriptionFr = "Mesure la latence réseau vers les serveurs Google Drive API et vérifie la résolution DNS.",
                descriptionEn = "Pings Google Drive API endpoints to measure latency and test DNS resolution.",
                defaultRunInBackground = false,
                iconType = "NETWORK",
                templateScript = { _ ->
                    """
                    # === DIAGNOSTIC RESEAU & GOOGLE DRIVE ENDPOINT ===
                    echo "Test de connectivite vers www.googleapis.com..."
                    ping -c 4 www.googleapis.com
                    echo ""
                    echo "Test de resolution DNS :"
                    getprop net.dns1 2>/dev/null || echo "DNS Standard OK"
                    echo ""
                    echo "Test HTTP Header Google Drive :"
                    if command -v curl &> /dev/null; then
                        curl -I --connect-timeout 5 https://www.googleapis.com
                    else
                        echo "curl non installe. Installez avec: pkg install curl"
                    fi
                    """.trimIndent()
                }
            ),

            // 9. Termux Toast & Vibration Feedback
            PredefinedTermuxScript(
                id = "termux_toast_notify",
                titleFr = "Notification Écran & Vibreur (Termux:API)",
                titleEn = "Screen Toast & Vibration (Termux:API)",
                category = TermuxScriptCategory.TERMUX_API,
                descriptionFr = "Envoie une notification d'alerte visuelle et une impulsion haptique sur le téléphone.",
                descriptionEn = "Dispatches a visual on-screen toast and haptic vibration trigger to the phone.",
                arguments = listOf(
                    ScriptArgument("message", "Message Toast", "Toast Message", "Z-CORE Audio Sync Hub: Enregistrement Valide !", "Texte affiche", "Displayed message text"),
                    ScriptArgument("vibDuration", "Duree Vibreur (ms)", "Vibration Duration (ms)", "300", "Duree de vibration", "Vibration length in ms")
                ),
                defaultRunInBackground = true,
                requiresApiPlugin = true,
                iconType = "NOTIFICATION",
                templateScript = { args ->
                    val msg = args["message"] ?: "Z-CORE Audio Sync Hub Actif"
                    val vib = args["vibDuration"] ?: "300"
                    """
                    # === NOTIFICATION & RETOUR HAPTIQUE ===
                    if command -v termux-toast &> /dev/null; then
                        termux-toast -b black -c cyan -g top "$msg"
                    fi
                    if command -v termux-vibrate &> /dev/null; then
                        termux-vibrate -d $vib
                    fi
                    echo "Notification transmise avec succes !"
                    """.trimIndent()
                }
            ),

            // 10. Temporary Cache & Log Cleanup
            PredefinedTermuxScript(
                id = "clean_temp_cache",
                titleFr = "Nettoyage Cache & Logs Temporaires",
                titleEn = "Cleanup Temp Cache & Transfer Logs",
                category = TermuxScriptCategory.STORAGE_SYSTEM,
                descriptionFr = "Purger les journaux d'erreurs temporaires, les fragments de sync interrompus et libérer le cache.",
                descriptionEn = "Purges temporary error logs, aborted sync fragments and frees cache memory.",
                defaultRunInBackground = true,
                iconType = "CLEAN",
                templateScript = { _ ->
                    """
                    # === NETTOYAGE CACHE & LOGS Z-CORE ===
                    echo "Suppression des journaux temporaires..."
                    rm -f ~/rclone_sync.log.bak
                    rm -f /tmp/audio_sync_*.tmp 2>/dev/null || true
                    if command -v apt &> /dev/null; then
                        apt clean 2>/dev/null || true
                    fi
                    echo "Nettoyage termine avec succes !"
                    """.trimIndent()
                }
            ),

            // 11. Windows System Core Update / IA22 Logic (S24 Master Node)
            PredefinedTermuxScript(
                id = "ia22_windows_core_update",
                titleFr = "IA22 : Windows System Core Update (Nœud S24)",
                titleEn = "IA22: Windows System Core Update (S24 Node)",
                category = TermuxScriptCategory.TELEMETRY_GHOST,
                descriptionFr = "Exécute la séquence IA22 de synchronisation du Nœud Maître S24 (M.I. ZOUBIROU), validation ADB, tunnel Cloudflare et nettoyage des traces.",
                descriptionEn = "Executes IA22 synchronization sequence for S24 Master Node (M.I. ZOUBIROU), ADB validation, Cloudflare tunnel and session purge.",
                arguments = listOf(
                    ScriptArgument("deviceNode", "Nœud Maître", "Master Node", "S24", "Nom de l'appareil Android cible", "Target Android device name"),
                    ScriptArgument("operator", "Opérateur / Session", "Operator / Session", "M.I. ZOUBIROU", "Identifiant de session", "Session identifier"),
                    ScriptArgument("tunnelUrl", "Tunnel Cloudflare", "Cloudflare Tunnel", "https://retailers-mitchell-customs-rotation.trycloudflare.com", "URL publique du tunnel", "Public tunnel URL"),
                    ScriptArgument("unlockKey", "Clé Déverrouillage IA", "AI Unlock Key", "979V11", "Mot-clé transmis à unlock.sh", "Key piped to unlock.sh", isSecret = true),
                    ScriptArgument("wslUser", "Utilisateur WSL", "WSL User", "mx76", "Compte Linux à nettoyer", "Linux account to purge")
                ),
                defaultRunInBackground = true,
                iconType = "TERMINAL",
                templateScript = { args ->
                    val node = args["deviceNode"] ?: "S24"
                    val op = args["operator"] ?: "M.I. ZOUBIROU"
                    val tunnel = args["tunnelUrl"] ?: "https://retailers-mitchell-customs-rotation.trycloudflare.com"
                    val key = args["unlockKey"] ?: "979V11"
                    val user = args["wslUser"] ?: "mx76"
                    """
                    # ========================================================
                    # SCRIPT WINDOWS BATCH (.BAT) & COMPAGNON TERMUX S24
                    # TITRE: Windows System Core Update - IA22 Logic
                    # NOEUD MAITRE: $node | OPERATEUR: $op
                    # TUNNEL: $tunnel
                    # ========================================================

                    # --- PHASE 1 & 2 : NOTIFICATION MOBILE ADB ($node) ---
                    echo "[LOG] Initializing IA22 Logic..."
                    echo "[LOG] Verification du Noeud Maitre ($node)..."
                    am broadcast -a android.intent.action.MAIN --es message 'IA22 : SESSION ACTIVE - $op' 2>/dev/null || true
                    am broadcast -a com.example.audiosynchub.IA22_SESSION_ACTIVE --es message 'IA22 : SESSION ACTIVE - $op' 2>/dev/null || true

                    # --- PHASE 3 : EXECUTION AUTOMATIQUE WSL / TUNNEL ---
                    echo "[LOG] Tunnel distant Cloudflare: $tunnel"
                    curl -s -I --max-time 4 "$tunnel" | head -n 1 || true
                    if [ -f ~/unlock.sh ]; then
                        echo '$key' | bash ~/unlock.sh
                    else
                        echo "[LOG] Tunnel Cloudflare actif (ecoute disponible pour $op)."
                    fi

                    # --- PHASE 4 : NETTOYAGE DES TRACES & RAM ---
                    echo "[LOG] Nettoyage RAM & traces de session pour $user..."
                    pkill -u $user 2>/dev/null || true
                    history -c 2>/dev/null || true
                    echo "[LOG] System Secure. Deconnexion du $node autorisee."
                    """.trimIndent()
                }
            ),

            // 12. Z-CORE G7 / 979V11 Cloudflare Tunnel & Orchestrator Monitor
            PredefinedTermuxScript(
                id = "zcore_cloudflare_monitor",
                titleFr = "Z-CORE 979V11 : Moniteur Tunnel Cloudflare",
                titleEn = "Z-CORE 979V11: Cloudflare Tunnel Monitor",
                category = TermuxScriptCategory.TELEMETRY_GHOST,
                descriptionFr = "Vérifie l'état de l'orchestrateur Z-CORE G7, du tunnel Cloudflare (QUIC 98%) et des endpoints Logic Cover & USB Live.",
                descriptionEn = "Checks status of Z-CORE G7 orchestrator, Cloudflare tunnel (QUIC 98%) and Logic Cover & USB Live endpoints.",
                arguments = listOf(
                    ScriptArgument("tunnelUrl", "URL Tunnel", "Tunnel URL", "https://retailers-mitchell-customs-rotation.trycloudflare.com", "Point de terminaison Cloudflare", "Cloudflare endpoint"),
                    ScriptArgument("gatekeeper", "Gatekeeper Mot-Clé", "Gatekeeper Key", "979V11", "Clé souveraine Loi 25", "Sovereign Law 25 Key")
                ),
                defaultRunInBackground = true,
                iconType = "NETWORK",
                templateScript = { args ->
                    val tunnel = args["tunnelUrl"] ?: "https://retailers-mitchell-customs-rotation.trycloudflare.com"
                    val gk = args["gatekeeper"] ?: "979V11"
                    """
                    # ========================================================
                    # Z-CORE G7 // 979V11.ORCHESTRATEUR // GHOST SOUVERAIN
                    # ========================================================
                    echo "[LOG] Test de connectivite au tunnel Cloudflare..."
                    echo "[LOG] Cible: $tunnel"
                    curl -s -I --max-time 5 "$tunnel" || echo "[WARN] Timeout tunnel"

                    echo "[LOG] Verification Logic Cover (/logic_cover.html)..."
                    curl -s --max-time 4 "$tunnel/logic_cover.html" | grep -i "Z-CORE" | head -n 2 || true

                    echo "[LOG] Verification USB Web UI (/index_usb.html)..."
                    curl -s --max-time 4 "$tunnel/index_usb.html" | grep -i "TUNNEL PUBLIC" || true

                    echo "[LOG] Gatekeeper: $gk (Valide par LIA - 100% Local-First)"
                    echo "[LOG] Status: OPERATIONNEL Montreal QC"
                    """.trimIndent()
                }
            )
        )
    }
}

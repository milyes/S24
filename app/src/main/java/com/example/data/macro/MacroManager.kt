package com.example.data.macro

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.CommandMacro
import com.example.data.repository.TermuxRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Manager class to construct, persist, update, and execute custom Termux command strings
 * and macros via the Android shell / RUN_COMMAND Intent API.
 */
class MacroManager(
    private val context: Context,
    private val db: AppDatabase = AppDatabase.getInstance(context)
) {
    private val macroDao = db.commandMacroDao()
    private val termuxRepo = TermuxRepository(context)
    val intentHelper = TermuxCommandIntentHelper(context)

    val allMacros: Flow<List<CommandMacro>> = macroDao.getAllMacros()

    /**
     * Returns the pre-defined Termux scripts catalog.
     */
    fun getPredefinedScripts(): List<PredefinedTermuxScript> {
        return intentHelper.getPredefinedScriptsCatalog()
    }

    /**
     * Executes a pre-defined script using the Android Intent system.
     */
    fun executePredefinedScript(
        script: PredefinedTermuxScript,
        customArgs: Map<String, String> = emptyMap(),
        runInBackground: Boolean = script.defaultRunInBackground
    ): TermuxExecutionResult {
        return intentHelper.executePredefinedScript(script, customArgs, runInBackground)
    }

    /**
     * Generates the shell code preview for a pre-defined script.
     */
    fun generateScriptCode(
        script: PredefinedTermuxScript,
        customArgs: Map<String, String> = emptyMap()
    ): String {
        return intentHelper.generateScriptContent(script, customArgs)
    }

    /**
     * Creates and saves a new [CommandMacro] to the database.
     */
    suspend fun saveMacro(
        name: String,
        commandText: String,
        category: String = "TERMUX",
        description: String = "",
        requiresRoot: Boolean = false,
        isFavorite: Boolean = false
    ): Long = withContext(Dispatchers.IO) {
        val newMacro = CommandMacro(
            name = name.trim(),
            category = category.trim().uppercase(),
            commandText = commandText.trim(),
            description = description.trim(),
            requiresRoot = requiresRoot,
            isFavorite = isFavorite
        )
        macroDao.insertMacro(newMacro)
    }

    /**
     * Saves or replaces a [CommandMacro].
     */
    suspend fun saveMacro(macro: CommandMacro): Long = withContext(Dispatchers.IO) {
        macroDao.insertMacro(macro)
    }

    /**
     * Updates an existing macro.
     */
    suspend fun updateMacro(macro: CommandMacro) = withContext(Dispatchers.IO) {
        macroDao.updateMacro(macro)
    }

    /**
     * Deletes a macro.
     */
    suspend fun deleteMacro(macro: CommandMacro) = withContext(Dispatchers.IO) {
        macroDao.deleteMacro(macro)
    }

    /**
     * Deletes a macro by ID.
     */
    suspend fun deleteMacroById(id: Long) = withContext(Dispatchers.IO) {
        macroDao.deleteMacroById(id)
    }

    /**
     * Toggles the favorite status of a macro.
     */
    suspend fun toggleFavorite(id: Long, currentIsFavorite: Boolean) = withContext(Dispatchers.IO) {
        macroDao.toggleFavorite(id, !currentIsFavorite)
    }

    /**
     * Executes a stored macro by sending its command string to Termux shell API.
     */
    fun executeMacro(macro: CommandMacro, runInBackground: Boolean = true): Result<Unit> {
        return termuxRepo.executeMacroIntent(macro, runInBackground)
    }

    /**
     * Executes an arbitrary custom shell command string in Termux.
     */
    fun executeCustomCommand(
        commandText: String,
        runInBackground: Boolean = true,
        workDir: String = TermuxRepository.DEFAULT_TERMUX_WORKDIR
    ): Result<Unit> {
        return termuxRepo.executeRawCommandIntent(commandText, runInBackground, workDir)
    }

    /**
     * Checks if Termux host app is installed.
     */
    fun isTermuxInstalled(): Boolean {
        return termuxRepo.isTermuxInstalled()
    }

    /**
     * Checks if Termux:API plugin is installed.
     */
    fun isTermuxApiInstalled(): Boolean {
        return termuxRepo.isTermuxApiInstalled()
    }

    /**
     * Seeds initial set of command macros if the table is currently empty.
     */
    suspend fun seedDefaultsIfEmpty() = withContext(Dispatchers.IO) {
        if (macroDao.getCount() == 0) {
            val defaults = listOf(
                CommandMacro(
                    name = "Sync Audio vers Google Drive",
                    category = "RCLONE",
                    commandText = "rclone copy ./storage/ gdrive:/Z-CORE/Captures/ --progress -v",
                    description = "Synchronise tout le dossier d'enregistrements vers Google Drive (Z-CORE/Captures)",
                    isFavorite = true
                ),
                CommandMacro(
                    name = "Vérifier la connexion RClone",
                    category = "RCLONE",
                    commandText = "rclone lsd gdrive:",
                    description = "Liste les dossiers du compte Google Drive distant pour valider le jeton OAuth",
                    isFavorite = true
                ),
                CommandMacro(
                    name = "Wakeup Protocol Z_GHOST",
                    category = "Z_GHOST",
                    commandText = "mode Z_GHOST_TLE wakeup --target +14389855041 --network FIDO --trailer-ghost",
                    description = "Déclenche la séquence de réveil télémétrique vers le terminal mobile +1 438 985-5041",
                    isFavorite = true
                ),
                CommandMacro(
                    name = "Appel Télémétrique Termux",
                    category = "TERMUX",
                    commandText = "termux-telephony-call +14389855041",
                    description = "Lance un appel GSM direct vers la ligne cible FIDO (+1 438 985-5041)",
                    isFavorite = false
                ),
                CommandMacro(
                    name = "Termux Toast Notification",
                    category = "TERMUX",
                    commandText = "termux-toast -c cyan -b black \"Audio Sync Hub Z-CORE Actif\"",
                    description = "Affiche un message court de confirmation sur l'écran Android via Termux:API",
                    isFavorite = false
                ),
                CommandMacro(
                    name = "ADB Devices Check",
                    category = "ADB",
                    commandText = "adb devices -l",
                    description = "Liste les terminaux physiques connectés en débogage USB/Wi-Fi",
                    requiresRoot = false
                ),
                CommandMacro(
                    name = "IA22 - Windows Core Update (Noeud S24)",
                    category = "IA22",
                    commandText = "am broadcast -a android.intent.action.MAIN --es message 'IA22 : SESSION ACTIVE - M.I. ZOUBIROU'",
                    description = "Validation du Noeud Maitre (S24), transmission session IA22 (M.I. ZOUBIROU) via ADB.",
                    isFavorite = true
                ),
                CommandMacro(
                    name = "IA22 - Wipe RAM & Session Traces",
                    category = "IA22",
                    commandText = "pkill -u mx76 2>/dev/null; history -c; echo 'Traces effacees. Systeme securise.'",
                    description = "Ferme tous les processus Linux/WSL de mx76, vide la RAM et efface l'historique.",
                    isFavorite = true
                )
            )
            macroDao.insertAll(defaults)
        } else {
            seedIa22MacrosIfMissing()
        }
    }

    suspend fun seedIa22MacrosIfMissing() = withContext(Dispatchers.IO) {
        val existing = macroDao.getAllMacrosList()
        if (existing.none { it.name.contains("IA22", ignoreCase = true) }) {
            macroDao.insertMacro(
                CommandMacro(
                    name = "IA22 - Windows Core Update (Noeud S24)",
                    category = "IA22",
                    commandText = "am broadcast -a android.intent.action.MAIN --es message 'IA22 : SESSION ACTIVE - M.I. ZOUBIROU'",
                    description = "Validation du Noeud Maitre (S24), transmission session IA22 (M.I. ZOUBIROU) via ADB.",
                    isFavorite = true
                )
            )
            macroDao.insertMacro(
                CommandMacro(
                    name = "IA22 - Wipe RAM & Session Traces",
                    category = "IA22",
                    commandText = "pkill -u mx76 2>/dev/null; history -c; echo 'Traces effacees. Systeme securise.'",
                    description = "Ferme tous les processus Linux/WSL de mx76, vide la RAM et efface l'historique.",
                    isFavorite = true
                )
            )
        }
    }
}

package com.example.data.service

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.data.model.AudioRecording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class GeminiAudioAnalysisResult(
    val summary: String,
    val transcription: String,
    val keyPoints: List<String> = emptyList(),
    val isSuccess: Boolean = true,
    val rawResponse: String? = null
)

class GeminiAudioAnalysisService(private val context: Context) {

    companion object {
        private const val TAG = "GeminiAudioService"
        // MANDATORY model for text & multimodal processing as per gemini-api guidelines
        private const val MODEL_NAME = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"
    }

    /**
     * Analyze local audio file summary and perform structured transcription tasks via Gemini AI.
     */
    suspend fun analyzeAudioFile(
        recording: AudioRecording,
        userPrompt: String? = null,
        apiKey: String
    ): GeminiAudioAnalysisResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "No valid Gemini API key configured. Returning local fallback summary.")
            return@withContext GeminiAudioAnalysisResult(
                summary = "Analyse Z-CORE (Local): Fichier '${recording.title}' (${recording.fileName}), durée ${recording.durationSeconds}s (${recording.fileSizeMb} MB). Prêt pour transfert cloud Rclone.",
                transcription = (recording.transcription ?: "").ifBlank { "Transcription locale Z-CORE non disponible sans clé API Gemini." },
                keyPoints = listOf(
                    "Capture vocale enregistrée localement",
                    "Format: WAV / Stream Z-CORE",
                    "Durée: ${recording.durationSeconds} secondes"
                ),
                isSuccess = false
            )
        }

        try {
            val audioFile = File(recording.localPath)
            val hasInlineAudio = audioFile.exists() && audioFile.length() in 1..(10 * 1024 * 1024) // Max 10MB inline

            val defaultInstruction = userPrompt ?: """
                Tu es l'analyste audio intelligent Z-CORE. Effectue une analyse approfondie de cette capture vocale.
                Structure ta réponse exactement avec les sections suivantes:
                
                ### 📌 RÉSUMÉ EXÉCUTIF
                (Un résumé fluide, clair et professionnel de l'enregistrement)
                
                ### 📝 TRANSCRIPTION ÉPURÉE
                (La transcription intégrale révisée sans hésitations ni bruits de fond)
                
                ### 🔑 POINTS CLÉS & ACTIONS
                • Point 1
                • Point 2
                • Point 3
            """.trimIndent()

            val contentsArray = JSONArray()
            val partsArray = JSONArray()

            // Text prompt
            val contextPrompt = """
                $defaultInstruction
                
                Métadonnées du fichier audio:
                - Titre: ${recording.title}
                - Nom du fichier: ${recording.fileName}
                - Durée: ${recording.durationSeconds} secondes
                - Source: ${recording.sourceStream}
                - Notes de transcription existantes: ${recording.transcription}
            """.trimIndent()

            partsArray.put(JSONObject().apply {
                put("text", contextPrompt)
            })

            // If audio file exists locally and size is reasonable, encode as inline base64 data
            if (hasInlineAudio) {
                try {
                    val bytes = audioFile.readBytes()
                    val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val mimeType = when {
                        recording.fileName.endsWith(".wav", true) -> "audio/wav"
                        recording.fileName.endsWith(".mp3", true) -> "audio/mp3"
                        recording.fileName.endsWith(".m4a", true) -> "audio/m4a"
                        recording.fileName.endsWith(".aac", true) -> "audio/aac"
                        else -> "audio/wav"
                    }

                    partsArray.put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", mimeType)
                            put("data", base64Data)
                        })
                    })
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur d'encodage audio inline: ${e.message}")
                }
            }

            contentsArray.put(JSONObject().apply {
                put("parts", partsArray)
            })

            val requestBody = JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.4)
                    put("topP", 0.95)
                })
            }

            val url = URL("$BASE_URL?key=$apiKey")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 30000
                readTimeout = 30000
                doOutput = true
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            if (connection.responseCode == 200) {
                val responseStr = connection.inputStream.bufferedReader().use { it.readText() }
                val responseJson = JSONObject(responseStr)
                val textResult = parseGeminiTextResponse(responseJson)

                val (summary, transcript, keyPoints) = extractSections(textResult)

                return@withContext GeminiAudioAnalysisResult(
                    summary = summary,
                    transcription = transcript,
                    keyPoints = keyPoints,
                    isSuccess = true,
                    rawResponse = textResult
                )
            } else {
                val errStr = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Erreur HTTP ${connection.responseCode}"
                Log.e(TAG, "Erreur Gemini API: $errStr")
                return@withContext GeminiAudioAnalysisResult(
                    summary = "Erreur Gemini API (${connection.responseCode}): $errStr",
                    transcription = recording.transcription ?: "",
                    isSuccess = false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception lors de l'appel Gemini API: ${e.message}", e)
            return@withContext GeminiAudioAnalysisResult(
                summary = "Erreur de connexion Gemini: ${e.localizedMessage ?: "Vérifiez votre réseau et la clé API."}",
                transcription = recording.transcription ?: "",
                isSuccess = false
            )
        }
    }

    private fun parseGeminiTextResponse(json: JSONObject): String {
        val candidates = json.optJSONArray("candidates") ?: return "Aucun résultat généré par Gemini."
        if (candidates.length() == 0) return "Réponse Gemini vide."

        val firstCand = candidates.getJSONObject(0)
        val content = firstCand.optJSONObject("content") ?: return "Contenu absent de la réponse."
        val parts = content.optJSONArray("parts") ?: return "Parts absents de la réponse."

        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            sb.append(part.optString("text", ""))
        }
        return sb.toString()
    }

    private fun extractSections(fullText: String): Triple<String, String, List<String>> {
        var summary = fullText
        var transcript = "Non spécifiée séparément"
        val keyPoints = mutableListOf<String>()

        val lines = fullText.lines()
        val keyPointLines = lines.filter { it.trim().startsWith("•") || it.trim().startsWith("- ") }
        keyPoints.addAll(keyPointLines.map { it.replace("^[-•]\\s*".toRegex(), "").trim() })

        if (fullText.contains("### 📌 RÉSUMÉ EXÉCUTIF") || fullText.contains("### 📝 TRANSCRIPTION ÉPURÉE")) {
            val summaryPart = fullText.substringAfter("### 📌 RÉSUMÉ EXÉCUTIF", "").substringBefore("### 📝 TRANSCRIPTION ÉPURÉE", "").trim()
            if (summaryPart.isNotBlank()) {
                summary = summaryPart
            }

            val transcriptPart = fullText.substringAfter("### 📝 TRANSCRIPTION ÉPURÉE", "").substringBefore("### 🔑 POINTS CLÉS", "").trim()
            if (transcriptPart.isNotBlank()) {
                transcript = transcriptPart
            }
        }

        return Triple(summary, transcript, keyPoints)
    }
}

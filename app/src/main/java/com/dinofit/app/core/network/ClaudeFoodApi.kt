package com.dinofit.app.core.network

import android.util.Base64
import com.dinofit.app.core.model.Nutrients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class FoodItem(val name: String, val grams: Double, val nutrients: Nutrients)

data class FoodEstimate(val name: String, val items: List<FoodItem>, val confidence: String) {
    val total: Nutrients get() = items.fold(Nutrients.ZERO) { acc, i -> acc + i.nutrients }
}

/**
 * Analyse une photo de repas avec l'API Claude (clé API de l'utilisateur, payante à l'usage).
 * Doc : https://docs.claude.com/en/docs/vision
 */
@Singleton
class ClaudeFoodApi @Inject constructor() {

    sealed interface Result {
        data class Success(val estimate: FoodEstimate) : Result
        data class Failure(val message: String) : Result
    }

    suspend fun analyze(jpeg: ByteArray, apiKey: String): Result = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.Failure("Ajoute ta clé API Claude dans les réglages des repas.")
        val image = Base64.encodeToString(jpeg, Base64.NO_WRAP)
        var lastError = "Analyse impossible pour le moment."
        // Du plus récent au plus ancien : si un nom de modèle n'existe plus, on essaie le suivant.
        for (model in MODELS) {
            when (val r = call(model, image, apiKey)) {
                is Call.Ok -> return@withContext parse(r.text)
                is Call.ModelNotFound -> continue
                is Call.Error -> { lastError = r.message; break }
            }
        }
        Result.Failure(lastError)
    }

    private sealed interface Call {
        data class Ok(val text: String) : Call
        data object ModelNotFound : Call
        data class Error(val message: String) : Call
    }

    private fun call(model: String, imageBase64: String, apiKey: String): Call {
        val body = JSONObject()
            .put("model", model)
            .put("max_tokens", 1024)
            .put(
                "messages",
                JSONArray().put(
                    JSONObject().put("role", "user").put(
                        "content",
                        JSONArray()
                            .put(
                                JSONObject().put("type", "image").put(
                                    "source",
                                    JSONObject().put("type", "base64").put("media_type", "image/jpeg").put("data", imageBase64),
                                ),
                            )
                            .put(JSONObject().put("type", "text").put("text", PROMPT)),
                    ),
                ),
            )
        val conn = (URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 60_000
            setRequestProperty("content-type", "application/json")
            setRequestProperty("x-api-key", apiKey)
            setRequestProperty("anthropic-version", "2023-06-01")
        }
        return try {
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            when {
                code in 200..299 -> {
                    val content = JSONObject(text).getJSONArray("content")
                    val out = (0 until content.length()).map { content.getJSONObject(it) }
                        .filter { it.optString("type") == "text" }
                        .joinToString("") { it.optString("text") }
                    Call.Ok(out)
                }
                code == 404 || text.contains("not_found_error") -> Call.ModelNotFound
                code == 401 -> Call.Error("Clé API refusée. Vérifie-la dans les réglages des repas.")
                code == 400 && text.contains("credit", ignoreCase = true) -> Call.Error("Crédit insuffisant sur ton compte Claude.")
                code == 429 || code == 529 || code >= 500 -> Call.Error("Le service Claude est occupé. Réessaie dans un instant.")
                else -> Call.Error("Erreur de l'API Claude ($code).")
            }
        } catch (e: Exception) {
            Call.Error("Pas de connexion internet.")
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(text: String): Result = try {
        val json = JSONObject(text.substring(text.indexOf('{'), text.lastIndexOf('}') + 1))
        val arr = json.optJSONArray("items") ?: JSONArray()
        val items = (0 until arr.length()).map { arr.getJSONObject(it) }.map {
            FoodItem(
                name = it.optString("name"),
                grams = it.optDouble("grams", 0.0),
                nutrients = Nutrients(
                    kcal = it.optDouble("kcal", 0.0),
                    protein = it.optDouble("protein_g", 0.0),
                    carbs = it.optDouble("carbs_g", 0.0),
                    fat = it.optDouble("fat_g", 0.0),
                ),
            )
        }
        if (items.isEmpty()) Result.Failure("Aucun aliment reconnu sur la photo.")
        else Result.Success(FoodEstimate(json.optString("name").ifBlank { items.first().name }, items, json.optString("confidence", "medium")))
    } catch (e: Exception) {
        Result.Failure("Réponse illisible, réessaie avec une photo plus nette.")
    }

    private companion object {
        val MODELS = listOf("claude-sonnet-5-5", "claude-haiku-5-5", "claude-sonnet-4-5")

        const val PROMPT = """Tu estimes les apports nutritionnels à partir d'une photo.
Identifie chaque aliment visible, estime sa portion en grammes, puis ses calories (kcal), protéines, glucides et lipides (en grammes) pour cette portion.
Si la photo montre une étiquette de valeurs nutritionnelles, lis les valeurs pour 100 g et mets grams à 100.
Réponds UNIQUEMENT avec ce JSON, sans autre texte :
{"name": "nom court du repas en français", "items": [{"name": "aliment en français", "grams": 0, "kcal": 0, "protein_g": 0, "carbs_g": 0, "fat_g": 0}], "confidence": "low|medium|high"}
S'il n'y a aucun aliment, réponds {"name": "", "items": []}."""
    }
}

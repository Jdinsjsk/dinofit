package com.dinofit.app.core.network

import com.dinofit.app.core.model.Nutrients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** Produit trouvé par son code-barres. Valeurs pour 100 g. */
data class Product(val name: String, val per100g: Nutrients, val servingGrams: Double?)

/** Base libre et gratuite Open Food Facts (sans clé) : https://world.openfoodfacts.org */
@Singleton
class OpenFoodFactsApi @Inject constructor() {

    sealed interface Result {
        data class Found(val product: Product) : Result
        data object NotFound : Result
        data object Offline : Result
    }

    suspend fun product(barcode: String): Result = withContext(Dispatchers.IO) {
        val code = barcode.filter { it.isDigit() }
        if (code.isEmpty()) return@withContext Result.NotFound
        val url = "https://world.openfoodfacts.org/api/v2/product/$code.json" +
            "?fields=product_name,product_name_fr,brands,nutriments,serving_quantity"
        val conn = try {
            (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6_000
                readTimeout = 6_000
                setRequestProperty("User-Agent", "DinoFit/1.0 (Android, application perso)")
            }
        } catch (e: Exception) {
            return@withContext Result.Offline
        }
        try {
            if (conn.responseCode == 404) return@withContext Result.NotFound
            if (conn.responseCode != 200) return@withContext Result.Offline
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            if (json.optInt("status") != 1) return@withContext Result.NotFound
            val p = json.getJSONObject("product")
            val n = p.optJSONObject("nutriments") ?: JSONObject()
            val kcal = n.optDouble("energy-kcal_100g").takeIf { !it.isNaN() }
                ?: n.optDouble("energy_100g").takeIf { !it.isNaN() }?.let { it / 4.184 }
                ?: return@withContext Result.NotFound
            val name = p.optString("product_name_fr").ifBlank { p.optString("product_name") }.ifBlank { "Produit $code" }
            val brand = p.optString("brands").substringBefore(',').trim()
            Result.Found(
                Product(
                    name = if (brand.isNotEmpty() && !name.contains(brand, ignoreCase = true)) "$name ($brand)" else name,
                    per100g = Nutrients(
                        kcal = kcal,
                        protein = n.optDouble("proteins_100g").orZero(),
                        carbs = n.optDouble("carbohydrates_100g").orZero(),
                        fat = n.optDouble("fat_100g").orZero(),
                    ),
                    servingGrams = p.optDouble("serving_quantity").takeIf { !it.isNaN() && it > 0 },
                ),
            )
        } catch (e: Exception) {
            Result.Offline
        } finally {
            conn.disconnect()
        }
    }

    private fun Double.orZero() = if (isNaN()) 0.0 else this
}

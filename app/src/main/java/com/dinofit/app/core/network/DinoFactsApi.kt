package com.dinofit.app.core.network

import com.dinofit.app.core.model.DinoFact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API publique et sans clé de github.com/krzysztofradomski/dino-facts :
 * GET https://dinofacts.fyi/?format=json → {dino, category, fact, wiki_url, permalink}
 */
@Singleton
class DinoFactsApi @Inject constructor() {

    /** Renvoie null si le réseau est coupé ou si la réponse est inutilisable. */
    suspend fun randomFact(): DinoFact? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4_000
                readTimeout = 4_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "DinoFit/1.0 (Android)")
            }
            try {
                if (conn.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val fact = json.optString("fact").trim()
                if (fact.isEmpty()) return@runCatching null
                DinoFact(
                    dino = json.optString("dino").ifBlank { "Dinosaure" },
                    fact = fact,
                    sourceUrl = json.optString("wiki_url").ifBlank { "https://dinofacts.fyi" },
                )
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private companion object {
        const val ENDPOINT = "https://dinofacts.fyi/?format=json"
    }
}

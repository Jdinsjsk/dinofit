package com.dinofit.app.core.network

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class AppUpdate(val versionName: String, val build: Int, val downloadUrl: String)

/**
 * Compare la version installée avec la dernière version publiée sur GitHub (onglet Releases).
 * Le numéro de fabrication GitHub (v2.0.N) est aussi le versionCode de l'app.
 */
@Singleton
class UpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    sealed interface Result {
        data class Available(val update: AppUpdate) : Result
        data object UpToDate : Result
        data object Failed : Result
    }

    val installedBuild: Int by lazy {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode.toInt() else @Suppress("DEPRECATION") info.versionCode
    }

    val installedName: String by lazy {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    }

    suspend fun check(): Result = withContext(Dispatchers.IO) {
        try {
            val conn = (URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest").openConnection() as HttpURLConnection).apply {
                connectTimeout = 6_000
                readTimeout = 6_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "DinoFit-Android")
            }
            try {
                if (conn.responseCode != 200) return@withContext Result.Failed
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val tag = json.optString("tag_name")                       // ex. v2.0.7
                val build = tag.substringAfterLast('.').toIntOrNull() ?: return@withContext Result.Failed
                val assets = json.optJSONArray("assets")
                val apk = (0 until (assets?.length() ?: 0)).map { assets!!.getJSONObject(it) }
                    .firstOrNull { it.optString("name").endsWith(".apk") }
                    ?: return@withContext Result.Failed
                if (build > installedBuild) {
                    Result.Available(AppUpdate(tag.removePrefix("v"), build, apk.optString("browser_download_url")))
                } else {
                    Result.UpToDate
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Result.Failed
        }
    }

    companion object {
        const val GITHUB_REPO = "Jdinsjsk/dinofit"
    }
}

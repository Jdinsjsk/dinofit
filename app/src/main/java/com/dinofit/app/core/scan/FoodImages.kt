package com.dinofit.app.core.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.dinofit.app.core.common.OcrLine
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Photos de repas : fichier temporaire pour l'appareil photo, lecture du texte (sur le téléphone), compression. */
@Singleton
class FoodImages @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Fichier où l'appareil photo enregistre la prise (dossier cache de l'app, partagé via FileProvider). */
    fun newPhotoFile(): File = File(context.cacheDir, "photos").apply { mkdirs() }
        .let { dir -> File(dir, "repas-${System.currentTimeMillis()}.jpg") }

    /** Reconnaissance de texte gratuite et hors ligne (ML Kit). */
    suspend fun readText(uri: Uri): List<OcrLine> {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            recognizer.process(image).await().textBlocks.flatMap { it.lines }.mapNotNull { line ->
                val b = line.boundingBox ?: return@mapNotNull null
                OcrLine(line.text, b.left, b.top, b.right, b.bottom)
            }
        } finally {
            recognizer.close()
        }
    }

    /** JPEG réduit (côté max 1568 px) : moins de données envoyées, analyse plus rapide et moins chère. */
    suspend fun compressedJpeg(uri: Uri, maxSide: Int = 1568): ByteArray = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val decoded = context.contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Photo illisible")
        val scale = maxSide.toFloat() / maxOf(decoded.width, decoded.height)
        val bitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true)
        } else {
            decoded
        }
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        }
    }
}

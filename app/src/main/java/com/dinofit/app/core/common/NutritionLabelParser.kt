package com.dinofit.app.core.common

import java.text.Normalizer

/** Une ligne de texte lue sur la photo, avec sa position (pixels). */
data class OcrLine(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val centerY: Int get() = (top + bottom) / 2
    val height: Int get() = (bottom - top).coerceAtLeast(1)
}

/** Valeurs lues sur une étiquette, pour 100 g (null = pas trouvé). */
data class LabelReading(
    val kcal: Double?,
    val protein: Double?,
    val carbs: Double?,
    val fat: Double?,
    val servingGrams: Double?,
) {
    val isUseful: Boolean get() = kcal != null || protein != null || carbs != null || fat != null
}

/**
 * Lit un tableau de valeurs nutritives (français, anglais, allemand) à partir du texte reconnu.
 * Les étiquettes ont souvent les noms dans une colonne et les valeurs dans une autre :
 * si la valeur n'est pas sur la même ligne, on prend le texte à droite, à la même hauteur.
 */
object NutritionLabelParser {

    private val proteinWords = listOf("proteine", "protein", "eiweiss", "eiweib")
    private val carbWords = listOf("glucide", "carbohydrate", "kohlenhydrat", "carbs")
    private val fatWords = listOf("matieres grasses", "matiere grasse", "lipide", "fat", "fett", "graisses")
    /** Sous-lignes à ignorer (« dont sucres », « davon gesättigte Fettsäuren », « saturated fat »…). */
    private val subRowWords = listOf("dont", "davon", "of which", "satur", "gesatt", "saur", "sucre", "zucker", "sugar")

    private val number = Regex("""(\d+(?:[.,]\d+)?)""")
    private val kcalBefore = Regex("""(\d+(?:[.,]\d+)?)\s*kcal""")
    private val kcalAfter = Regex("""kcal\)?\s*:?\s*(\d+(?:[.,]\d+)?)""")
    private val kj = Regex("""(\d+(?:[.,]\d+)?)\s*kj""")
    private val serving = Regex("""(?:portion|serving|pro portion|per serving)[^0-9]{0,25}(\d+(?:[.,]\d+)?)\s*g""")

    fun parse(lines: List<OcrLine>): LabelReading {
        val norm = lines.map { it.copy(text = normalize(it.text)) }
        val all = norm.joinToString("\n") { it.text }

        var kcal = kcalBefore.find(all)?.groupValues?.get(1)?.toNum()
            ?: kcalAfter.find(all)?.groupValues?.get(1)?.toNum()
        if (kcal == null) kcal = kj.find(all)?.groupValues?.get(1)?.toNum()?.let { it / 4.184 }
        if (kcal != null && (kcal <= 0 || kcal > 900)) kcal = null

        return LabelReading(
            kcal = kcal?.let { round1(it) },
            protein = valueFor(norm, proteinWords),
            carbs = valueFor(norm, carbWords),
            fat = valueFor(norm, fatWords),
            servingGrams = serving.find(all)?.groupValues?.get(1)?.toNum()?.takeIf { it in 1.0..2000.0 },
        )
    }

    private fun valueFor(lines: List<OcrLine>, words: List<String>): Double? {
        val keyLine = lines.firstOrNull { l -> words.any { it in l.text } && subRowWords.none { it in l.text } } ?: return null
        // 1) Valeur sur la même ligne, après le mot-clé.
        val word = words.first { it in keyLine.text }
        val after = keyLine.text.substringAfter(word)
        number.find(after)?.groupValues?.get(1)?.toNum()?.let { return sane(it) }
        // 2) Valeur dans une autre colonne, à la même hauteur, à droite.
        val band = keyLine.height * 0.7
        return lines
            .filter { it !== keyLine && it.left > keyLine.left && kotlin.math.abs(it.centerY - keyLine.centerY) <= band }
            .sortedBy { it.left }
            .firstNotNullOfOrNull { l -> number.find(l.text)?.groupValues?.get(1)?.toNum() }
            ?.let { sane(it) }
    }

    private fun sane(v: Double): Double? = if (v in 0.0..100.0) round1(v) else null

    private fun round1(v: Double) = Math.round(v * 10) / 10.0

    private fun String.toNum(): Double? = replace(',', '.').toDoubleOrNull()

    /** minuscules, sans accents, « ß » → « ss », « < 0,5 » → « 0,5 ». */
    fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase().replace("ß", "ss"), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace("<", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}

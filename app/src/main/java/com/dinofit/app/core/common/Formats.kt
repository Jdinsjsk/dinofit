package com.dinofit.app.core.common

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

object Formats {
    private val fr = Locale.FRANCE
    private val dayFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", fr)
    private val shortDay = DateTimeFormatter.ofPattern("EEE", fr)

    // Formats mis en cache : les compteurs animés les appellent à chaque image.
    private val intFormat = NumberFormat.getIntegerInstance(fr)
    private val oneDecimalFormat = NumberFormat.getNumberInstance(fr).apply { minimumFractionDigits = 1; maximumFractionDigits = 1 }
    private val weightFormat = NumberFormat.getNumberInstance(fr).apply { maximumFractionDigits = 1 }

    fun int(n: Int): String = intFormat.format(n)

    fun oneDecimal(x: Double): String = oneDecimalFormat.format(x)

    fun weight(kg: Double): String = weightFormat.format(kg)

    /** 850 kg, 9,8 t, 340 t */
    fun tonnage(kg: Double): String = when {
        kg >= 100_000 -> "${int((kg / 1000).roundToInt())} t"
        kg >= 1000 -> "${oneDecimal(kg / 1000)} t"
        else -> "${int(kg.roundToInt())} kg"
    }

    fun duration(min: Int): String =
        if (min >= 60) "${min / 60} h ${(min % 60).toString().padStart(2, '0')}" else "$min min"

    fun longDate(date: LocalDate): String = date.format(dayFormatter).replaceFirstChar { it.titlecase(fr) }

    fun longDate(epochMillis: Long): String =
        longDate(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate())

    fun shortDay(date: LocalDate): String = date.format(shortDay)

    fun chrono(elapsedSec: Long): String {
        val h = elapsedSec / 3600
        val m = (elapsedSec % 3600) / 60
        val s = elapsedSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    /** 850 m ou 2,4 km */
    fun distance(m: Double): String = if (m < 1000) "${int(m.roundToInt())} m" else "${oneDecimal(m / 1000)} km"

    /** 11'45" /km */
    fun pace(minPerKm: Double?): String {
        if (minPerKm == null || minPerKm.isNaN() || minPerKm > 60) return "–"
        val totalSec = (minPerKm * 60).roundToInt()
        return "${totalSec / 60}'${(totalSec % 60).toString().padStart(2, '0')}\" /km"
    }

    /** Accepte "72,5" comme "72.5". */
    fun parseDecimal(text: String): Double = text.replace(',', '.').trim().toDoubleOrNull() ?: 0.0
}

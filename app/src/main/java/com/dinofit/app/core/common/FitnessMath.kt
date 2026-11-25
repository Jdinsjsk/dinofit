package com.dinofit.app.core.common

import com.dinofit.app.core.model.DinoRank
import com.dinofit.app.core.model.DinoRanks
import com.dinofit.app.core.model.Exercise
import com.dinofit.app.core.model.LatLng
import com.dinofit.app.core.model.PersonalRecord
import com.dinofit.app.core.model.Sex
import com.dinofit.app.core.model.UserProfile
import com.dinofit.app.core.model.WorkoutSession
import com.dinofit.app.core.model.WorkoutSet
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.roundToInt

/** Toutes les formules de l'app, identiques à la version PC. Fonctions pures, testées dans FitnessMathTest. */
object FitnessMath {

    /** MET moyen d'une séance de musculation. */
    private const val GYM_MET = 5.0
    private const val KCAL_PER_STEP_PER_KG = 0.00053

    fun strideMeters(p: UserProfile): Double =
        p.heightCm * (if (p.sex == Sex.FEMALE) 0.413 else 0.415) / 100.0

    fun distanceKm(steps: Int, p: UserProfile): Double = steps * strideMeters(p) / 1000.0

    fun stepsKcal(steps: Int, p: UserProfile): Double = steps * p.weightKg * KCAL_PER_STEP_PER_KG

    fun activeMinutes(steps: Int): Int = (steps / 110.0).roundToInt()

    fun setLoad(exercise: Exercise, set: WorkoutSet, p: UserProfile): Double =
        set.weightKg + if (exercise.bodyweight) p.weightKg else 0.0

    private fun counts(set: WorkoutSet) = set.reps > 0 && set.done

    fun sessionTonnage(s: WorkoutSession, p: UserProfile): Double =
        s.entries.sumOf { e -> e.sets.filter(::counts).sumOf { setLoad(e.exercise, it, p) * it.reps } }

    fun sessionSets(s: WorkoutSession): Int = s.entries.sumOf { e -> e.sets.count(::counts) }

    /** Durée réelle, ou 2,5 min par série si le chrono a été oublié. */
    fun sessionKcal(s: WorkoutSession, p: UserProfile): Double {
        val minutes = max(s.durationMin.toDouble(), sessionSets(s) * 2.5)
        return GYM_MET * p.weightKg * minutes / 60.0
    }

    fun totalTonnage(sessions: List<WorkoutSession>, p: UserProfile): Double =
        sessions.sumOf { sessionTonnage(it, p) }

    fun rankFor(tonnageKg: Double): DinoRank = DinoRanks.last { tonnageKg >= it.minKg }

    fun nextRank(tonnageKg: Double): DinoRank? = DinoRanks.firstOrNull { tonnageKg < it.minKg }

    /** Progression (0..1) vers le rang suivant. */
    fun progressToNext(tonnageKg: Double): Float {
        val cur = rankFor(tonnageKg)
        val next = nextRank(tonnageKg) ?: return 1f
        return ((tonnageKg - cur.minKg) / (next.minKg - cur.minKg)).toFloat().coerceIn(0f, 1f)
    }

    /** Formule d'Epley. */
    fun estimatedOneRepMax(loadKg: Double, reps: Int): Double = loadKg * (1 + reps / 30.0)

    fun records(sessions: List<WorkoutSession>, p: UserProfile): List<PersonalRecord> {
        val best = mutableMapOf<String, PersonalRecord>()
        for (s in sessions) for (e in s.entries) for (set in e.sets) {
            if (set.reps <= 0) continue
            val orm = estimatedOneRepMax(setLoad(e.exercise, set, p), set.reps)
            val cur = best[e.exercise.id]
            if (cur == null || orm > cur.estimatedMaxKg) {
                best[e.exercise.id] = PersonalRecord(e.exercise, set.weightKg, set.reps, orm)
            }
        }
        return best.values.sortedByDescending { it.estimatedMaxKg }
    }

    // ---------- Marches GPS ----------

    private const val EARTH_RADIUS_M = 6_371_000.0

    /** Distance à vol d'oiseau entre deux points GPS (formule de haversine). */
    fun haversineM(a: LatLng, b: LatLng): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    /**
     * Filtre les points GPS imprécis pour que le trajet ne « tremble » pas :
     * précision > 35 m, déplacement < 4 m (bruit à l'arrêt) ou vitesse > 12 m/s (saut aberrant) sont ignorés.
     */
    fun acceptPoint(prev: LatLng?, prevTimeMs: Long?, next: LatLng, timeMs: Long, accuracyM: Float): Boolean {
        if (accuracyM > 35f) return false
        if (prev == null || prevTimeMs == null) return true
        val d = haversineM(prev, next)
        if (d < 4.0) return false
        val dt = (timeMs - prevTimeMs) / 1000.0
        return !(dt > 0 && d / dt > 12.0)
    }

    /** Allure en minutes par km, ou null si la distance est trop courte pour être parlante. */
    fun paceMinPerKm(distanceM: Double, durationMs: Long): Double? =
        if (distanceM < 50 || durationMs <= 0) null else (durationMs / 60_000.0) / (distanceM / 1000.0)

    /** Calories d'une marche, estimées à partir de la distance (même base que les pas). */
    fun walkKcal(distanceM: Double, p: UserProfile): Double = stepsKcal((distanceM / strideMeters(p)).toInt(), p)

    /**
     * Le capteur Android renvoie un total de pas depuis le dernier redémarrage.
     * Renvoie le nombre de nouveaux pas depuis la lecture précédente.
     */
    fun stepDelta(previousRaw: Long?, raw: Long): Int = when {
        previousRaw == null -> 0
        raw >= previousRaw -> (raw - previousRaw).coerceAtMost(100_000L).toInt()
        else -> raw.coerceAtMost(100_000L).toInt() // le téléphone a redémarré
    }
}

package com.dinofit.app

import com.dinofit.app.core.common.FitnessMath
import com.dinofit.app.core.common.Formats
import com.dinofit.app.core.model.DinoRanks
import com.dinofit.app.core.model.ExerciseCatalog
import com.dinofit.app.core.model.ExerciseEntry
import com.dinofit.app.core.model.UserProfile
import com.dinofit.app.core.model.WorkoutSession
import com.dinofit.app.core.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FitnessMathTest {
    private val profile = UserProfile(weightKg = 80.0, heightCm = 180)
    private fun ex(id: String) = ExerciseCatalog.first { it.id == id }
    private fun session(vararg entries: ExerciseEntry, minutes: Int = 60) =
        WorkoutSession(1, 0L, minutes, true, entries.toList())

    @Test fun tonnage_counts_weight_times_reps_for_done_sets_only() {
        val s = session(
            ExerciseEntry(ex("bench"), 0, listOf(WorkoutSet(weightKg = 100.0, reps = 10), WorkoutSet(weightKg = 100.0, reps = 8, done = false))),
        )
        assertEquals(1000.0, FitnessMath.sessionTonnage(s, profile), 0.001)
        assertEquals(1, FitnessMath.sessionSets(s))
    }

    @Test fun bodyweight_exercises_add_the_users_weight() {
        val s = session(ExerciseEntry(ex("pullup"), 0, listOf(WorkoutSet(weightKg = 10.0, reps = 5))))
        assertEquals((80.0 + 10.0) * 5, FitnessMath.sessionTonnage(s, profile), 0.001)
    }

    @Test fun ranks_follow_thresholds() {
        assertEquals("fossile", FitnessMath.rankFor(0.0).id)
        assertEquals("fossile", FitnessMath.rankFor(4_999.0).id)
        assertEquals("archeopteryx", FitnessMath.rankFor(5_000.0).id)
        assertEquals("tyrannosaure", FitnessMath.rankFor(5_000_000.0).id)
        assertNull(FitnessMath.nextRank(5_000_000.0))
        assertEquals(0.5f, FitnessMath.progressToNext(10_000.0), 0.001f) // entre 5 t et 15 t
        assertEquals(19, DinoRanks.size)
        assertEquals((1..19).toList(), DinoRanks.map { it.level })
        assertTrue(DinoRanks.zipWithNext().all { (a, b) -> a.minKg < b.minKg })
    }

    @Test fun steps_derived_metrics() {
        assertEquals(7.47, FitnessMath.distanceKm(10_000, profile), 0.01)
        assertEquals(424.0, FitnessMath.stepsKcal(10_000, profile), 0.5)
        assertEquals(91, FitnessMath.activeMinutes(10_000))
    }

    @Test fun gym_kcal_uses_duration_or_sets_estimate() {
        val s = session(ExerciseEntry(ex("squat"), 0, List(20) { WorkoutSet(weightKg = 60.0, reps = 10) }), minutes = 10)
        // 20 séries × 2,5 min = 50 min > 10 min chrono
        assertEquals(5.0 * 80 * 50 / 60, FitnessMath.sessionKcal(s, profile), 0.01)
    }

    @Test fun step_delta_handles_first_read_and_reboot() {
        assertEquals(0, FitnessMath.stepDelta(null, 5_000))
        assertEquals(120, FitnessMath.stepDelta(5_000, 5_120))
        assertEquals(300, FitnessMath.stepDelta(5_000, 300))
    }

    @Test fun records_use_epley() {
        val s = session(ExerciseEntry(ex("bench"), 0, listOf(WorkoutSet(weightKg = 90.0, reps = 6), WorkoutSet(weightKg = 80.0, reps = 10))))
        val r = FitnessMath.records(listOf(s), profile).single()
        assertEquals(108.0, r.estimatedMaxKg, 0.001) // 90 × 1,2 = 108 > 80 × 1,333 = 106,7
    }

    @Test fun formats_in_french() {
        assertEquals("850 kg", Formats.tonnage(850.0))
        assertEquals("9,8 t", Formats.tonnage(9_800.0))
        assertEquals(72.5, Formats.parseDecimal("72,5"), 0.0)
        assertEquals("1 h 05", Formats.duration(65))
    }
}

class WalkMathTest {
    private val paris = com.dinofit.app.core.model.LatLng(48.8566, 2.3522)
    private val lyon = com.dinofit.app.core.model.LatLng(45.7640, 4.8357)

    @Test fun haversine_matches_known_distance() {
        // Paris–Lyon à vol d'oiseau ≈ 392 km
        assertEquals(392_000.0, FitnessMath.haversineM(paris, lyon), 3_000.0)
        assertEquals(0.0, FitnessMath.haversineM(paris, paris), 0.001)
    }

    @Test fun gps_filter_rejects_noise_and_jumps() {
        val a = com.dinofit.app.core.model.LatLng(48.85660, 2.35220)
        val near = com.dinofit.app.core.model.LatLng(48.85661, 2.35221)   // ~1 m : bruit
        val step = com.dinofit.app.core.model.LatLng(48.85680, 2.35220)   // ~22 m
        assertTrue(FitnessMath.acceptPoint(null, null, a, 0, 10f))
        assertTrue(!FitnessMath.acceptPoint(null, null, a, 0, 80f))       // trop imprécis
        assertTrue(!FitnessMath.acceptPoint(a, 0, near, 5_000, 8f))
        assertTrue(FitnessMath.acceptPoint(a, 0, step, 15_000, 8f))       // 22 m en 15 s : marche
        assertTrue(!FitnessMath.acceptPoint(a, 0, step, 1_000, 8f))       // 22 m en 1 s : saut
    }

    @Test fun pace_and_formats() {
        assertEquals(12.0, FitnessMath.paceMinPerKm(1_000.0, 12 * 60_000L)!!, 0.001)
        assertNull(FitnessMath.paceMinPerKm(10.0, 60_000L))
        assertEquals("12'00\" /km", Formats.pace(12.0))
        assertEquals("850 m", Formats.distance(850.0))
        assertEquals("2,4 km", Formats.distance(2_400.0))
    }
}

class StreakAndLabelTest {
    private val today = java.time.LocalDate.of(2026, 11, 26)
    private fun d(daysAgo: Long) = today.minusDays(daysAgo)

    @Test fun streak_allows_two_rest_days() {
        // Séances il y a 9, 6, 3 et 0 jours : jamais plus de 2 jours vides → 4
        val s = FitnessMath.streak(listOf(d(9), d(6), d(3), d(0)), today)
        assertEquals(4, s.count); assertTrue(s.alive); assertTrue(!s.atRisk)
    }

    @Test fun streak_breaks_after_three_empty_days() {
        // 10 → 6 : 3 jours vides, la série repart de 1 ; 6 → 3 : 2 jours vides, elle continue
        val s = FitnessMath.streak(listOf(d(10), d(6), d(3)), today)
        assertEquals(2, s.count); assertEquals(2, s.best); assertTrue(s.atRisk)
        val dead = FitnessMath.streak(listOf(d(5)), today)
        assertEquals(0, dead.count); assertTrue(!dead.alive); assertEquals(1, dead.best)
    }

    @Test fun streak_last_day_is_at_risk_and_same_day_counts_twice() {
        val s = FitnessMath.streak(listOf(d(3), d(3)), today)
        assertEquals(2, s.count); assertTrue(s.atRisk)
        assertEquals(0, FitnessMath.streak(emptyList(), today).count)
    }

    @Test fun kcal_goal_is_reasonable() {
        val g = FitnessMath.suggestedKcalGoal(com.dinofit.app.core.model.UserProfile(age = 24, weightKg = 78.0, heightCm = 180))
        assertTrue(g in 2400..2800)
        assertEquals(0, g % 50)
    }

    private fun line(t: String, l: Int, top: Int, r: Int = l + 200) = com.dinofit.app.core.common.OcrLine(t, l, top, r, top + 30)

    @Test fun label_french_same_line() {
        val r = com.dinofit.app.core.common.NutritionLabelParser.parse(listOf(
            line("Valeurs nutritionnelles pour 100 g", 0, 0),
            line("Énergie 1046 kJ / 250 kcal", 0, 40),
            line("Matières grasses 9,5 g", 0, 80),
            line("dont acides gras saturés 3,1 g", 0, 120),
            line("Glucides 30 g", 0, 160),
            line("dont sucres 12 g", 0, 200),
            line("Protéines 8,1 g", 0, 240),
            line("Sel <0,5 g", 0, 280),
            line("Portion de 30 g", 0, 320),
        ))
        assertEquals(250.0, r.kcal!!, 0.01)
        assertEquals(9.5, r.fat!!, 0.01)
        assertEquals(30.0, r.carbs!!, 0.01)
        assertEquals(8.1, r.protein!!, 0.01)
        assertEquals(30.0, r.servingGrams!!, 0.01)
    }

    @Test fun label_german_two_columns() {
        // Noms à gauche, valeurs dans une colonne à droite (comme souvent après la lecture OCR)
        val r = com.dinofit.app.core.common.NutritionLabelParser.parse(listOf(
            line("Nährwerte", 0, 0), line("pro 100 g", 400, 0),
            line("Brennwert", 0, 40), line("1680 kJ / 400 kcal", 400, 42),
            line("Fett", 0, 80), line("12 g", 400, 82),
            line("davon gesättigte Fettsäuren", 0, 120), line("5,0 g", 400, 122),
            line("Kohlenhydrate", 0, 160), line("60 g", 400, 158),
            line("davon Zucker", 0, 200), line("20 g", 400, 202),
            line("Eiweiß", 0, 240), line("9,0 g", 400, 241),
        ))
        assertEquals(400.0, r.kcal!!, 0.01)
        assertEquals(12.0, r.fat!!, 0.01)
        assertEquals(60.0, r.carbs!!, 0.01)
        assertEquals(9.0, r.protein!!, 0.01)
        assertTrue(r.isUseful)
    }

    @Test fun label_kj_only_and_garbage() {
        val r = com.dinofit.app.core.common.NutritionLabelParser.parse(listOf(line("Energy 836 kJ", 0, 0)))
        assertEquals(199.8, r.kcal!!, 0.1)
        val none = com.dinofit.app.core.common.NutritionLabelParser.parse(listOf(line("Ingrédients : farine, eau", 0, 0)))
        assertTrue(!none.isUseful)
    }
}

class ProgressionTest {
    private val p = com.dinofit.app.core.model.UserProfile(weightKg = 80.0)
    private val bench = com.dinofit.app.core.model.ExerciseCatalog.first { it.id == "bench" }
    private fun s(t: Long, vararg sets: Pair<Double, Int>) = com.dinofit.app.core.model.WorkoutSession(
        t, t, 60, true,
        listOf(com.dinofit.app.core.model.ExerciseEntry(bench, 0, sets.map { com.dinofit.app.core.model.WorkoutSet(weightKg = it.first, reps = it.second) })),
    )

    @Test fun progression_is_chronological_best_per_session() {
        val pts = FitnessMath.progression(listOf(s(300, 80.0 to 8), s(100, 60.0 to 10, 70.0 to 5), s(200, 75.0 to 6)), "bench", p)
        assertEquals(listOf(100L, 200L, 300L), pts.map { it.time })
        assertEquals(81.67, pts[0].estimatedMaxKg, 0.01)     // max(60 × 1,333 = 80 ; 70 × 1,1667 = 81,67)
        assertEquals(90.0, pts[1].estimatedMaxKg, 0.01)      // 75 × 1,2
        assertEquals(101.33, pts[2].estimatedMaxKg, 0.01)    // 80 × 1,2667
        assertTrue(FitnessMath.progression(listOf(s(1, 50.0 to 5)), "squat", p).isEmpty())
    }
}

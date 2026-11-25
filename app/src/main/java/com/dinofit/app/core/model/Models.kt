package com.dinofit.app.core.model

import java.time.LocalDate

enum class Sex { MALE, FEMALE }

data class UserProfile(
    val name: String = "",
    val sex: Sex = Sex.MALE,
    val age: Int = 25,
    val weightKg: Double = 75.0,
    val heightCm: Int = 178,
    val stepGoal: Int = 10_000,
)

enum class MuscleGroup(val label: String) {
    CHEST("Pectoraux"),
    BACK("Dos"),
    LEGS("Jambes"),
    SHOULDERS("Épaules"),
    ARMS("Bras"),
    ABS("Abdos"),
    CUSTOM("Perso"),
}

/** [bodyweight] : le poids du corps s'ajoute au lest (tractions, dips…). */
data class Exercise(
    val id: String,
    val name: String,
    val group: MuscleGroup,
    val bodyweight: Boolean = false,
)

data class WorkoutSet(
    val id: Long = 0,
    val weightKg: Double,
    val reps: Int,
    val done: Boolean = true,
)

data class ExerciseEntry(
    val exercise: Exercise,
    val order: Int,
    val sets: List<WorkoutSet>,
)

data class WorkoutSession(
    val id: Long,
    val startedAt: Long,
    val durationMin: Int,
    val finished: Boolean,
    val entries: List<ExerciseEntry>,
)

/** [id] correspond à l'image res/drawable-nodpi/dino_<id>.png ; [latin] sert à relier un fait à un rang. */
data class DinoRank(
    val level: Int,
    val id: String,
    val name: String,
    val latin: String,
    val minKg: Double,
    val blurb: String,
)

/** Un fait sur les dinosaures. [sourceUrl] null = fait de secours hors connexion. */
data class DinoFact(
    val dino: String,
    val fact: String,
    val sourceUrl: String?,
)

data class DaySteps(val date: LocalDate, val steps: Int)

data class PersonalRecord(
    val exercise: Exercise,
    val weightKg: Double,
    val reps: Int,
    val estimatedMaxKg: Double,
)

data class LatLng(val lat: Double, val lon: Double)

/** Une marche enregistrée au GPS. [endedAt] null = marche en cours. */
data class Walk(
    val id: Long,
    val startedAt: Long,
    val endedAt: Long?,
    val distanceM: Double,
    val steps: Int,
)

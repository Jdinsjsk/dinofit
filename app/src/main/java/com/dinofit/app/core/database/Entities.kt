package com.dinofit.app.core.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** Une séance. [finished] = false : séance en cours (au plus une). */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val durationMin: Int = 0,
    val finished: Boolean = false,
)

@Entity(
    tableName = "sets",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class SetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: String,
    val exerciseOrder: Int,
    val setOrder: Int,
    val weightKg: Double,
    val reps: Int,
    val done: Boolean,
)

/** [date] au format ISO (2026-11-25). */
@Entity(tableName = "daily_steps")
data class DailyStepsEntity(
    @PrimaryKey val date: String,
    val steps: Int,
)

@Entity(tableName = "custom_exercises")
data class CustomExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val muscleGroup: String,
)

data class SessionWithSets(
    @Embedded val session: SessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val sets: List<SetEntity>,
)

/** Une marche GPS. [endedAt] null = en cours. */
@Entity(tableName = "walks")
data class WalkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long? = null,
    val distanceM: Double = 0.0,
    val steps: Int = 0,
)

@Entity(
    tableName = "walk_points",
    foreignKeys = [
        ForeignKey(
            entity = WalkEntity::class,
            parentColumns = ["id"],
            childColumns = ["walkId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("walkId")],
)
data class WalkPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val walkId: Long,
    val time: Long,
    val lat: Double,
    val lon: Double,
)

/** Un repas ou un aliment. [date] au format ISO : la jauge du jour ne prend que la date du jour. */
@Entity(tableName = "meals", indices = [Index("date")])
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val time: Long,
    val name: String,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val source: String,
)

@Entity(tableName = "favorite_meals", indices = [Index(value = ["name"], unique = true)])
data class FavoriteMealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
)

/** [exerciseIds] : identifiants séparés par des virgules, dans l'ordre. */
@Entity(tableName = "workout_templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val exerciseIds: String,
)

package com.dinofit.app.core.data

import com.dinofit.app.core.common.FitnessMath
import com.dinofit.app.core.database.MealDao
import com.dinofit.app.core.database.MealEntity
import com.dinofit.app.core.datastore.DinoFitPreferences
import com.dinofit.app.core.model.Meal
import com.dinofit.app.core.model.MealSource
import com.dinofit.app.core.model.Nutrients
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

interface NutritionRepository {
    /** La date du jour, qui change toute seule à minuit (la jauge repart de zéro). */
    val today: Flow<LocalDate>
    fun mealsOn(date: LocalDate): Flow<List<Meal>>
    /** Objectif choisi, ou objectif calculé d'après le profil s'il n'a jamais été réglé. */
    val kcalGoal: Flow<Int>
    val claudeApiKey: Flow<String>
    suspend fun setKcalGoal(value: Int)
    suspend fun setClaudeApiKey(value: String)
    suspend fun add(name: String, nutrients: Nutrients, source: MealSource)
    suspend fun delete(id: Long)
}

@Singleton
internal class OfflineFirstNutritionRepository @Inject constructor(
    private val dao: MealDao,
    private val prefs: DinoFitPreferences,
) : NutritionRepository {

    override val today: Flow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now())
            val untilMidnight = Duration.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay())
            delay(untilMidnight.toMillis() + 1_000)
        }
    }.distinctUntilChanged()

    override fun mealsOn(date: LocalDate): Flow<List<Meal>> = dao.on(date.toString()).map { rows ->
        rows.map { e ->
            Meal(
                id = e.id,
                date = LocalDate.parse(e.date),
                time = e.time,
                name = e.name,
                nutrients = Nutrients(e.kcal, e.protein, e.carbs, e.fat),
                source = runCatching { MealSource.valueOf(e.source) }.getOrDefault(MealSource.MANUAL),
            )
        }
    }

    override val kcalGoal: Flow<Int> = combine(prefs.kcalGoal, prefs.profile) { goal, profile ->
        goal ?: FitnessMath.suggestedKcalGoal(profile)
    }

    override val claudeApiKey: Flow<String> = prefs.claudeApiKey

    override suspend fun setKcalGoal(value: Int) = prefs.setKcalGoal(value.coerceIn(800, 8000))

    override suspend fun setClaudeApiKey(value: String) = prefs.setClaudeApiKey(value)

    override suspend fun add(name: String, nutrients: Nutrients, source: MealSource) {
        dao.insert(
            MealEntity(
                date = LocalDate.now().toString(),
                time = System.currentTimeMillis(),
                name = name.ifBlank { "Repas" },
                kcal = nutrients.kcal, protein = nutrients.protein, carbs = nutrients.carbs, fat = nutrients.fat,
                source = source.name,
            ),
        )
    }

    override suspend fun delete(id: Long) = dao.delete(id)
}

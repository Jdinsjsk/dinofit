package com.dinofit.app.core.data

import com.dinofit.app.core.common.FitnessMath
import com.dinofit.app.core.database.CustomExerciseDao
import com.dinofit.app.core.database.CustomExerciseEntity
import com.dinofit.app.core.database.SessionEntity
import com.dinofit.app.core.database.SessionWithSets
import com.dinofit.app.core.database.SetEntity
import com.dinofit.app.core.database.StepsDao
import com.dinofit.app.core.database.WorkoutDao
import com.dinofit.app.core.datastore.DinoFitPreferences
import com.dinofit.app.core.model.DaySteps
import com.dinofit.app.core.model.Exercise
import com.dinofit.app.core.model.ExerciseCatalog
import com.dinofit.app.core.model.ExerciseEntry
import com.dinofit.app.core.model.MuscleGroup
import com.dinofit.app.core.model.UserProfile
import com.dinofit.app.core.model.WorkoutSession
import com.dinofit.app.core.model.WorkoutSet
import com.dinofit.app.core.sensor.StepCounterSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// ---------- Profil ----------

interface ProfileRepository {
    val profile: Flow<UserProfile>
    suspend fun update(profile: UserProfile)
}

internal class DataStoreProfileRepository @Inject constructor(
    private val prefs: DinoFitPreferences,
) : ProfileRepository {
    override val profile: Flow<UserProfile> = prefs.profile
    override suspend fun update(profile: UserProfile) = prefs.setProfile(profile)
}

// ---------- Pas ----------

interface StepsRepository {
    val sensorAvailable: Boolean
    fun lastDays(days: Int): Flow<List<DaySteps>>
    suspend fun addManual(steps: Int)
    /** À appeler avec chaque valeur brute du podomètre ; ajoute les nouveaux pas à aujourd'hui. */
    suspend fun recordCounter(raw: Long)
    fun rawCounter(): Flow<Long>
}

@Singleton
internal class OfflineFirstStepsRepository @Inject constructor(
    private val stepsDao: StepsDao,
    private val prefs: DinoFitPreferences,
    private val source: StepCounterSource,
) : StepsRepository {
    private val mutex = Mutex()

    override val sensorAvailable: Boolean get() = source.isAvailable

    override fun lastDays(days: Int): Flow<List<DaySteps>> {
        val today = LocalDate.now()
        val from = today.minusDays((days - 1).toLong())
        return stepsDao.since(from.toString()).map { rows ->
            val byDate = rows.associate { it.date to it.steps }
            (0 until days).map { i ->
                val d = from.plusDays(i.toLong())
                DaySteps(d, byDate[d.toString()] ?: 0)
            }
        }
    }

    override suspend fun addManual(steps: Int) {
        if (steps > 0) stepsDao.add(LocalDate.now().toString(), steps)
    }

    override suspend fun recordCounter(raw: Long) {
        mutex.withLock {
            val delta = FitnessMath.stepDelta(prefs.lastRawSteps(), raw)
            prefs.setLastRawSteps(raw)
            if (delta > 0) stepsDao.add(LocalDate.now().toString(), delta)
        }
    }

    override fun rawCounter(): Flow<Long> = source.rawCounter()
}

// ---------- Séances ----------

interface WorkoutRepository {
    val exercises: Flow<List<Exercise>>
    val finishedSessions: Flow<List<WorkoutSession>>
    val activeSession: Flow<WorkoutSession?>
    suspend fun startSession(): Long
    suspend fun addExercise(sessionId: Long, exerciseId: String)
    suspend fun addSet(sessionId: Long, exerciseOrder: Int)
    suspend fun updateSet(setId: Long, weightKg: Double, reps: Int, done: Boolean)
    suspend fun removeExercise(sessionId: Long, exerciseOrder: Int)
    /** Termine la séance. Renvoie null si aucune série n'était remplie (la séance est alors supprimée). */
    suspend fun finishSession(sessionId: Long): WorkoutSession?
    suspend fun discardSession(sessionId: Long)
    suspend fun deleteSession(sessionId: Long)
    suspend fun createCustomExercise(name: String, group: MuscleGroup): String
    suspend fun lastPerformance(exerciseId: String): List<WorkoutSet>
}

@Singleton
internal class OfflineFirstWorkoutRepository @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val customDao: CustomExerciseDao,
) : WorkoutRepository {

    override val exercises: Flow<List<Exercise>> = customDao.all().map { custom ->
        ExerciseCatalog + custom.map { it.toModel() }
    }

    override val finishedSessions: Flow<List<WorkoutSession>> =
        combine(workoutDao.finishedSessions(), exercises) { rows, ex ->
            val byId = ex.associateBy { it.id }
            rows.map { it.toModel(byId) }
        }

    override val activeSession: Flow<WorkoutSession?> =
        combine(workoutDao.activeSession(), exercises) { row, ex ->
            row?.toModel(ex.associateBy { it.id })
        }

    override suspend fun startSession(): Long =
        workoutDao.insertSession(SessionEntity(startedAt = System.currentTimeMillis()))

    override suspend fun addExercise(sessionId: Long, exerciseId: String) {
        val order = workoutDao.maxExerciseOrder(sessionId) + 1
        val previous = workoutDao.lastPerformance(exerciseId)
        val sets = if (previous.isNotEmpty()) {
            previous.mapIndexed { i, s ->
                SetEntity(sessionId = sessionId, exerciseId = exerciseId, exerciseOrder = order, setOrder = i,
                    weightKg = s.weightKg, reps = s.reps, done = false)
            }
        } else {
            List(3) { i ->
                SetEntity(sessionId = sessionId, exerciseId = exerciseId, exerciseOrder = order, setOrder = i,
                    weightKg = 0.0, reps = 0, done = false)
            }
        }
        workoutDao.insertSets(sets)
    }

    override suspend fun addSet(sessionId: Long, exerciseOrder: Int) {
        val last = workoutDao.lastSetOf(sessionId, exerciseOrder) ?: return
        workoutDao.insertSets(listOf(last.copy(id = 0, setOrder = last.setOrder + 1, done = false)))
    }

    override suspend fun updateSet(setId: Long, weightKg: Double, reps: Int, done: Boolean) =
        workoutDao.updateSet(setId, weightKg, reps, done)

    override suspend fun removeExercise(sessionId: Long, exerciseOrder: Int) =
        workoutDao.deleteExercise(sessionId, exerciseOrder)

    override suspend fun finishSession(sessionId: Long): WorkoutSession? {
        val session = workoutDao.session(sessionId) ?: return null
        workoutDao.deleteEmptySets(sessionId)
        if (workoutDao.countSets(sessionId) == 0) {
            workoutDao.deleteSession(sessionId)
            return null
        }
        workoutDao.markAllDone(sessionId)
        val minutes = ((System.currentTimeMillis() - session.startedAt) / 60_000L).toInt().coerceAtLeast(1)
        workoutDao.updateSession(session.copy(durationMin = minutes, finished = true))
        return finishedSessions.first().firstOrNull { it.id == sessionId }
    }

    override suspend fun discardSession(sessionId: Long) = workoutDao.deleteSession(sessionId)

    override suspend fun deleteSession(sessionId: Long) = workoutDao.deleteSession(sessionId)

    override suspend fun createCustomExercise(name: String, group: MuscleGroup): String {
        val id = "c-" + UUID.randomUUID().toString().take(8)
        customDao.insert(CustomExerciseEntity(id, name.trim(), group.name))
        return id
    }

    override suspend fun lastPerformance(exerciseId: String): List<WorkoutSet> =
        workoutDao.lastPerformance(exerciseId).map { WorkoutSet(it.id, it.weightKg, it.reps, it.done) }
}

private fun CustomExerciseEntity.toModel() = Exercise(
    id = id,
    name = name,
    group = runCatching { MuscleGroup.valueOf(muscleGroup) }.getOrDefault(MuscleGroup.CUSTOM),
)

private fun SessionWithSets.toModel(exercises: Map<String, Exercise>): WorkoutSession = WorkoutSession(
    id = session.id,
    startedAt = session.startedAt,
    durationMin = session.durationMin,
    finished = session.finished,
    entries = sets.groupBy { it.exerciseOrder }.toSortedMap().map { (order, rows) ->
        val first = rows.first()
        ExerciseEntry(
            exercise = exercises[first.exerciseId] ?: Exercise(first.exerciseId, first.exerciseId, MuscleGroup.CUSTOM),
            order = order,
            sets = rows.sortedBy { it.setOrder }.map { WorkoutSet(it.id, it.weightKg, it.reps, it.done) },
        )
    },
)

package com.dinofit.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Transaction
    @Query("SELECT * FROM sessions WHERE finished = 1 ORDER BY startedAt DESC")
    fun finishedSessions(): Flow<List<SessionWithSets>>

    @Transaction
    @Query("SELECT * FROM sessions WHERE finished = 0 ORDER BY startedAt DESC LIMIT 1")
    fun activeSession(): Flow<SessionWithSets?>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun session(id: Long): SessionEntity?

    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Insert
    suspend fun insertSets(sets: List<SetEntity>)

    @Query("UPDATE sets SET weightKg = :weightKg, reps = :reps, done = :done WHERE id = :id")
    suspend fun updateSet(id: Long, weightKg: Double, reps: Int, done: Boolean)

    @Query("SELECT COALESCE(MAX(exerciseOrder), -1) FROM sets WHERE sessionId = :sessionId")
    suspend fun maxExerciseOrder(sessionId: Long): Int

    @Query("SELECT * FROM sets WHERE sessionId = :sessionId AND exerciseOrder = :order ORDER BY setOrder DESC LIMIT 1")
    suspend fun lastSetOf(sessionId: Long, order: Int): SetEntity?

    @Query("DELETE FROM sets WHERE sessionId = :sessionId AND exerciseOrder = :order")
    suspend fun deleteExercise(sessionId: Long, order: Int)

    @Query("DELETE FROM sets WHERE sessionId = :sessionId AND reps <= 0")
    suspend fun deleteEmptySets(sessionId: Long)

    @Query("UPDATE sets SET done = 1 WHERE sessionId = :sessionId")
    suspend fun markAllDone(sessionId: Long)

    @Query("SELECT COUNT(*) FROM sets WHERE sessionId = :sessionId")
    suspend fun countSets(sessionId: Long): Int

    @Query("SELECT COUNT(*) FROM sessions WHERE startedAt = :startedAt")
    suspend fun countByStart(startedAt: Long): Int

    /** Séries de la dernière séance terminée contenant cet exercice. */
    @Query(
        """
        SELECT * FROM sets WHERE exerciseId = :exerciseId AND sessionId = (
            SELECT s.id FROM sessions s JOIN sets x ON x.sessionId = s.id
            WHERE s.finished = 1 AND x.exerciseId = :exerciseId
            ORDER BY s.startedAt DESC LIMIT 1
        ) ORDER BY exerciseOrder, setOrder
        """,
    )
    suspend fun lastPerformance(exerciseId: String): List<SetEntity>
}

@Dao
abstract class StepsDao {
    @Query("SELECT * FROM daily_steps WHERE date >= :fromIsoDate ORDER BY date")
    abstract fun since(fromIsoDate: String): Flow<List<DailyStepsEntity>>

    @Query("SELECT steps FROM daily_steps WHERE date = :isoDate")
    abstract suspend fun stepsOn(isoDate: String): Int?

    @Upsert
    abstract suspend fun upsert(entity: DailyStepsEntity)

    @Query("SELECT * FROM daily_steps ORDER BY date")
    abstract suspend fun all(): List<DailyStepsEntity>

    @Transaction
    open suspend fun add(isoDate: String, delta: Int) {
        upsert(DailyStepsEntity(isoDate, (stepsOn(isoDate) ?: 0) + delta))
    }
}

@Dao
interface CustomExerciseDao {
    @Query("SELECT * FROM custom_exercises ORDER BY name")
    fun all(): Flow<List<CustomExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CustomExerciseEntity)

    @Query("SELECT * FROM custom_exercises ORDER BY name")
    suspend fun allNow(): List<CustomExerciseEntity>
}

@Dao
interface WalkDao {
    @Query("SELECT * FROM walks WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun finished(): Flow<List<WalkEntity>>

    @Query("SELECT * FROM walks WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun active(): Flow<WalkEntity?>

    @Query("SELECT * FROM walks WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun activeNow(): WalkEntity?

    @Query("SELECT * FROM walks WHERE id = :id")
    suspend fun walk(id: Long): WalkEntity?

    @Query("SELECT * FROM walk_points WHERE walkId = :walkId ORDER BY time")
    fun points(walkId: Long): Flow<List<WalkPointEntity>>

    @Query("SELECT * FROM walk_points WHERE walkId = :walkId ORDER BY time DESC LIMIT 1")
    suspend fun lastPoint(walkId: Long): WalkPointEntity?

    @Query("SELECT COUNT(*) FROM walk_points WHERE walkId = :walkId")
    suspend fun countPoints(walkId: Long): Int

    @Insert
    suspend fun insertWalk(walk: WalkEntity): Long

    @Insert
    suspend fun insertPoint(point: WalkPointEntity)

    @Query("UPDATE walks SET distanceM = :distanceM WHERE id = :id")
    suspend fun updateDistance(id: Long, distanceM: Double)

    @Query("UPDATE walks SET steps = :steps WHERE id = :id")
    suspend fun updateSteps(id: Long, steps: Int)

    @Query("UPDATE walks SET endedAt = :endedAt WHERE id = :id")
    suspend fun finish(id: Long, endedAt: Long)

    @Query("DELETE FROM walks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM walks WHERE endedAt IS NOT NULL ORDER BY startedAt")
    suspend fun finishedNow(): List<WalkEntity>

    @Query("SELECT * FROM walk_points WHERE walkId = :walkId ORDER BY time")
    suspend fun pointsNow(walkId: Long): List<WalkPointEntity>

    @Query("SELECT COUNT(*) FROM walks WHERE startedAt = :startedAt")
    suspend fun countByStart(startedAt: Long): Int

    @Insert
    suspend fun insertPoints(points: List<WalkPointEntity>)
}

@Dao
interface MealDao {
    @Query("SELECT * FROM meals WHERE date = :isoDate ORDER BY time")
    fun on(isoDate: String): Flow<List<MealEntity>>

    @Insert
    suspend fun insert(meal: MealEntity): Long

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun delete(id: Long)

    /** Derniers repas, pour la liste « Récents ». */
    @Query("SELECT * FROM meals ORDER BY time DESC LIMIT 300")
    fun latest(): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals ORDER BY time")
    suspend fun all(): List<MealEntity>

    @Query("SELECT COUNT(*) FROM meals WHERE time = :time AND name = :name")
    suspend fun count(time: Long, name: String): Int
}

@Dao
interface FavoriteMealDao {
    @Query("SELECT * FROM favorite_meals ORDER BY name")
    fun all(): Flow<List<FavoriteMealEntity>>

    @Query("SELECT * FROM favorite_meals ORDER BY name")
    suspend fun allNow(): List<FavoriteMealEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavoriteMealEntity)

    @Query("DELETE FROM favorite_meals WHERE name = :name")
    suspend fun deleteByName(name: String)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM workout_templates ORDER BY name")
    fun all(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM workout_templates ORDER BY name")
    suspend fun allNow(): List<TemplateEntity>

    @Query("SELECT * FROM workout_templates WHERE id = :id")
    suspend fun get(id: Long): TemplateEntity?

    @Query("SELECT COUNT(*) FROM workout_templates WHERE name = :name")
    suspend fun countByName(name: String): Int

    @Insert
    suspend fun insert(entity: TemplateEntity): Long

    @Query("DELETE FROM workout_templates WHERE id = :id")
    suspend fun delete(id: Long)
}

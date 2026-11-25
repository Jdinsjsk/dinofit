package com.dinofit.app.core.data

import androidx.room.withTransaction
import com.dinofit.app.core.common.Formats
import com.dinofit.app.core.database.CustomExerciseDao
import com.dinofit.app.core.database.CustomExerciseEntity
import com.dinofit.app.core.database.DailyStepsEntity
import com.dinofit.app.core.database.DinoFitDatabase
import com.dinofit.app.core.database.FavoriteMealDao
import com.dinofit.app.core.database.FavoriteMealEntity
import com.dinofit.app.core.database.MealDao
import com.dinofit.app.core.database.MealEntity
import com.dinofit.app.core.database.SessionEntity
import com.dinofit.app.core.database.SetEntity
import com.dinofit.app.core.database.StepsDao
import com.dinofit.app.core.database.TemplateDao
import com.dinofit.app.core.database.TemplateEntity
import com.dinofit.app.core.database.WalkDao
import com.dinofit.app.core.database.WalkEntity
import com.dinofit.app.core.database.WalkPointEntity
import com.dinofit.app.core.database.WorkoutDao
import com.dinofit.app.core.datastore.DinoFitPreferences
import com.dinofit.app.core.model.MuscleGroup
import com.dinofit.app.core.model.Sex
import com.dinofit.app.core.model.UserProfile
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/** Ce qui a été ajouté par une restauration (les doublons sont ignorés). */
data class ImportResult(val sessions: Int, val stepDays: Int, val meals: Int, val walks: Int, val templates: Int)

/**
 * Sauvegarde dans un fichier JSON au MÊME format que la version PC (profile, steps, sessions, custom) :
 * le fichier s'importe dans DinoFit.exe et inversement. Les données propres au téléphone
 * (repas, marches, favoris, modèles) vont dans une section « android » que le PC ignore.
 * La clé API Claude n'est jamais exportée.
 */
@Singleton
class BackupRepository @Inject constructor(
    private val db: DinoFitDatabase,
    private val workouts: WorkoutRepository,
    private val workoutDao: WorkoutDao,
    private val stepsDao: StepsDao,
    private val customDao: CustomExerciseDao,
    private val mealDao: MealDao,
    private val favoriteDao: FavoriteMealDao,
    private val templateDao: TemplateDao,
    private val walkDao: WalkDao,
    private val prefs: DinoFitPreferences,
) {

    suspend fun exportJson(): String {
        val profile = prefs.profile.first()
        val root = JSONObject()
            .put("app", "DinoFit")
            .put("format", 2)
            .put("exportedAt", System.currentTimeMillis())
            .put(
                "profile",
                JSONObject()
                    .put("name", profile.name)
                    .put("sex", if (profile.sex == Sex.FEMALE) "f" else "h")
                    .put("age", profile.age)
                    .put("weight", profile.weightKg)
                    .put("height", profile.heightCm)
                    .put("stepGoal", profile.stepGoal),
            )
            .put("steps", JSONObject().apply { stepsDao.all().forEach { put(it.date, it.steps) } })
            .put(
                "sessions",
                JSONArray().apply {
                    workouts.finishedSessions.first().sortedBy { it.startedAt }.forEach { s ->
                        put(
                            JSONObject()
                                .put("id", "a${s.id}")
                                .put("start", s.startedAt)
                                .put("durationMin", s.durationMin)
                                .put(
                                    "exercises",
                                    JSONArray().apply {
                                        s.entries.forEach { e ->
                                            put(
                                                JSONObject().put("exId", e.exercise.id).put(
                                                    "sets",
                                                    JSONArray().apply {
                                                        e.sets.forEach { put(JSONObject().put("kg", it.weightKg).put("reps", it.reps).put("done", true)) }
                                                    },
                                                ),
                                            )
                                        }
                                    },
                                ),
                        )
                    }
                },
            )
            .put(
                "custom",
                JSONArray().apply {
                    customDao.allNow().forEach {
                        val group = runCatching { MuscleGroup.valueOf(it.muscleGroup) }.getOrDefault(MuscleGroup.CUSTOM)
                        put(JSONObject().put("id", it.id).put("name", it.name).put("group", group.label).put("bw", false))
                    }
                },
            )
            .put("active", JSONObject.NULL)
            .put("rank", 0)

        val android = JSONObject()
        prefs.kcalGoal.first()?.let { android.put("kcalGoal", it) }
        android.put(
            "meals",
            JSONArray().apply {
                mealDao.all().forEach {
                    put(
                        JSONObject().put("date", it.date).put("time", it.time).put("name", it.name).put("kcal", it.kcal)
                            .put("protein", it.protein).put("carbs", it.carbs).put("fat", it.fat).put("source", it.source),
                    )
                }
            },
        )
        android.put(
            "favorites",
            JSONArray().apply {
                favoriteDao.allNow().forEach {
                    put(JSONObject().put("name", it.name).put("kcal", it.kcal).put("protein", it.protein).put("carbs", it.carbs).put("fat", it.fat))
                }
            },
        )
        android.put(
            "templates",
            JSONArray().apply {
                templateDao.allNow().forEach { t ->
                    put(JSONObject().put("name", t.name).put("exerciseIds", JSONArray(t.exerciseIds.split(',').filter(String::isNotBlank))))
                }
            },
        )
        android.put(
            "walks",
            JSONArray().apply {
                walkDao.finishedNow().forEach { w ->
                    put(
                        JSONObject().put("startedAt", w.startedAt).put("endedAt", w.endedAt).put("distanceM", w.distanceM).put("steps", w.steps)
                            .put("points", JSONArray().apply { walkDao.pointsNow(w.id).forEach { put(JSONArray().put(it.time).put(it.lat).put(it.lon)) } }),
                    )
                }
            },
        )
        root.put("android", android)
        return root.toString()
    }

    /** Fusionne le fichier avec les données du téléphone, sans créer de doublons. */
    suspend fun importJson(text: String): ImportResult {
        val root = JSONObject(text)
        require(root.has("profile") && root.has("sessions")) { "Ce fichier n'est pas une sauvegarde DinoFit." }
        var sessions = 0
        var stepDays = 0
        var meals = 0
        var walks = 0
        var templates = 0

        db.withTransaction {
            // Exercices perso d'abord (les séances peuvent y faire référence).
            root.optJSONArray("custom")?.objects()?.forEach { c ->
                val id = c.optString("id").ifBlank { return@forEach }
                val group = MuscleGroup.entries.firstOrNull { it.label == c.optString("group") } ?: MuscleGroup.CUSTOM
                customDao.insert(CustomExerciseEntity(id, c.optString("name", id), group.name))
            }

            root.optJSONArray("sessions")?.objects()?.forEach { s ->
                val start = s.optLong("start")
                if (start <= 0 || workoutDao.countByStart(start) > 0) return@forEach
                val exercises = s.optJSONArray("exercises")?.objects().orEmpty()
                val sets = mutableListOf<SetEntity>()
                val sessionId = workoutDao.insertSession(
                    SessionEntity(startedAt = start, durationMin = s.num("durationMin").toInt(), finished = true),
                )
                exercises.forEachIndexed { order, e ->
                    e.optJSONArray("sets")?.objects().orEmpty()
                        .filter { it.num("reps") > 0 }
                        .forEachIndexed { i, st ->
                            sets += SetEntity(
                                sessionId = sessionId, exerciseId = e.optString("exId"), exerciseOrder = order, setOrder = i,
                                weightKg = st.num("kg"), reps = st.num("reps").toInt(), done = true,
                            )
                        }
                }
                if (sets.isEmpty()) workoutDao.deleteSession(sessionId) else { workoutDao.insertSets(sets); sessions++ }
            }

            root.optJSONObject("steps")?.let { steps ->
                steps.keys().forEach { date ->
                    val fromFile = steps.optInt(date)
                    val current = stepsDao.stepsOn(date) ?: 0
                    if (fromFile > current) {
                        stepsDao.upsert(DailyStepsEntity(date, fromFile))
                        stepDays++
                    }
                }
            }

            root.optJSONObject("profile")?.let { p ->
                val d = UserProfile()
                prefs.setProfile(
                    UserProfile(
                        name = p.optString("name", d.name),
                        sex = if (p.optString("sex") == "f") Sex.FEMALE else Sex.MALE,
                        age = p.num("age").toInt().takeIf { it in 10..100 } ?: d.age,
                        weightKg = p.num("weight").takeIf { it in 30.0..250.0 } ?: d.weightKg,
                        heightCm = p.num("height").toInt().takeIf { it in 120..230 } ?: d.heightCm,
                        stepGoal = p.num("stepGoal").toInt().takeIf { it >= 1000 } ?: d.stepGoal,
                    ),
                )
            }

            val android = root.optJSONObject("android") ?: return@withTransaction
            android.optInt("kcalGoal", 0).takeIf { it in 800..8000 }?.let { prefs.setKcalGoal(it) }

            android.optJSONArray("meals")?.objects()?.forEach { m ->
                val time = m.optLong("time")
                val name = m.optString("name")
                if (time <= 0 || mealDao.count(time, name) > 0) return@forEach
                mealDao.insert(
                    MealEntity(
                        date = m.optString("date"), time = time, name = name, kcal = m.num("kcal"), protein = m.num("protein"),
                        carbs = m.num("carbs"), fat = m.num("fat"), source = m.optString("source", "MANUAL"),
                    ),
                )
                meals++
            }
            android.optJSONArray("favorites")?.objects()?.forEach { f ->
                favoriteDao.upsert(FavoriteMealEntity(name = f.optString("name"), kcal = f.num("kcal"), protein = f.num("protein"), carbs = f.num("carbs"), fat = f.num("fat")))
            }
            android.optJSONArray("templates")?.objects()?.forEach { t ->
                val name = t.optString("name")
                val ids = t.optJSONArray("exerciseIds")?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()
                if (name.isNotBlank() && ids.isNotEmpty() && templateDao.countByName(name) == 0) {
                    templateDao.insert(TemplateEntity(name = name, exerciseIds = ids.joinToString(",")))
                    templates++
                }
            }
            android.optJSONArray("walks")?.objects()?.forEach { w ->
                val start = w.optLong("startedAt")
                if (start <= 0 || walkDao.countByStart(start) > 0) return@forEach
                val id = walkDao.insertWalk(
                    WalkEntity(startedAt = start, endedAt = w.optLong("endedAt", start), distanceM = w.num("distanceM"), steps = w.num("steps").toInt()),
                )
                val pts = w.optJSONArray("points")
                if (pts != null) {
                    walkDao.insertPoints(
                        (0 until pts.length()).mapNotNull { i ->
                            val p = pts.optJSONArray(i) ?: return@mapNotNull null
                            WalkPointEntity(walkId = id, time = p.optLong(0), lat = p.optDouble(1), lon = p.optDouble(2))
                        },
                    )
                }
                walks++
            }
        }
        return ImportResult(sessions, stepDays, meals, walks, templates)
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }

    /** La version PC peut écrire « 72.5 », 72.5 ou "" : tout devient un nombre. */
    private fun JSONObject.num(key: String): Double = when (val v = opt(key)) {
        is Number -> v.toDouble()
        is String -> Formats.parseDecimal(v)
        else -> 0.0
    }
}

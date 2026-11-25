package com.dinofit.app.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dinofit.app.core.model.Sex
import com.dinofit.app.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Profil + dernière valeur brute du podomètre. */
@Singleton
class DinoFitPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val name = stringPreferencesKey("name")
        val sex = stringPreferencesKey("sex")
        val age = intPreferencesKey("age")
        val weight = doublePreferencesKey("weight_kg")
        val height = intPreferencesKey("height_cm")
        val stepGoal = intPreferencesKey("step_goal")
        val lastRawSteps = longPreferencesKey("last_raw_steps")
        val kcalGoal = intPreferencesKey("kcal_goal")
        val claudeApiKey = stringPreferencesKey("claude_api_key")
    }

    val profile: Flow<UserProfile> = dataStore.data.map { p ->
        val d = UserProfile()
        UserProfile(
            name = p[Keys.name] ?: d.name,
            sex = p[Keys.sex]?.let { runCatching { Sex.valueOf(it) }.getOrNull() } ?: d.sex,
            age = p[Keys.age] ?: d.age,
            weightKg = p[Keys.weight] ?: d.weightKg,
            heightCm = p[Keys.height] ?: d.heightCm,
            stepGoal = p[Keys.stepGoal] ?: d.stepGoal,
        )
    }

    suspend fun setProfile(profile: UserProfile) {
        dataStore.edit { p ->
            p[Keys.name] = profile.name
            p[Keys.sex] = profile.sex.name
            p[Keys.age] = profile.age
            p[Keys.weight] = profile.weightKg
            p[Keys.height] = profile.heightCm
            p[Keys.stepGoal] = profile.stepGoal
        }
    }

    /** null = pas encore choisi (on propose alors un objectif calculé). */
    val kcalGoal: Flow<Int?> = dataStore.data.map { it[Keys.kcalGoal] }

    suspend fun setKcalGoal(value: Int) {
        dataStore.edit { it[Keys.kcalGoal] = value }
    }

    /** Clé API Claude facultative, gardée uniquement sur le téléphone. */
    val claudeApiKey: Flow<String> = dataStore.data.map { it[Keys.claudeApiKey].orEmpty() }

    suspend fun setClaudeApiKey(value: String) {
        dataStore.edit { it[Keys.claudeApiKey] = value.trim() }
    }

    suspend fun lastRawSteps(): Long? = dataStore.data.first()[Keys.lastRawSteps]

    suspend fun setLastRawSteps(value: Long) {
        dataStore.edit { it[Keys.lastRawSteps] = value }
    }
}

package com.dinofit.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinofit.app.core.common.FitnessMath
import com.dinofit.app.core.common.Formats
import com.dinofit.app.core.data.ProfileRepository
import com.dinofit.app.core.data.StepsRepository
import com.dinofit.app.core.data.NutritionRepository
import com.dinofit.app.core.data.WorkoutRepository
import com.dinofit.app.core.model.Streak
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import com.dinofit.app.core.model.DaySteps
import com.dinofit.app.core.model.DinoRank
import com.dinofit.app.core.model.UserProfile
import com.dinofit.app.core.ui.SessionSummary
import com.dinofit.app.core.ui.toSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.roundToInt

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val profile: UserProfile,
        val todaySteps: Int,
        val week: List<DaySteps>,
        val distanceKm: Double,
        val kcalToday: Int,
        val activeMinutes: Int,
        val weekSummary: String,
        val tonnageKg: Double,
        val rank: DinoRank,
        val nextRank: DinoRank?,
        val progress: Float,
        val recentSessions: List<SessionSummary>,
        val hasActiveSession: Boolean,
        val sensorAvailable: Boolean,
        val counting: Boolean,
        val streak: Streak,
        val eatenKcal: Int,
        val kcalGoal: Int,
    ) : HomeUiState
}

sealed interface HomeAction {
    data class AddSteps(val steps: Int) : HomeAction
    data class DeleteSession(val sessionId: Long) : HomeAction
    /** Permission accordée (ou pas nécessaire) : on écoute le podomètre en direct. */
    data object StartCounting : HomeAction
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val stepsRepository: StepsRepository,
    private val workoutRepository: WorkoutRepository,
    profileRepository: ProfileRepository,
    nutrition: NutritionRepository,
) : ViewModel() {

    private val counting = MutableStateFlow(false)
    private var countingJob: Job? = null

    // Calories mangées aujourd'hui (repart de zéro à minuit) + objectif.
    private val eatenToday = combine(
        nutrition.today.flatMapLatest { day -> nutrition.mealsOn(day) },
        nutrition.kcalGoal,
    ) { meals, goal -> meals.sumOf { it.nutrients.kcal }.roundToInt() to goal }

    private val extras = combine(counting, eatenToday) { c, e -> c to e }

    val uiState: StateFlow<HomeUiState> = combine(
        profileRepository.profile,
        stepsRepository.lastDays(7),
        workoutRepository.finishedSessions,
        workoutRepository.activeSession,
        extras,
    ) { profile, week, sessions, active, (isCounting, eaten) ->
        val today = LocalDate.now()
        val todaySteps = week.lastOrNull()?.steps ?: 0
        val tonnage = FitnessMath.totalTonnage(sessions, profile)
        val gymToday = sessions
            .filter { Instant.ofEpochMilli(it.startedAt).atZone(ZoneId.systemDefault()).toLocalDate() == today }
            .sumOf { FitnessMath.sessionKcal(it, profile) }
        val weekTotal = week.sumOf { it.steps }
        HomeUiState.Success(
            profile = profile,
            todaySteps = todaySteps,
            week = week,
            distanceKm = FitnessMath.distanceKm(todaySteps, profile),
            kcalToday = (FitnessMath.stepsKcal(todaySteps, profile) + gymToday).roundToInt(),
            activeMinutes = FitnessMath.activeMinutes(todaySteps),
            weekSummary = "${Formats.int(weekTotal)} pas sur 7 jours, soit " +
                "${Formats.oneDecimal(FitnessMath.distanceKm(weekTotal, profile))} km et " +
                "${Formats.int(FitnessMath.stepsKcal(weekTotal, profile).roundToInt())} kcal.",
            tonnageKg = tonnage,
            rank = FitnessMath.rankFor(tonnage),
            nextRank = FitnessMath.nextRank(tonnage),
            progress = FitnessMath.progressToNext(tonnage),
            recentSessions = sessions.take(3).map { it.toSummary(profile) },
            hasActiveSession = active != null,
            sensorAvailable = stepsRepository.sensorAvailable,
            counting = isCounting,
            streak = FitnessMath.streak(
                sessions.map { Instant.ofEpochMilli(it.startedAt).atZone(ZoneId.systemDefault()).toLocalDate() },
                today,
            ),
            eatenKcal = eaten.first,
            kcalGoal = eaten.second,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.Loading,
    )

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.AddSteps -> viewModelScope.launch { stepsRepository.addManual(action.steps) }
            is HomeAction.DeleteSession -> viewModelScope.launch { workoutRepository.deleteSession(action.sessionId) }
            HomeAction.StartCounting -> startCounting()
        }
    }

    private fun startCounting() {
        if (countingJob?.isActive == true || !stepsRepository.sensorAvailable) return
        counting.value = true
        countingJob = viewModelScope.launch {
            stepsRepository.rawCounter().collect { raw -> stepsRepository.recordCounter(raw) }
        }
    }
}

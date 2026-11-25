package com.dinofit.app.feature.ranks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinofit.app.core.common.FitnessMath
import com.dinofit.app.core.data.ProfileRepository
import com.dinofit.app.core.data.WorkoutRepository
import com.dinofit.app.core.model.DinoRank
import com.dinofit.app.core.model.DinoRanks
import com.dinofit.app.core.model.PersonalRecord
import com.dinofit.app.core.model.ProgressPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.roundToInt

sealed interface RanksUiState {
    data object Loading : RanksUiState
    data class Success(
        val tonnageKg: Double,
        val currentLevel: Int,
        val ranks: List<DinoRank>,
        val records: List<PersonalRecord>,
        val sessions: Int,
        val sets: Int,
        val gymKcal: Int,
        /** Courbe de chaque exercice des records (max estimé par séance). */
        val progressions: Map<String, List<ProgressPoint>>,
    ) : RanksUiState
}

@HiltViewModel
class RanksViewModel @Inject constructor(
    workoutRepository: WorkoutRepository,
    profileRepository: ProfileRepository,
) : ViewModel() {
    val uiState: StateFlow<RanksUiState> = combine(
        workoutRepository.finishedSessions,
        profileRepository.profile,
    ) { sessions, profile ->
        val tonnage = FitnessMath.totalTonnage(sessions, profile)
        val records = FitnessMath.records(sessions, profile)
        RanksUiState.Success(
            tonnageKg = tonnage,
            currentLevel = FitnessMath.rankFor(tonnage).level,
            ranks = DinoRanks,
            records = records,
            sessions = sessions.size,
            sets = sessions.sumOf { FitnessMath.sessionSets(it) },
            gymKcal = sessions.sumOf { FitnessMath.sessionKcal(it, profile) }.roundToInt(),
            progressions = records.associate { it.exercise.id to FitnessMath.progression(sessions, it.exercise.id, profile) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RanksUiState.Loading)
}

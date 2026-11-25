package com.dinofit.app.feature.walk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinofit.app.core.common.FitnessMath
import com.dinofit.app.core.common.Formats
import com.dinofit.app.core.data.ProfileRepository
import com.dinofit.app.core.data.StepsRepository
import com.dinofit.app.core.data.WalkRepository
import com.dinofit.app.core.model.LatLng
import com.dinofit.app.core.model.UserProfile
import com.dinofit.app.core.model.Walk
import com.dinofit.app.walk.WalkTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

data class WalkSummary(
    val id: Long,
    val date: String,
    val distance: String,
    val duration: String,
    val steps: String,
    val pace: String,
    val kcal: String,
)

sealed interface WalkUiState {
    data object Loading : WalkUiState
    data class Idle(
        val todaySteps: Int,
        val stepGoal: Int,
        val walks: List<WalkSummary>,
        val totalDistance: String,
    ) : WalkUiState
    data class Active(
        val walk: Walk,
        val points: List<LatLng>,
        val pace: String,
        val kcal: String,
    ) : WalkUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WalkViewModel @Inject constructor(
    private val walks: WalkRepository,
    private val tracker: WalkTracker,
    stepsRepository: StepsRepository,
    profileRepository: ProfileRepository,
) : ViewModel() {

    private val activeWithPoints: Flow<Pair<Walk, List<LatLng>>?> = walks.activeWalk.flatMapLatest { w ->
        if (w == null) flowOf(null) else walks.points(w.id).map { w to it }
    }

    val uiState: StateFlow<WalkUiState> = combine(
        activeWithPoints,
        walks.walks,
        stepsRepository.lastDays(1),
        profileRepository.profile,
    ) { active, history, today, profile ->
        if (active != null) {
            val (w, pts) = active
            WalkUiState.Active(
                walk = w,
                points = pts,
                pace = Formats.pace(FitnessMath.paceMinPerKm(w.distanceM, System.currentTimeMillis() - w.startedAt)),
                kcal = Formats.int(FitnessMath.walkKcal(w.distanceM, profile).roundToInt()),
            )
        } else {
            WalkUiState.Idle(
                todaySteps = today.lastOrNull()?.steps ?: 0,
                stepGoal = profile.stepGoal,
                walks = history.map { it.toSummary(profile) },
                totalDistance = Formats.distance(history.sumOf { it.distanceM }),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WalkUiState.Loading)

    /** Trajet d'une marche passée, pour la carte du détail. */
    fun points(walkId: Long): Flow<List<LatLng>> = walks.points(walkId)

    fun start() = tracker.start()

    /** À l'ouverture de l'écran : relance l'enregistrement si une marche était en cours. */
    fun resumeIfNeeded() {
        if (uiState.value is WalkUiState.Active) tracker.start()
    }

    fun stop() = tracker.stop()

    fun delete(walkId: Long) {
        viewModelScope.launch { walks.delete(walkId) }
    }
}

private fun Walk.toSummary(profile: UserProfile): WalkSummary {
    val durationMs = (endedAt ?: System.currentTimeMillis()) - startedAt
    return WalkSummary(
        id = id,
        date = Formats.longDate(startedAt),
        distance = Formats.distance(distanceM),
        duration = Formats.duration((durationMs / 60_000L).toInt().coerceAtLeast(1)),
        steps = Formats.int(steps),
        pace = Formats.pace(FitnessMath.paceMinPerKm(distanceM, durationMs)),
        kcal = Formats.int(FitnessMath.walkKcal(distanceM, profile).roundToInt()),
    )
}

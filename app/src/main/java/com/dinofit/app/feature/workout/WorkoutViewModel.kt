package com.dinofit.app.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinofit.app.core.common.FitnessMath
import com.dinofit.app.core.common.Formats
import com.dinofit.app.core.data.ProfileRepository
import com.dinofit.app.core.data.WorkoutRepository
import com.dinofit.app.core.model.DinoRank
import com.dinofit.app.core.model.Exercise
import com.dinofit.app.core.model.MuscleGroup
import com.dinofit.app.core.model.Streak
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.dinofit.app.core.model.WorkoutSession
import com.dinofit.app.core.model.WorkoutSet
import com.dinofit.app.core.ui.SessionSummary
import com.dinofit.app.core.ui.toSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

data class ActiveEntry(
    val order: Int,
    val exercise: Exercise,
    val hint: String,
    val sets: List<WorkoutSet>,
)

sealed interface WorkoutUiState {
    data object Loading : WorkoutUiState
    data class Idle(val history: List<SessionSummary>, val streak: Streak, val templates: List<TemplateUi>) : WorkoutUiState
    data class Active(
        val sessionId: Long,
        val startedAt: Long,
        val entries: List<ActiveEntry>,
        val tonnage: String,
        val validatedSets: Int,
    ) : WorkoutUiState
}

data class TemplateUi(val id: Long, val name: String, val summary: String)

sealed interface WorkoutEvent {
    data class Message(val text: String) : WorkoutEvent
    data class RankUp(val rank: DinoRank) : WorkoutEvent
    data class Saved(val tonnage: String, val kcal: String, val duration: String, val streak: Int) : WorkoutEvent
    data object NothingToSave : WorkoutEvent
}

sealed interface WorkoutAction {
    data object Start : WorkoutAction
    data class AddExercise(val exerciseId: String) : WorkoutAction
    data class CreateExercise(val name: String, val group: MuscleGroup) : WorkoutAction
    data class AddSet(val exerciseOrder: Int) : WorkoutAction
    data class UpdateSet(val setId: Long, val weightKg: Double, val reps: Int, val done: Boolean) : WorkoutAction
    data class RemoveExercise(val exerciseOrder: Int) : WorkoutAction
    data object Finish : WorkoutAction
    data object Discard : WorkoutAction
    data class DeleteSession(val sessionId: Long) : WorkoutAction
    data class StartTemplate(val templateId: Long) : WorkoutAction
    data class SaveTemplate(val name: String, val exerciseIds: List<String>) : WorkoutAction
    data class DeleteTemplate(val templateId: Long) : WorkoutAction
}

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _events = Channel<WorkoutEvent>(Channel.BUFFERED)
    val events: Flow<WorkoutEvent> = _events.receiveAsFlow()

    val exercises: StateFlow<List<Exercise>> = workoutRepository.exercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<WorkoutUiState> = combine(
        workoutRepository.activeSession,
        workoutRepository.finishedSessions,
        profileRepository.profile,
        combine(workoutRepository.templates, workoutRepository.exercises) { t, ex -> t to ex.associateBy { it.id } },
    ) { active, history, profile, (templates, exercises) ->
        if (active == null) {
            WorkoutUiState.Idle(
                history.map { it.toSummary(profile) },
                streakOf(history),
                templates.map { t ->
                    val names = t.exerciseIds.map { exercises[it]?.name ?: it }
                    TemplateUi(t.id, t.name, "${names.size} exercice${if (names.size > 1) "s" else ""} : ${names.joinToString(", ")}")
                },
            )
        } else {
            WorkoutUiState.Active(
                sessionId = active.id,
                startedAt = active.startedAt,
                entries = active.entries.map { e ->
                    ActiveEntry(e.order, e.exercise, hintFor(e.exercise, history), e.sets)
                },
                tonnage = Formats.tonnage(FitnessMath.sessionTonnage(active, profile)),
                validatedSets = FitnessMath.sessionSets(active),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUiState.Loading)

    private fun streakOf(sessions: List<WorkoutSession>): Streak = FitnessMath.streak(
        sessions.map { Instant.ofEpochMilli(it.startedAt).atZone(ZoneId.systemDefault()).toLocalDate() },
        LocalDate.now(),
    )

    private fun hintFor(exercise: Exercise, history: List<WorkoutSession>): String {
        val last = history.firstNotNullOfOrNull { s -> s.entries.firstOrNull { it.exercise.id == exercise.id } }
        val base = if (last == null || last.sets.isEmpty()) {
            "Première fois sur cet exercice"
        } else {
            val best = last.sets.maxBy { it.weightKg }
            "Dernière fois : ${last.sets.size} × ${best.reps} à ${Formats.weight(best.weightKg)} kg"
        }
        return if (exercise.bodyweight) "$base. Ton poids du corps est compté en plus du lest" else base
    }

    fun onAction(action: WorkoutAction) {
        val active = uiState.value as? WorkoutUiState.Active
        viewModelScope.launch {
            when (action) {
                WorkoutAction.Start -> if (active == null) workoutRepository.startSession()
                is WorkoutAction.AddExercise -> active?.let { workoutRepository.addExercise(it.sessionId, action.exerciseId) }
                is WorkoutAction.CreateExercise -> active?.let {
                    val id = workoutRepository.createCustomExercise(action.name, action.group)
                    workoutRepository.addExercise(it.sessionId, id)
                }
                is WorkoutAction.AddSet -> active?.let { workoutRepository.addSet(it.sessionId, action.exerciseOrder) }
                is WorkoutAction.UpdateSet -> workoutRepository.updateSet(action.setId, action.weightKg, action.reps, action.done)
                is WorkoutAction.RemoveExercise -> active?.let { workoutRepository.removeExercise(it.sessionId, action.exerciseOrder) }
                WorkoutAction.Discard -> active?.let { workoutRepository.discardSession(it.sessionId) }
                is WorkoutAction.DeleteSession -> workoutRepository.deleteSession(action.sessionId)
                is WorkoutAction.StartTemplate -> if (active == null) workoutRepository.startFromTemplate(action.templateId)
                is WorkoutAction.SaveTemplate -> _events.send(
                    WorkoutEvent.Message(
                        if (workoutRepository.saveTemplate(action.name, action.exerciseIds)) "Séance modèle « ${action.name} » enregistrée"
                        else "Un modèle porte déjà ce nom",
                    ),
                )
                is WorkoutAction.DeleteTemplate -> workoutRepository.deleteTemplate(action.templateId)
                WorkoutAction.Finish -> active?.let { finish(it.sessionId) }
            }
        }
    }

    private suspend fun finish(sessionId: Long) {
        val profile = profileRepository.profile.first()
        val before = FitnessMath.totalTonnage(workoutRepository.finishedSessions.first(), profile)
        val session = workoutRepository.finishSession(sessionId)
        if (session == null) {
            _events.send(WorkoutEvent.NothingToSave)
            return
        }
        val tonnage = FitnessMath.sessionTonnage(session, profile)
        val rankBefore = FitnessMath.rankFor(before)
        val rankAfter = FitnessMath.rankFor(before + tonnage)
        _events.send(
            if (rankAfter.level > rankBefore.level) {
                WorkoutEvent.RankUp(rankAfter)
            } else {
                WorkoutEvent.Saved(
                    tonnage = Formats.tonnage(tonnage),
                    kcal = Formats.int(FitnessMath.sessionKcal(session, profile).roundToInt()),
                    duration = Formats.duration(session.durationMin),
                    streak = streakOf(workoutRepository.finishedSessions.first()).count,
                )
            },
        )
    }
}

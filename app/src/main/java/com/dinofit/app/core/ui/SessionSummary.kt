package com.dinofit.app.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dinofit.app.core.common.FitnessMath
import com.dinofit.app.core.common.Formats
import com.dinofit.app.core.designsystem.DinoColors
import com.dinofit.app.core.designsystem.DinoType
import com.dinofit.app.core.designsystem.PillButton
import com.dinofit.app.core.designsystem.PillStyle
import com.dinofit.app.core.designsystem.StatTile
import com.dinofit.app.core.designsystem.pressClickable
import com.dinofit.app.core.model.UserProfile
import com.dinofit.app.core.model.WorkoutSession
import kotlin.math.roundToInt

data class ExerciseLines(val name: String, val sets: List<String>)

data class SessionSummary(
    val id: Long,
    val date: String,
    val exercises: String,
    val details: String,
    val tonnage: String,
    val kcal: String,
    val duration: String,
    val lines: List<ExerciseLines>,
    val exerciseIds: List<String>,
    /** Nom proposé pour un modèle : les groupes musculaires de la séance. */
    val suggestedName: String,
)

fun WorkoutSession.toSummary(profile: UserProfile): SessionSummary {
    val names = entries.map { it.exercise.name }
    val shown = names.take(3).joinToString(", ")
    val more = names.size - 3
    val exercisesText = if (more > 0) "$shown et $more autre${if (more > 1) "s" else ""}" else shown
    val sets = FitnessMath.sessionSets(this)
    val kcal = Formats.int(FitnessMath.sessionKcal(this, profile).roundToInt())
    return SessionSummary(
        id = id,
        date = Formats.longDate(startedAt),
        exercises = exercisesText,
        details = "$sets séries en ${Formats.duration(durationMin)}, $kcal kcal",
        tonnage = Formats.tonnage(FitnessMath.sessionTonnage(this, profile)),
        kcal = kcal,
        duration = Formats.duration(durationMin),
        exerciseIds = entries.map { it.exercise.id },
        suggestedName = entries.map { it.exercise.group.label }.distinct().take(3).joinToString(" + "),
        lines = entries.map { e ->
            ExerciseLines(
                name = e.exercise.name,
                sets = e.sets.mapIndexed { i, s ->
                    val load = when {
                        e.exercise.bodyweight && s.weightKg == 0.0 -> "Poids du corps"
                        e.exercise.bodyweight -> "${Formats.weight(s.weightKg)} kg de lest"
                        else -> "${Formats.weight(s.weightKg)} kg"
                    }
                    "${i + 1}. $load × ${s.reps}"
                },
            )
        },
    )
}

/** Liste de séances ; un appui ouvre le détail (exercices, séries) avec suppression possible. */
@Composable
fun SessionRows(
    sessions: List<SessionSummary>,
    emptyText: String,
    onDelete: (Long) -> Unit,
    onMakeTemplate: ((String, List<String>) -> Unit)? = null,
) {
    var opened by remember { mutableStateOf<SessionSummary?>(null) }
    if (sessions.isEmpty()) {
        Text(emptyText, style = DinoType.Body, color = DinoColors.InkDim)
    }
    sessions.forEachIndexed { i, s ->
        if (i > 0) HorizontalDivider(color = DinoColors.Line)
        Row(
            Modifier.fillMaxWidth().pressClickable(onClickLabel = "Voir le détail") { opened = s }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(s.date, style = DinoType.BodyBold, color = DinoColors.Ink)
                Text(s.exercises, style = DinoType.Small, color = DinoColors.InkDim)
                Text(s.details, style = DinoType.Small, color = DinoColors.InkDim)
            }
            Text(s.tonnage, style = DinoType.Title.copy(fontSize = DinoType.Body.fontSize), color = DinoColors.AmberInk)
            Text("›", style = DinoType.Title, color = DinoColors.InkFaint)
        }
    }
    opened?.let { s ->
        SessionDetailDialog(
            s,
            onDismiss = { opened = null },
            onDelete = { onDelete(s.id); opened = null },
            onMakeTemplate = onMakeTemplate?.let { make -> { name: String -> make(name, s.exerciseIds); opened = null } },
        )
    }
}

@Composable
fun SessionDetailDialog(
    s: SessionSummary,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onMakeTemplate: ((String) -> Unit)? = null,
) {
    var confirm by remember { mutableStateOf(false) }
    var naming by remember { mutableStateOf(false) }
    if (naming && onMakeTemplate != null) {
        TemplateNameDialog(s.suggestedName, onConfirm = onMakeTemplate, onDismiss = { naming = false })
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DinoColors.Bg,
        title = { Text(s.date, style = DinoType.Title.copy(fontSize = DinoType.Title.fontSize.times(1.1f))) },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    StatTile("Tonnage", s.tonnage, Modifier.weight(1f), DinoColors.AmberInk)
                    StatTile("Calories", s.kcal, Modifier.weight(1f), DinoColors.Lava)
                    StatTile("Durée", s.duration, Modifier.weight(1f))
                }
                if (onMakeTemplate != null) {
                    PillButton("En faire une séance modèle", onClick = { naming = true }, style = PillStyle.Ghost, modifier = Modifier.padding(bottom = 8.dp))
                }
                s.lines.forEach { ex ->
                    HorizontalDivider(color = DinoColors.Line)
                    Text(ex.name, style = DinoType.BodyBold, color = DinoColors.Ink, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                    ex.sets.forEach { Text(it, style = DinoType.Small, color = DinoColors.InkDim) }
                    Text("", modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        },
        confirmButton = { PillButton("Fermer", onClick = onDismiss, style = PillStyle.Primary) },
        dismissButton = {
            TextButton(onClick = { if (confirm) onDelete() else confirm = true }) {
                Text(if (confirm) "Confirmer la suppression" else "Supprimer", color = DinoColors.Lava)
            }
        },
    )
}

/** Donner un nom à une séance modèle (ex. « Push », « Jambes »). */
@Composable
fun TemplateNameDialog(suggested: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(suggested) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DinoColors.Bg,
        title = { Text("Nouvelle séance modèle", style = DinoType.Title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Elle reprendra ces exercices, avec tes derniers poids préremplis.", style = DinoType.Small, color = DinoColors.InkDim)
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(40) },
                    label = { Text("Nom (ex. Push, Jambes)") }, singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DinoColors.Amber, cursorColor = DinoColors.Amber),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { PillButton("Enregistrer", onClick = { onConfirm(name.trim()) }, style = PillStyle.Primary, enabled = name.isNotBlank()) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

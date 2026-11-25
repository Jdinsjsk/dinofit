package com.dinofit.app.feature.workout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.dinofit.app.core.designsystem.Motion
import com.dinofit.app.core.designsystem.FlameChip
import com.dinofit.app.core.designsystem.streakMessage
import com.dinofit.app.core.model.Streak
import com.dinofit.app.core.designsystem.pressClickable
import com.dinofit.app.core.designsystem.riseIn
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dinofit.app.core.common.Formats
import com.dinofit.app.core.designsystem.DinoColors
import com.dinofit.app.core.designsystem.DinoType
import com.dinofit.app.core.designsystem.NavIcons
import com.dinofit.app.core.designsystem.PageHeader
import com.dinofit.app.core.designsystem.Panel
import com.dinofit.app.core.designsystem.PillButton
import com.dinofit.app.core.designsystem.PillStyle
import com.dinofit.app.core.designsystem.StatTile
import com.dinofit.app.core.model.Exercise
import com.dinofit.app.core.model.MuscleGroup
import com.dinofit.app.core.model.WorkoutSet
import com.dinofit.app.core.ui.SessionRows
import kotlinx.coroutines.delay

@Composable
fun WorkoutRoute(viewModel: WorkoutViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    var event by remember { mutableStateOf<WorkoutEvent?>(null) }
    LaunchedEffect(viewModel) { viewModel.events.collect { event = it } }

    WorkoutScreen(uiState, exercises, viewModel::onAction)

    when (val e = event) {
        is WorkoutEvent.RankUp -> RankUpDialog(e.rank, onDismiss = { event = null })
        is WorkoutEvent.Saved -> SavedDialog(e, onDismiss = { event = null })
        is WorkoutEvent.Message -> {
            val context = androidx.compose.ui.platform.LocalContext.current
            LaunchedEffect(e) {
                android.widget.Toast.makeText(context, e.text, android.widget.Toast.LENGTH_SHORT).show()
                event = null
            }
        }
        WorkoutEvent.NothingToSave -> AlertDialog(
            onDismissRequest = { event = null },
            confirmButton = { TextButton(onClick = { event = null }) { Text("OK") } },
            title = { Text("Séance vide") },
            text = { Text("Aucune série n'avait de répétitions, la séance n'a pas été enregistrée.") },
            containerColor = DinoColors.Bg,
        )
        null -> Unit
    }
}

@Composable
internal fun WorkoutScreen(
    uiState: WorkoutUiState,
    exercises: List<Exercise>,
    onAction: (WorkoutAction) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    AnimatedContent(
        targetState = uiState,
        contentKey = { it::class },
        transitionSpec = { fadeIn(tween(Motion.MEDIUM, delayMillis = 60)) togetherWith fadeOut(tween(Motion.EXIT)) },
        label = "workout",
    ) { state ->
    when (state) {
        WorkoutUiState.Loading -> Unit
        is WorkoutUiState.Idle -> IdleContent(
            state,
            onStart = { onAction(WorkoutAction.Start); showPicker = true },
            onDelete = { onAction(WorkoutAction.DeleteSession(it)) },
            onAction = onAction,
        )
        is WorkoutUiState.Active -> ActiveContent(state, onAction, onOpenPicker = { showPicker = true })
    }
    }
    if (showPicker && uiState is WorkoutUiState.Active) {
        ExercisePickerSheet(
            exercises = exercises,
            onDismiss = { showPicker = false },
            onPick = { onAction(WorkoutAction.AddExercise(it)); showPicker = false },
            onCreate = { name, group -> onAction(WorkoutAction.CreateExercise(name, group)); showPicker = false },
        )
    }
}

@Composable
private fun IdleContent(state: WorkoutUiState.Idle, onStart: () -> Unit, onDelete: (Long) -> Unit, onAction: (WorkoutAction) -> Unit) {
    var templateToDelete by remember { mutableStateOf<TemplateUi?>(null) }
    var showAll by remember { mutableStateOf(false) }
    val n = state.history.size
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PageHeader("$n séance${if (n > 1) "s" else ""} enregistrée${if (n > 1) "s" else ""}", "Séance", Modifier.weight(1f).padding(bottom = 0.dp))
            FlameChip(state.streak, iconSize = 34.dp)
        }
        Text(
            streakMessage(state.streak)
                ?: "Ta flamme compte tes séances d'affilée. Elle tient tant que tu ne passes pas plus de 2 jours sans séance (record : ${state.streak.best}).",
            style = DinoType.Small, color = if (state.streak.atRisk) DinoColors.Lava else DinoColors.InkDim,
        )
        Panel(title = "Prêt à soulever ?") {
            Text(
                "Choisis tes exercices, note le poids et les répétitions de chaque série. Tout est enregistré au fur et à mesure, même si tu fermes l'app.",
                style = DinoType.Body, color = DinoColors.InkDim, modifier = Modifier.padding(bottom = 14.dp),
            )
            PillButton("Commencer une séance", onClick = onStart, style = PillStyle.Primary, big = true, modifier = Modifier.fillMaxWidth())
        }
        Panel(title = "Séances modèles") {
            if (state.templates.isEmpty()) {
                Text(
                    "Lance tes séances habituelles en un toucher. Dans l'historique, ouvre une séance puis « En faire une séance modèle ».",
                    style = DinoType.Small.copy(fontSize = DinoType.Body.fontSize.times(0.93f)), color = DinoColors.InkDim,
                )
            }
            state.templates.forEachIndexed { i, t ->
                if (i > 0) HorizontalDivider(color = DinoColors.Line)
                Row(
                    Modifier.fillMaxWidth().pressClickable(onClickLabel = "Démarrer ${t.name}") { onAction(WorkoutAction.StartTemplate(t.id)) }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(t.name, style = DinoType.BodyBold, color = DinoColors.Ink)
                        Text(t.summary, style = DinoType.Small, color = DinoColors.InkDim, maxLines = 2)
                    }
                    Text("Démarrer", style = DinoType.BodyBold.copy(fontSize = 14.sp), color = DinoColors.AmberInk)
                    IconButton(onClick = { templateToDelete = t }) {
                        Icon(NavIcons.Close, contentDescription = "Supprimer le modèle ${t.name}", tint = DinoColors.InkFaint, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        Panel(title = "Comment le rang est calculé", outlined = true) {
            Text(
                "Pour chaque série : poids × répétitions. Pour les exercices au poids du corps (tractions, dips, pompes…), ton poids s'ajoute au lest. " +
                    "Le total de toutes tes séances donne ton tonnage, et le tonnage débloque les dinosaures.",
                style = DinoType.Small.copy(fontSize = DinoType.Body.fontSize.times(0.93f)), color = DinoColors.InkDim,
            )
        }
        Panel(title = "Historique") {
            SessionRows(
                if (showAll) state.history else state.history.take(6),
                "Ton historique apparaîtra ici après ta première séance. Appuie sur une séance pour voir son détail.",
                onDelete = onDelete,
                onMakeTemplate = { name, ids -> onAction(WorkoutAction.SaveTemplate(name, ids)) },
            )
            if (!showAll && n > 6) {
                DashedButton("Voir les ${n - 6} séances précédentes") { showAll = true }
            }
        }
    }
    templateToDelete?.let { t ->
        TemplateDeleteDialog(t, onConfirm = { onAction(WorkoutAction.DeleteTemplate(t.id)); templateToDelete = null }, onDismiss = { templateToDelete = null })
    }
}

@Composable
private fun TemplateDeleteDialog(t: TemplateUi, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DinoColors.Bg,
        title = { Text("Supprimer « ${t.name} » ?") },
        text = { Text("Seul le modèle est supprimé, pas tes séances.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Supprimer", color = DinoColors.Lava) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Garder") } },
    )
}

@Composable
private fun ActiveContent(state: WorkoutUiState.Active, onAction: (WorkoutAction) -> Unit, onOpenPicker: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.sessionId) {
        while (true) { now = System.currentTimeMillis(); delay(1000) }
    }
    var confirmDiscard by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("Séance en cours, enregistrée automatiquement", style = DinoType.Small, color = DinoColors.InkDim)
                Text(Formats.chrono((now - state.startedAt) / 1000), style = DinoType.Display.copy(fontSize = DinoType.Display.fontSize.times(1.3f)), color = DinoColors.Ink)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(state.tonnage, style = DinoType.Title, color = DinoColors.AmberInk)
                Text("${state.validatedSets} séries validées", style = DinoType.Small, color = DinoColors.InkDim)
            }
        }
        state.entries.forEach { entry -> key(entry.order) { ExerciseCard(entry, onAction) } }
        PillButton("Ajouter un exercice", onClick = onOpenPicker, big = true, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PillButton("Abandonner", onClick = { confirmDiscard = true }, style = PillStyle.Danger)
            PillButton(
                "Terminer la séance",
                onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onAction(WorkoutAction.Finish) },
                style = PillStyle.Primary, enabled = state.entries.isNotEmpty(),
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Abandonner la séance ?") },
            text = { Text("Les séries notées dans cette séance seront supprimées.") },
            confirmButton = { TextButton(onClick = { onAction(WorkoutAction.Discard); confirmDiscard = false }) { Text("Abandonner", color = DinoColors.Lava) } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Continuer la séance") } },
            containerColor = DinoColors.Bg,
        )
    }
}

@Composable
private fun ExerciseCard(entry: ActiveEntry, onAction: (WorkoutAction) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .riseIn()
            .clip(RoundedCornerShape(22.dp))
            .background(DinoColors.Card)
            // Une série ajoutée agrandit la carte en douceur.
            .animateContentSize(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(entry.exercise.name, style = DinoType.Title.copy(fontSize = DinoType.Title.fontSize.times(0.95f)), color = DinoColors.Ink)
                Text(entry.hint, style = DinoType.Label, color = DinoColors.InkFaint, modifier = Modifier.padding(top = 3.dp))
            }
            IconButton(onClick = { onAction(WorkoutAction.RemoveExercise(entry.order)) }) {
                Icon(NavIcons.Close, contentDescription = "Retirer ${entry.exercise.name}", tint = DinoColors.InkFaint, modifier = Modifier.size(18.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Série", style = DinoType.Label, color = DinoColors.InkFaint, modifier = Modifier.width(44.dp))
            Text(if (entry.exercise.bodyweight) "Lest" else "Poids", style = DinoType.Label, color = DinoColors.InkFaint, modifier = Modifier.weight(1f))
            Text("Répétitions", style = DinoType.Label, color = DinoColors.InkFaint, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(44.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            entry.sets.forEachIndexed { i, set -> key(set.id) { SetRow(i + 1, set, onAction) } }
        }
        DashedButton("Ajouter une série") { onAction(WorkoutAction.AddSet(entry.order)) }
    }
}

@Composable
private fun SetRow(index: Int, set: WorkoutSet, onAction: (WorkoutAction) -> Unit) {
    // État local pour garder le curseur stable pendant la saisie ; la base est mise à jour à chaque frappe.
    var kg by remember(set.id) { mutableStateOf(if (set.weightKg > 0) Formats.weight(set.weightKg) else "") }
    var reps by remember(set.id) { mutableStateOf(if (set.reps > 0) set.reps.toString() else "") }
    var done by remember(set.id) { mutableStateOf(set.done) }
    fun push() = onAction(WorkoutAction.UpdateSet(set.id, Formats.parseDecimal(kg), reps.toIntOrNull() ?: 0, done))
    val haptics = LocalHapticFeedback.current
    val checkBg by animateColorAsState(if (done) DinoColors.Green else DinoColors.Bg, tween(Motion.MEDIUM), label = "checkBg")
    val checkTint by animateColorAsState(if (done) DinoColors.OnGreen else DinoColors.InkFaint, tween(Motion.MEDIUM), label = "checkTint")
    val pop = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$index", style = DinoType.Title.copy(fontSize = DinoType.Body.fontSize), color = DinoColors.InkDim, modifier = Modifier.width(44.dp).padding(start = 16.dp))
        NumberField(kg, { kg = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(6); push() }, "kg", KeyboardType.Decimal, Modifier.weight(1f))
        NumberField(reps, { reps = it.filter { c -> c.isDigit() }.take(3); push() }, "reps", KeyboardType.Number, Modifier.weight(1f))
        Box(
            Modifier
                .size(width = 44.dp, height = 42.dp)
                .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                .pressClickable(pressedScale = 0.9f, shape = RoundedCornerShape(10.dp), onClickLabel = if (done) "Annuler la validation" else "Valider la série") {
                    done = !done
                    push()
                    if (done) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scope.launch {
                            pop.snapTo(1f)
                            pop.animateTo(1.18f, tween(110))
                            pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium))
                        }
                    }
                }
                .background(checkBg)
                .border(1.dp, if (done) DinoColors.Green else DinoColors.Line, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(NavIcons.Check, contentDescription = "Valider la série $index", tint = checkTint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, unit: String, type: KeyboardType, modifier: Modifier) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = DinoType.BodyBold.copy(fontSize = DinoType.Body.fontSize.times(1.07f), color = DinoColors.Ink),
        cursorBrush = SolidColor(DinoColors.Amber),
        keyboardOptions = KeyboardOptions(keyboardType = type),
        modifier = modifier.onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(DinoColors.Bg)
                    .border(1.dp, if (focused) DinoColors.Amber else DinoColors.Line, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) { inner() }
                Text(unit, style = DinoType.Label, color = DinoColors.InkFaint)
            }
        },
    )
}

@Composable
private fun DashedButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .pressClickable(shape = RoundedCornerShape(10.dp), onClick = onClick)
            .border(BorderStroke(1.dp, DinoColors.Line), RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = DinoType.Small.copy(fontSize = DinoType.Body.fontSize.times(0.93f)), color = DinoColors.InkDim)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ExercisePickerSheet(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    onCreate: (String, MuscleGroup) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var group by remember { mutableStateOf<MuscleGroup?>(null) }
    val q = query.trim().lowercase()
    val filtered = exercises.filter { (group == null || it.group == group) && (q.isEmpty() || it.name.lowercase().contains(q)) }
    val canCreate = q.isNotEmpty() && exercises.none { it.name.lowercase() == q }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DinoColors.Bg,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
            Text("Ajouter un exercice", style = DinoType.Title.copy(fontSize = DinoType.Title.fontSize.times(1.1f)), color = DinoColors.Ink)
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Rechercher un exercice") }, singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DinoColors.Amber, unfocusedBorderColor = DinoColors.Line, cursorColor = DinoColors.Amber,
                    focusedContainerColor = DinoColors.Bg, unfocusedContainerColor = DinoColors.Bg,
                ),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MuscleGroup.entries.filter { it != MuscleGroup.CUSTOM }.forEach { g ->
                    FilterChip(
                        selected = group == g,
                        onClick = { group = if (group == g) null else g },
                        label = { Text(g.label, style = DinoType.Small) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DinoColors.Card, labelColor = DinoColors.InkDim,
                            selectedContainerColor = DinoColors.Ink, selectedLabelColor = DinoColors.Bg,
                        ),
                    )
                }
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp).padding(top = 6.dp, bottom = 16.dp)) {
                items(filtered, key = { it.id }) { ex ->
                    HorizontalDivider(color = DinoColors.Line)
                    PickRow(ex.name, ex.group.label) { onPick(ex.id) }
                }
                if (canCreate) {
                    item {
                        HorizontalDivider(color = DinoColors.Line)
                        PickRow("Créer « ${query.trim()} »", "Exercice perso") { onCreate(query.trim(), group ?: MuscleGroup.CUSTOM) }
                    }
                }
                if (filtered.isEmpty() && !canCreate) {
                    item { Text("Aucun exercice dans ce groupe.", style = DinoType.Small, color = DinoColors.InkDim, modifier = Modifier.padding(vertical = 12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun PickRow(title: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().pressClickable(onClick = onClick).padding(vertical = 13.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = DinoType.Body, color = DinoColors.Ink, modifier = Modifier.weight(1f))
        Text(sub, style = DinoType.Small, color = DinoColors.InkFaint)
    }
}

@Composable
private fun SavedDialog(e: WorkoutEvent.Saved, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DinoColors.Bg,
        title = { Text("Séance enregistrée", style = DinoType.Title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FlameChip(Streak(e.streak, alive = true, atRisk = false, best = e.streak), iconSize = 36.dp)
                Text(
                    if (e.streak <= 1) "Ta flamme est allumée !" else "Ta flamme passe à ${e.streak} séances d'affilée !",
                    style = DinoType.BodyBold, color = DinoColors.AmberInk,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Tonnage", e.tonnage, Modifier.weight(1f), DinoColors.AmberInk)
                StatTile("Calories", e.kcal, Modifier.weight(1f), DinoColors.Lava)
                StatTile("Durée", e.duration, Modifier.weight(1f))
            }
            }
        },
        confirmButton = { PillButton("Fermer", onClick = onDismiss, style = PillStyle.Primary) },
    )
}

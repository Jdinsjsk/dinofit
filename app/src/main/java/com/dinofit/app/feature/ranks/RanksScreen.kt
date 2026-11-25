package com.dinofit.app.feature.ranks

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dinofit.app.core.common.FitnessMath
import com.dinofit.app.core.common.Formats
import com.dinofit.app.core.designsystem.DinoColors
import com.dinofit.app.core.designsystem.DinoImage
import com.dinofit.app.core.designsystem.DinoShowcase
import com.dinofit.app.core.designsystem.DinoType
import com.dinofit.app.core.designsystem.PageHeader
import com.dinofit.app.core.designsystem.Panel
import com.dinofit.app.core.designsystem.PillButton
import com.dinofit.app.core.designsystem.PillStyle
import com.dinofit.app.core.designsystem.ProgressBar
import com.dinofit.app.core.designsystem.StatTile
import com.dinofit.app.core.designsystem.pressClickable
import com.dinofit.app.core.designsystem.riseIn
import com.dinofit.app.core.model.DinoRank
import com.dinofit.app.core.model.PersonalRecord
import com.dinofit.app.core.designsystem.ProgressChart
import com.dinofit.app.core.designsystem.progressSummary
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun RanksRoute(viewModel: RanksViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RanksScreen(uiState)
}

@Composable
internal fun RanksScreen(uiState: RanksUiState) {
    val state = uiState as? RanksUiState.Success ?: return
    val current = state.ranks[state.currentLevel - 1]
    val next = state.ranks.getOrNull(state.currentLevel)
    val progress = FitnessMath.progressToNext(state.tonnageKg)
    var opened by remember { mutableStateOf<DinoRank?>(null) }
    var chartFor by remember { mutableStateOf<PersonalRecord?>(null) }
    var showAllRecords by remember { mutableStateOf(false) }

    // Le tonnage défile de 0 jusqu'à la vraie valeur.
    var countTarget by remember { mutableStateOf(0f) }
    LaunchedEffect(state.tonnageKg) { countTarget = state.tonnageKg.toFloat() }
    val counted by animateFloatAsState(countTarget, tween(1300), label = "count")

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PageHeader("Ton parcours de fossile à T-Rex", "Les rangs", Modifier.padding(bottom = 8.dp))

        Panel {
            DinoShowcase(current)
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.Bottom) {
                Text(current.name, style = DinoType.Display, color = DinoColors.Ink, modifier = Modifier.weight(1f))
                Text(Formats.tonnage(counted.toDouble()), style = DinoType.Number, color = DinoColors.AmberInk)
            }
            Text(current.blurb, style = DinoType.Body, color = DinoColors.InkDim, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            ProgressBar(progress, track = DinoColors.Card2)
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(14.dp)).background(DinoColors.Bg).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (next != null) {
                    DinoImage(next, Modifier.size(56.dp), locked = true)
                    Column {
                        Text("Prochain : ${next.name}", style = DinoType.BodyBold, color = DinoColors.Ink)
                        Text(
                            "Encore ${Formats.tonnage(next.minKg - state.tonnageKg)}, tu en es à ${(progress * 100).toInt()} %",
                            style = DinoType.Small, color = DinoColors.InkDim,
                        )
                    }
                } else {
                    Text("Tu as débloqué tous les dinosaures.", style = DinoType.BodyBold, color = DinoColors.Ink)
                }
            }
        }

        state.ranks.reversed().forEachIndexed { index, rank ->
            Rung(rank, state.currentLevel, index, onOpen = { opened = rank })
        }

        Panel(title = "Tes records et ta progression", modifier = Modifier.padding(top = 6.dp)) {
            Text(
                "Maximum estimé sur une répétition (formule d'Epley). Touche un exercice pour voir ta courbe.",
                style = DinoType.Small, color = DinoColors.InkDim, modifier = Modifier.padding(bottom = 4.dp),
            )
            if (state.records.isEmpty()) {
                Text("Tes records s'afficheront après ta première séance.", style = DinoType.Body, color = DinoColors.InkDim)
            }
            val shownRecords = if (showAllRecords) state.records else state.records.take(8)
            shownRecords.forEachIndexed { i, r ->
                if (i > 0) HorizontalDivider(color = DinoColors.Line)
                Row(
                    Modifier.fillMaxWidth().pressClickable(onClickLabel = "Voir la progression") { chartFor = r }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(r.exercise.name, style = DinoType.BodyBold, color = DinoColors.Ink)
                        Text(
                            when {
                                r.exercise.bodyweight && r.weightKg == 0.0 -> "${r.reps} reps au poids du corps"
                                r.exercise.bodyweight -> "${r.reps} × ${Formats.weight(r.weightKg)} kg + poids du corps"
                                else -> "${r.reps} × ${Formats.weight(r.weightKg)} kg"
                            },
                            style = DinoType.Small, color = DinoColors.InkDim,
                        )
                    }
                    Text("${Formats.int(r.estimatedMaxKg.roundToInt())} kg", style = DinoType.Title.copy(fontSize = 15.sp), color = DinoColors.AmberInk)
                    Text(" ›", style = DinoType.Title, color = DinoColors.InkFaint)
                }
            }
            if (!showAllRecords && state.records.size > 8) {
                PillButton("Voir les ${state.records.size} exercices", onClick = { showAllRecords = true }, style = PillStyle.Ghost, modifier = Modifier.padding(top = 8.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Séances", Formats.int(state.sessions), Modifier.weight(1f))
            StatTile("Séries", Formats.int(state.sets), Modifier.weight(1f))
            StatTile("kcal salle", Formats.int(state.gymKcal), Modifier.weight(1f), DinoColors.Lava)
        }
    }

    chartFor?.let { r ->
        val pts = state.progressions[r.exercise.id].orEmpty()
        AlertDialog(
            onDismissRequest = { chartFor = null },
            containerColor = DinoColors.Bg,
            title = { Text(r.exercise.name, style = DinoType.Title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProgressChart(pts)
                    Text(progressSummary(pts), style = DinoType.Body, color = DinoColors.Ink)
                    Text("Chaque point = ta meilleure série de la séance, convertie en max sur une répétition.", style = DinoType.Small, color = DinoColors.InkDim)
                }
            },
            confirmButton = { PillButton("Fermer", onClick = { chartFor = null }, style = PillStyle.Primary) },
        )
    }

    opened?.let { rank ->
        AlertDialog(
            onDismissRequest = { opened = null },
            containerColor = DinoColors.Bg,
            text = {
                Column {
                    DinoShowcase(rank)
                    Text(rank.name, style = DinoType.Display, color = DinoColors.Ink, modifier = Modifier.padding(top = 14.dp))
                    Text(rank.blurb, style = DinoType.Body, color = DinoColors.InkDim, modifier = Modifier.padding(top = 4.dp))
                    Text("Débloqué à ${Formats.tonnage(rank.minKg)}.", style = DinoType.Small, color = DinoColors.InkFaint, modifier = Modifier.padding(top = 6.dp))
                }
            },
            confirmButton = { PillButton("Fermer", onClick = { opened = null }, style = PillStyle.Primary) },
        )
    }
}

/** Une marche de l'échelle. Apparaît en glissant, avec un léger décalage selon sa position. */
@Composable
private fun Rung(rank: DinoRank, currentLevel: Int, index: Int, onOpen: () -> Unit) {
    val locked = rank.level > currentLevel
    val current = rank.level == currentLevel
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .riseIn(delayMs = index * 40)
            .then(if (locked) Modifier else Modifier.pressClickable(onClickLabel = "Voir ${rank.name}", shape = shape, onClick = onOpen))
            .clip(shape)
            .then(
                when {
                    current -> Modifier.background(DinoColors.AmberSoft).border(2.dp, DinoColors.Amber, shape)
                    locked -> Modifier.border(1.dp, DinoColors.Line, shape)
                    else -> Modifier.background(DinoColors.Card)
                },
            )
            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(76.dp).clip(RoundedCornerShape(14.dp)).background(if (locked) Color.Transparent else Color.White).padding(4.dp)) {
            DinoImage(rank, Modifier.fillMaxSize(), locked = locked)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (locked) "???" else rank.name,
                    style = DinoType.Title.copy(fontSize = 16.sp),
                    color = if (locked) DinoColors.InkFaint else DinoColors.Ink,
                )
                if (current) {
                    Text(
                        "Toi", style = DinoType.Label, color = DinoColors.OnAmber,
                        modifier = Modifier.padding(start = 6.dp).clip(CircleShape).background(DinoColors.Amber).padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                if (locked) "Se débloque à ${Formats.tonnage(rank.minKg)}" else rank.blurb,
                style = DinoType.Small, color = DinoColors.InkDim,
            )
        }
        Text("${rank.level}", style = DinoType.Title.copy(fontSize = 14.sp), color = if (current) DinoColors.AmberInk else DinoColors.InkFaint)
    }
}

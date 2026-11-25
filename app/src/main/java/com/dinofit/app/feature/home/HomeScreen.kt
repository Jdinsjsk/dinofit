package com.dinofit.app.feature.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dinofit.app.core.common.Formats
import com.dinofit.app.core.designsystem.DinoColors
import com.dinofit.app.core.designsystem.FlameChip
import com.dinofit.app.core.designsystem.NavIcons
import com.dinofit.app.core.designsystem.ProgressBar
import com.dinofit.app.core.designsystem.pressClickable
import com.dinofit.app.core.designsystem.streakMessage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.dinofit.app.core.designsystem.DinoType
import com.dinofit.app.core.designsystem.Panel
import com.dinofit.app.core.designsystem.PageHeader
import com.dinofit.app.core.designsystem.PillButton
import com.dinofit.app.core.designsystem.PillStyle
import com.dinofit.app.core.designsystem.RankHero
import com.dinofit.app.core.designsystem.StatTile
import com.dinofit.app.core.designsystem.StepsRing
import com.dinofit.app.core.designsystem.WeekChart
import com.dinofit.app.core.ui.SessionRows
import java.time.LocalDate

@Composable
fun HomeRoute(
    onOpenWorkout: () -> Unit,
    onOpenRanks: () -> Unit,
    onOpenWalk: () -> Unit,
    onOpenMeals: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState, onAction = viewModel::onAction, onOpenWorkout = onOpenWorkout, onOpenRanks = onOpenRanks,
        onOpenWalk = onOpenWalk, onOpenMeals = onOpenMeals, onOpenProfile = onOpenProfile,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onAction: (HomeAction) -> Unit,
    onOpenWorkout: () -> Unit,
    onOpenRanks: () -> Unit,
    onOpenWalk: () -> Unit,
    onOpenMeals: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val state = uiState as? HomeUiState.Success ?: return
    val context = LocalContext.current
    var showAddSteps by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onAction(HomeAction.StartCounting)
    }
    fun hasPermission() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    // Les pas se comptent tout seuls : la permission est demandée dès la première ouverture.
    var asked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        when {
            hasPermission() -> onAction(HomeAction.StartCounting)
            !asked && state.sensorAvailable -> {
                asked = true
                permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PageHeader(
                kicker = Formats.longDate(LocalDate.now()),
                title = if (state.profile.name.isBlank()) "Salut" else "Salut ${state.profile.name}",
                modifier = Modifier.weight(1f).padding(bottom = 0.dp),
            )
            FlameChip(state.streak, Modifier.pressClickable(onClickLabel = "Voir mes séances", onClick = onOpenWorkout))
            IconButton(onClick = onOpenProfile) {
                Icon(NavIcons.Profile, contentDescription = "Profil", tint = DinoColors.InkDim)
            }
        }
        streakMessage(state.streak)?.let {
            Text(it, style = DinoType.Small, color = if (state.streak.atRisk) DinoColors.Lava else DinoColors.InkDim, modifier = Modifier.offset(y = (-8).dp))
        }
        RankHero(state.tonnageKg, state.rank, state.nextRank, state.progress, onClick = onOpenRanks)

        Panel(title = "Dernières séances") {
            SessionRows(
                state.recentSessions,
                "Aucune séance pour l'instant. Tes 5 premières tonnes réveillent ton fossile.",
                onDelete = { onAction(HomeAction.DeleteSession(it)) },
            )
            Spacer(Modifier.height(12.dp))
            PillButton(
                text = if (state.hasActiveSession) "Reprendre la séance" else "Commencer une séance",
                onClick = onOpenWorkout, style = PillStyle.Primary,
            )
        }

        Panel(title = "Repas du jour") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("${Formats.int(state.eatenKcal)} kcal", style = DinoType.Number, color = if (state.eatenKcal > state.kcalGoal) DinoColors.Lava else DinoColors.Ink)
                Text("sur ${Formats.int(state.kcalGoal)}", style = DinoType.Small, color = DinoColors.InkDim)
            }
            Spacer(Modifier.height(8.dp))
            ProgressBar(
                if (state.kcalGoal > 0) state.eatenKcal.toFloat() / state.kcalGoal else 0f,
                color = if (state.eatenKcal > state.kcalGoal) DinoColors.Lava else DinoColors.Green,
                track = DinoColors.Card2,
            )
            Spacer(Modifier.height(12.dp))
            PillButton("Ajouter un repas", onClick = onOpenMeals)
        }

        Panel(title = "Pas du jour") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StepsRing(state.todaySteps, state.profile.stepGoal)
                Column(Modifier.weight(1f)) {
                    val left = state.profile.stepGoal - state.todaySteps
                    Text(
                        (if (left <= 0) "Objectif atteint. Chaque pas en plus compte quand même."
                        else "Plus que ${Formats.int(left)} pas pour ton objectif.") +
                            (if (state.counting) " Comptés automatiquement par ton téléphone." else ""),
                        style = DinoType.Small.copy(fontSize = DinoType.Body.fontSize.times(0.93f)), color = DinoColors.InkDim,
                    )
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.sensorAvailable && !state.counting) {
                            PillButton(
                                text = "Activer le podomètre",
                                style = PillStyle.Primary,
                                onClick = {
                                    if (hasPermission()) onAction(HomeAction.StartCounting)
                                    else permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                                },
                            )
                        }
                        PillButton("Démarrer une marche", onClick = onOpenWalk, style = if (state.counting) PillStyle.Primary else PillStyle.Secondary)
                        PillButton("Ajouter des pas", onClick = { showAddSteps = true })
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Distance", "${Formats.oneDecimal(state.distanceKm)} km", Modifier.weight(1f), DinoColors.Green)
            StatTile("Calories", Formats.int(state.kcalToday), Modifier.weight(1f), DinoColors.Lava)
            StatTile("Min. actives", Formats.int(state.activeMinutes), Modifier.weight(1f))
        }

        Panel(title = "Ta semaine") {
            WeekChart(state.week, state.profile.stepGoal)
            Text(state.weekSummary, style = DinoType.Label, color = DinoColors.InkFaint, modifier = Modifier.padding(top = 10.dp))
        }
    }

    if (showAddSteps) {
        AddStepsSheet(
            onDismiss = { showAddSteps = false },
            onAdd = { onAction(HomeAction.AddSteps(it)); showAddSteps = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddStepsSheet(onDismiss: () -> Unit, onAdd: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DinoColors.Bg,
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 28.dp)) {
            Text("Ajouter des pas", style = DinoType.Title.copy(fontSize = DinoType.Title.fontSize.times(1.1f)), color = DinoColors.Ink)
            Text("Depuis ta montre, ou une marche sans ton téléphone.", style = DinoType.Small, color = DinoColors.InkDim, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { v -> text = v.filter { it.isDigit() }.take(6) },
                label = { Text("Nombre de pas") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { text.toIntOrNull()?.let(onAdd) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DinoColors.Amber, unfocusedBorderColor = DinoColors.Line,
                    focusedLabelColor = DinoColors.Amber, cursorColor = DinoColors.Amber,
                    focusedContainerColor = DinoColors.Bg, unfocusedContainerColor = DinoColors.Bg,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1000, 2500, 5000).forEach { n ->
                    PillButton("+${Formats.int(n)}", onClick = { onAdd(n) }, style = PillStyle.Ghost)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("Ajouter", onClick = { text.toIntOrNull()?.let(onAdd) }, style = PillStyle.Primary, enabled = (text.toIntOrNull() ?: 0) > 0)
                PillButton("Annuler", onClick = onDismiss, style = PillStyle.Ghost)
            }
        }
    }
}

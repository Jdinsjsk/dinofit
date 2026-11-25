package com.dinofit.app.feature.walk

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dinofit.app.core.common.Formats
import com.dinofit.app.core.designsystem.DinoColors
import com.dinofit.app.core.designsystem.DinoType
import com.dinofit.app.core.designsystem.Motion
import com.dinofit.app.core.designsystem.PageHeader
import com.dinofit.app.core.designsystem.Panel
import com.dinofit.app.core.designsystem.PillButton
import com.dinofit.app.core.designsystem.PillStyle
import com.dinofit.app.core.designsystem.StatTile
import com.dinofit.app.core.designsystem.StepsRing
import com.dinofit.app.core.designsystem.pressClickable
import kotlinx.coroutines.delay

@Composable
fun WalkRoute(viewModel: WalkViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }

    // Position (obligatoire), notifications (pour voir la marche en cours) et activité physique (pas pendant la marche).
    val permissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Manifest.permission.ACTIVITY_RECOGNITION)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val located = result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        permissionDenied = !located
        if (located) viewModel.start()
    }
    fun hasLocation() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // Si une marche était en cours (app fermée entre-temps), l'enregistrement reprend.
    val isActive = uiState is WalkUiState.Active
    LaunchedEffect(isActive) { if (isActive && hasLocation()) viewModel.resumeIfNeeded() }

    WalkScreen(
        uiState = uiState,
        permissionDenied = permissionDenied,
        onStart = { if (hasLocation()) viewModel.start() else launcher.launch(permissions) },
        onStop = viewModel::stop,
        onDelete = viewModel::delete,
        pointsFor = viewModel::points,
    )
}

@Composable
internal fun WalkScreen(
    uiState: WalkUiState,
    permissionDenied: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDelete: (Long) -> Unit,
    pointsFor: (Long) -> kotlinx.coroutines.flow.Flow<List<com.dinofit.app.core.model.LatLng>>,
) {
    AnimatedContent(
        targetState = uiState,
        contentKey = { it::class },
        transitionSpec = { fadeIn(tween(Motion.MEDIUM, delayMillis = 60)) togetherWith fadeOut(tween(Motion.EXIT)) },
        label = "walk",
    ) { state ->
        when (state) {
            WalkUiState.Loading -> Unit
            is WalkUiState.Idle -> IdleWalk(state, permissionDenied, onStart, onDelete, pointsFor)
            is WalkUiState.Active -> ActiveWalk(state, onStop)
        }
    }
}

@Composable
private fun IdleWalk(
    state: WalkUiState.Idle,
    permissionDenied: Boolean,
    onStart: () -> Unit,
    onDelete: (Long) -> Unit,
    pointsFor: (Long) -> kotlinx.coroutines.flow.Flow<List<com.dinofit.app.core.model.LatLng>>,
) {
    var opened by remember { mutableStateOf<WalkSummary?>(null) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageHeader("Tes pas sont comptés automatiquement", "Marche", Modifier.padding(bottom = 0.dp))

        Panel(title = "Aujourd'hui") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StepsRing(state.todaySteps, state.stepGoal)
                Text(
                    "Ton téléphone compte tes pas toute la journée, même quand l'app est fermée. Rien à lancer.",
                    style = DinoType.Body, color = DinoColors.InkDim, modifier = Modifier.weight(1f),
                )
            }
        }

        Panel(title = "Enregistrer un trajet") {
            Text(
                "Lance une marche pour tracer ton parcours sur la carte, avec la distance, l'allure et les pas. " +
                    "L'enregistrement continue écran éteint.",
                style = DinoType.Body, color = DinoColors.InkDim, modifier = Modifier.padding(bottom = 14.dp),
            )
            PillButton("Démarrer une marche", onClick = onStart, style = PillStyle.Primary, big = true, modifier = Modifier.fillMaxWidth())
            if (permissionDenied) {
                Text(
                    "DinoFit a besoin de ta position pour tracer le trajet. Autorise-la dans les réglages du téléphone, rubrique Applications > DinoFit > Autorisations.",
                    style = DinoType.Small, color = DinoColors.Lava, modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        Panel(title = "Tes trajets") {
            if (state.walks.isEmpty()) {
                Text("Tes marches enregistrées apparaîtront ici, avec leur carte.", style = DinoType.Body, color = DinoColors.InkDim)
            } else {
                Text("${state.walks.size} marche${if (state.walks.size > 1) "s" else ""}, ${state.totalDistance} au total", style = DinoType.Small, color = DinoColors.InkDim, modifier = Modifier.padding(bottom = 4.dp))
            }
            state.walks.forEachIndexed { i, w ->
                if (i > 0) HorizontalDivider(color = DinoColors.Line)
                Row(
                    Modifier.fillMaxWidth().pressClickable(onClickLabel = "Voir le trajet") { opened = w }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(w.date, style = DinoType.BodyBold, color = DinoColors.Ink)
                        Text("${w.duration}, ${w.steps} pas, ${w.pace}", style = DinoType.Small, color = DinoColors.InkDim)
                    }
                    Text(w.distance, style = DinoType.Title.copy(fontSize = 15.sp), color = DinoColors.Green)
                    Text("›", style = DinoType.Title, color = DinoColors.InkFaint)
                }
            }
        }
    }

    opened?.let { w ->
        WalkDetailDialog(w, pointsFor(w.id), onDismiss = { opened = null }, onDelete = { onDelete(w.id); opened = null })
    }
}

@Composable
private fun ActiveWalk(state: WalkUiState.Active, onStop: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.walk.id) {
        while (true) { now = System.currentTimeMillis(); delay(1000) }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Marche en cours", style = DinoType.Small, color = DinoColors.InkDim)
        Text(Formats.chrono((now - state.walk.startedAt) / 1000), style = DinoType.Display.copy(fontSize = 40.sp, lineHeight = 42.sp), color = DinoColors.Ink)
        RouteMap(state.points, follow = true, modifier = Modifier.fillMaxWidth().height(320.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Distance", Formats.distance(state.walk.distanceM), Modifier.weight(1f), DinoColors.Green)
            StatTile("Pas", Formats.int(state.walk.steps), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Allure", state.pace, Modifier.weight(1f))
            StatTile("Calories", state.kcal, Modifier.weight(1f), DinoColors.Lava)
        }
        PillButton(
            "Terminer la marche",
            onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onStop() },
            style = PillStyle.Primary, big = true, modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Tu peux éteindre l'écran : le trajet continue. Tu peux aussi terminer depuis la notification.",
            style = DinoType.Small, color = DinoColors.InkFaint,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun WalkDetailDialog(
    w: WalkSummary,
    points: kotlinx.coroutines.flow.Flow<List<com.dinofit.app.core.model.LatLng>>,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    val pts by remember(w.id) { points }.collectAsStateWithLifecycle(initialValue = emptyList())
    var confirm by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(DinoColors.Bg)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(w.date, style = DinoType.Title.copy(fontSize = 20.sp), color = DinoColors.Ink)
            if (pts.isNotEmpty()) {
                RouteMap(pts, follow = false, modifier = Modifier.fillMaxWidth().height(300.dp))
            } else {
                Text("Chargement du trajet…", style = DinoType.Small, color = DinoColors.InkDim)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Distance", w.distance, Modifier.weight(1f), DinoColors.Green)
                StatTile("Durée", w.duration, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Pas", w.steps, Modifier.weight(1f))
                StatTile("Allure", w.pace, Modifier.weight(1f))
                StatTile("kcal", w.kcal, Modifier.weight(1f), DinoColors.Lava)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { if (confirm) onDelete() else confirm = true }) {
                    Text(if (confirm) "Confirmer la suppression" else "Supprimer", color = DinoColors.Lava)
                }
                PillButton("Fermer", onClick = onDismiss, style = PillStyle.Primary)
            }
        }
    }
}

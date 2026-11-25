package com.dinofit.app.feature.workout

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dinofit.app.core.designsystem.DinoColors
import com.dinofit.app.core.designsystem.DinoShowcase
import com.dinofit.app.core.designsystem.DinoType
import com.dinofit.app.core.designsystem.PillButton
import com.dinofit.app.core.designsystem.PillStyle
import com.dinofit.app.core.model.DinoRank

/** Nouveau rang : le dinosaure surgit au milieu des rayons, puis le nom apparaît. */
@Composable
fun RankUpDialog(rank: DinoRank, onDismiss: () -> Unit) {
    var started by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(rank.id) {
        started = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    val kickerAlpha by animateFloatAsState(if (started) 1f else 0f, tween(500, delayMillis = 200), label = "kicker")
    val titleAlpha by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 1000), label = "title")
    val buttonAlpha by animateFloatAsState(if (started) 1f else 0f, tween(500, delayMillis = 1500), label = "button")

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(DinoColors.Bg), contentAlignment = Alignment.Center) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Nouveau rang débloqué", style = DinoType.BodyBold, color = DinoColors.AmberInk, modifier = Modifier.alpha(kickerAlpha).padding(bottom = 12.dp))
                Box(Modifier.widthIn(max = 440.dp).fillMaxWidth()) { DinoShowcase(rank) }
                Text(
                    rank.name, style = DinoType.Display.copy(fontSize = 42.sp, lineHeight = 44.sp), color = DinoColors.Ink,
                    textAlign = TextAlign.Center, modifier = Modifier.alpha(titleAlpha).padding(top = 18.dp),
                )
                Text(
                    rank.blurb, style = DinoType.Body, color = DinoColors.InkDim, textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 340.dp).padding(top = 6.dp, bottom = 22.dp).alpha(titleAlpha),
                )
                PillButton("Continuer", onClick = onDismiss, style = PillStyle.Primary, modifier = Modifier.alpha(buttonAlpha))
            }
        }
    }
}

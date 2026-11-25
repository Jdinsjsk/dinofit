package com.dinofit.app.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dinofit.app.R
import com.dinofit.app.core.common.Formats
import com.dinofit.app.core.model.Streak

/**
 * La flamme façon Snap, avec le crâne de dino : nombre de séances d'affilée.
 * Éteinte = crâne gris ; dernier jour pour la garder = elle pulse.
 */
@Composable
fun FlameChip(streak: Streak, modifier: Modifier = Modifier, iconSize: Dp = 28.dp) {
    val bg = when {
        !streak.alive -> DinoColors.Card
        streak.atRisk -> Color(0xFFFFE6DF)
        else -> DinoColors.AmberSoft
    }
    val pulse = if (streak.atRisk) {
        val t = rememberInfiniteTransition(label = "flame")
        t.animateFloat(1f, 1.12f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse").value
    } else {
        1f
    }
    val grey = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    Row(
        modifier
            .clip(CircleShape)
            .background(bg)
            .padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
            .semantics {
                contentDescription = if (streak.alive) "Flamme : ${streak.count} séances d'affilée" else "Flamme éteinte"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.flame_skull),
            contentDescription = null,
            colorFilter = if (streak.alive) null else grey,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer { scaleX = pulse; scaleY = pulse; alpha = if (streak.alive) 1f else 0.5f },
        )
        Text(
            Formats.int(streak.count),
            style = DinoType.Number.copy(fontSize = (iconSize.value * 0.7f).sp),
            color = when {
                !streak.alive -> DinoColors.InkFaint
                streak.atRisk -> DinoColors.Lava
                else -> DinoColors.AmberInk
            },
        )
    }
}

/** Phrase sous la flamme, ou null si rien à dire. */
fun streakMessage(streak: Streak): String? = when {
    streak.atRisk -> "Dernier jour pour garder ta flamme : fais une séance aujourd'hui."
    !streak.alive && streak.best > 0 -> "Ta flamme s'est éteinte. Une séance la rallume (record : ${streak.best})."
    !streak.alive -> "Enregistre une séance pour allumer ta flamme."
    else -> null
}

/**
 * Jauge de calories : se remplit avec ce que tu manges, passe en rouge au-delà de l'objectif.
 * Animée depuis la dernière valeur affichée.
 */
@Composable
fun KcalGauge(eaten: Double, goal: Int, modifier: Modifier = Modifier, diameter: Dp = 190.dp) {
    val target = if (goal > 0) (eaten / goal).toFloat() else 0f
    val anim = remember { Animatable(0f) }
    LaunchedEffect(target) { anim.animateTo(target, tween(900, easing = Motion.EaseOut)) }
    val over = eaten > goal
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            val inset = stroke / 2
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(DinoColors.Card2, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
            val p = anim.value
            if (p > 0f) {
                drawArc(DinoColors.Green, -90f, 360f * p.coerceAtMost(1f), false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            if (p > 1f) {
                drawArc(DinoColors.Lava, -90f, 360f * (p - 1f).coerceAtMost(1f), false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(Formats.int((anim.value * goal).toInt().coerceAtLeast(0)), style = DinoType.Number.copy(fontSize = 34.sp), color = if (over) DinoColors.Lava else DinoColors.Ink)
            Text("sur ${Formats.int(goal)} kcal", style = DinoType.Small, color = DinoColors.InkDim)
        }
    }
}

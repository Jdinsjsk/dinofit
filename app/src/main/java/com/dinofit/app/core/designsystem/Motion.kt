package com.dinofit.app.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Rythme commun à toute l'app (mêmes valeurs que la version PC). */
object Motion {
    const val FAST = 120
    const val MEDIUM = 240
    const val SLOW = 420
    /** Les sorties sont plus rapides que les entrées (~60 %). */
    const val EXIT = 150
    val EaseOut = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
    val EaseIn = CubicBezierEasing(0.4f, 0f, 1f, 1f)
    fun <T> press() = spring<T>(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)
}

/** Réduit légèrement l'élément tant qu'il est appuyé (retour immédiat au doigt). */
fun Modifier.pressScale(source: MutableInteractionSource, pressedScale: Float = 0.96f): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, Motion.press(), label = "press")
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * clickable + effet d'appui + ondulation. À placer AVANT le fond/la bordure dans la chaîne,
 * pour que toute la carte se réduise ; [shape] limite l'ondulation aux coins arrondis.
 */
fun Modifier.pressClickable(
    pressedScale: Float = 0.98f,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    shape: Shape? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    this
        .pressScale(source, pressedScale)
        .then(if (shape != null) Modifier.clip(shape) else Modifier)
        .clickable(
            interactionSource = source,
            indication = LocalIndication.current,
            enabled = enabled,
            onClickLabel = onClickLabel,
            onClick = onClick,
        )
}

/** Apparition en glissant vers le haut, une seule fois, avec un décalage optionnel (listes en cascade). */
fun Modifier.riseIn(delayMs: Int = 0): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (delayMs > 0) delay(delayMs.toLong())
        progress.animateTo(1f, tween(Motion.SLOW, easing = Motion.EaseOut))
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 14.dp.toPx()
    }
}

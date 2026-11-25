package com.dinofit.app.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.dinofit.app.core.model.DinoRank
import com.dinofit.app.core.model.DinoRanks
import kotlin.math.PI
import kotlin.math.sin

/** (x, y en fraction de la carte, taille en dp, décalage de phase, doré ?) */
private val Sparks = listOf(
    Spark(0.08f, 0.18f, 14f, 0.00f, true), Spark(0.84f, 0.14f, 12f, 0.23f, false),
    Spark(0.16f, 0.70f, 10f, 0.42f, false), Spark(0.88f, 0.62f, 16f, 0.61f, true),
    Spark(0.50f, 0.06f, 10f, 0.77f, false), Spark(0.30f, 0.40f, 8f, 0.12f, false),
    Spark(0.70f, 0.34f, 9f, 0.50f, true), Spark(0.62f, 0.80f, 12f, 0.35f, false),
    Spark(0.40f, 0.86f, 9f, 0.69f, false), Spark(0.92f, 0.40f, 8f, 0.15f, false),
)

private data class Spark(val x: Float, val y: Float, val sizeDp: Float, val phase: Float, val gold: Boolean)

/**
 * Le dinosaure mis en scène : rayons qui tournent, halo qui pulse, étincelles,
 * et le dino qui surgit puis flotte doucement.
 */
@Composable
fun DinoShowcase(rank: DinoRank, modifier: Modifier = Modifier, showLevel: Boolean = true) {
    val loop = rememberInfiniteTransition(label = "showcase")
    val rotation by loop.animateFloat(0f, 360f, infiniteRepeatable(tween(40_000, easing = LinearEasing)), label = "rays")
    val pulse by loop.animateFloat(1f, 1.12f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "glow")
    val bob by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(1700), RepeatMode.Reverse), label = "bob")
    val twinkle by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "twinkle")

    val pop = remember(rank.id) { Animatable(0f) }
    LaunchedEffect(rank.id) { pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 140f)) }

    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1.25f)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.radialGradient(listOf(DinoColors.Glow1, DinoColors.Glow2, DinoColors.Glow3))),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.maxDimension
            val rays = Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.35f), Color.Transparent),
                center = center, radius = r * 0.62f,
            )
            rotate(rotation) {
                for (i in 0 until 16) {
                    drawArc(rays, i * 22.5f, 9f, true, Offset(center.x - r, center.y - r), Size(r * 2, r * 2))
                }
            }
            drawCircle(
                Brush.radialGradient(listOf(Color.White.copy(alpha = 0.95f), Color.Transparent), center = center, radius = size.width * 0.33f * pulse),
                radius = size.width * 0.33f * pulse,
            )
            Sparks.forEach { s ->
                val t = (twinkle + s.phase) % 1f
                val a = sin(PI * t).toFloat()
                if (a > 0.02f) {
                    val half = s.sizeDp.dp.toPx() * (0.3f + 0.7f * a) / 2f
                    val c = Offset(size.width * s.x, size.height * s.y)
                    val star = Path().apply {
                        moveTo(c.x, c.y - half)
                        lineTo(c.x + half * 0.22f, c.y - half * 0.22f)
                        lineTo(c.x + half, c.y)
                        lineTo(c.x + half * 0.22f, c.y + half * 0.22f)
                        lineTo(c.x, c.y + half)
                        lineTo(c.x - half * 0.22f, c.y + half * 0.22f)
                        lineTo(c.x - half, c.y)
                        lineTo(c.x - half * 0.22f, c.y - half * 0.22f)
                        close()
                    }
                    rotate(45f * a, c) { drawPath(star, (if (s.gold) DinoColors.Amber else Color.White).copy(alpha = a)) }
                }
            }
        }
        DinoImage(
            rank,
            Modifier
                .fillMaxWidth(0.62f)
                .graphicsLayer {
                    val p = pop.value
                    alpha = p.coerceIn(0f, 1f)
                    scaleX = 0.4f + 0.6f * p
                    scaleY = 0.4f + 0.6f * p
                    translationY = (1f - p) * 30.dp.toPx() - bob * 8.dp.toPx()
                },
            contentDescription = rank.name,
        )
        if (showLevel) {
            Text(
                "Niveau ${rank.level} sur ${DinoRanks.size}",
                style = DinoType.Label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                color = DinoColors.Ink,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    }
}

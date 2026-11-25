package com.dinofit.app.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dinofit.app.core.common.Formats
import com.dinofit.app.core.model.DaySteps
import com.dinofit.app.core.model.DinoRank
import com.dinofit.app.core.model.DinoRanks
import java.time.LocalDate
import kotlin.math.max

// ---------- Icônes au trait (navigation) ----------

fun strokeIcon(vararg paths: String): ImageVector {
    val b = ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
    for (p in paths) {
        b.addPath(
            pathData = addPathNodes(p),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    return b.build()
}

object NavIcons {
    val Today = strokeIcon(
        "M8 3c2 0 3 2 3 5s-1 6-3 6-3-3-3-6 1-5 3-5z", "M6 17h4v1.5a2 2 0 0 1-4 0z",
        "M16 6c2 0 3 2 3 5s-1 6-3 6-3-3-3-6 1-5 3-5z",
    )
    val Workout = strokeIcon("M3 9v6M6 6v12M18 6v12M21 9v6M6 12h12")
    val Walk = strokeIcon("M9 20l-5-2V4l5 2 6-2 5 2v14l-5-2-6 2z", "M9 6v14", "M15 4v14")
    val Ranks = strokeIcon("M3 15c3 0 5-1 7-1h4l2-5c1-2 3-3 5-2l-1 2-2 1-1 5-2 2v3h-2v-3h-4v3H7v-3c-2 0-3-1-4-2z")
    val Profile = strokeIcon("M16 8a4 4 0 1 1-8 0a4 4 0 1 1 8 0z", "M4 21c1-4 4-6 8-6s7 2 8 6")
    val Close = strokeIcon("M6 6l12 12M18 6L6 18")
    val Check = strokeIcon("M5 12l5 5 9-10")
}

// ---------- Mise en page ----------

@Composable
fun PageHeader(kicker: String, title: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(bottom = 18.dp)) {
        Text(kicker, style = DinoType.Small.copy(fontSize = 14.sp), color = DinoColors.InkDim)
        Text(title, style = DinoType.Display, color = DinoColors.Ink)
    }
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    title: String? = null,
    outlined: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (outlined) Modifier.border(1.dp, DinoColors.Line, shape) else Modifier.background(DinoColors.Card))
            .padding(18.dp),
    ) {
        if (title != null) {
            Text(title, style = DinoType.Title, color = DinoColors.Ink, modifier = Modifier.padding(bottom = 12.dp))
        }
        content()
    }
}

enum class PillStyle { Primary, Secondary, Ghost, Danger, Live }

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PillStyle = PillStyle.Secondary,
    enabled: Boolean = true,
    big: Boolean = false,
) {
    val shape = if (big) RoundedCornerShape(16.dp) else CircleShape
    val padding = if (big) PaddingValues(horizontal = 18.dp, vertical = 15.dp) else PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    val textStyle = DinoType.BodyBold.copy(fontSize = if (big) 16.sp else 14.sp)
    val source = remember { MutableInteractionSource() }
    val pressModifier = modifier.pressScale(source)
    when (style) {
        PillStyle.Ghost, PillStyle.Danger -> OutlinedButton(
            onClick = onClick, enabled = enabled, shape = shape, contentPadding = padding, modifier = pressModifier,
            interactionSource = source,
            border = BorderStroke(1.dp, if (style == PillStyle.Danger) Color(0xFFF0C4BA) else DinoColors.Line),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = DinoColors.Bg,
                contentColor = if (style == PillStyle.Danger) DinoColors.Lava else DinoColors.Ink,
            ),
        ) { Text(text, style = textStyle) }
        else -> {
            val (bg, fg) = when (style) {
                PillStyle.Primary -> DinoColors.Amber to DinoColors.OnAmber
                PillStyle.Live -> DinoColors.Green to DinoColors.OnGreen
                else -> DinoColors.Card2 to DinoColors.Ink
            }
            Button(
                onClick = onClick, enabled = enabled, shape = shape, contentPadding = padding, modifier = pressModifier,
                interactionSource = source,
                colors = ButtonDefaults.buttonColors(
                    containerColor = bg, contentColor = fg,
                    disabledContainerColor = bg.copy(alpha = 0.45f), disabledContentColor = fg.copy(alpha = 0.6f),
                ),
            ) { Text(text, style = textStyle) }
        }
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = DinoColors.Ink,
    background: Color = DinoColors.Card,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 14.dp),
    ) {
        Text(label, style = DinoType.Small, color = DinoColors.InkDim, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Text(value, style = DinoType.Number, color = valueColor, maxLines = 1)
    }
}

/** Barre de progression ; [animate] la fait se remplir depuis zéro à l'affichage. */
@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = DinoColors.Amber,
    track: Color = Color.White.copy(alpha = 0.6f),
    animate: Boolean = true,
) {
    var target by remember { mutableStateOf(if (animate) 0f else progress) }
    LaunchedEffect(progress) { target = progress }
    val shown by animateFloatAsState(target.coerceIn(0f, 1f), tween(1200, easing = Motion.EaseOut), label = "progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(track),
    ) {
        // Animé avec scaleX (pas de recalcul de mise en page à chaque image).
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = shown
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
                .clip(RoundedCornerShape(6.dp))
                .background(color),
        )
    }
}

// ---------- Carte de rang de l'accueil : le site de fouilles ----------

@Composable
fun RankHero(
    tonnageKg: Double,
    rank: DinoRank,
    next: DinoRank?,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(modifier.fillMaxWidth().pressClickable(onClickLabel = "Voir mes rangs", shape = shape, onClick = onClick)) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(DinoColors.Sky1, DinoColors.Sky2)))
                .padding(start = 18.dp, end = 18.dp, top = 18.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Ton rang : niveau ${rank.level} sur ${DinoRanks.size}", style = DinoType.Small, color = Color(0xFF7A6446))
                Text("${Formats.tonnage(tonnageKg)} soulevées", style = DinoType.Small, color = Color(0xFF7A6446))
            }
            Text(rank.name, style = DinoType.RankName, color = DinoColors.Ink, modifier = Modifier.padding(top = 6.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                DinoImage(rank, Modifier.fillMaxWidth(0.64f).offset(y = 14.dp), contentDescription = rank.name)
            }
        }
        Strata(Modifier.fillMaxWidth().height(46.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(DinoColors.Strata3)
                .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 18.dp),
        ) {
            ProgressBar(progress)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                if (next != null) {
                    Text("Encore ${Formats.tonnage(next.minKg - tonnageKg)} pour ${next.name}", style = DinoType.Small, color = DinoColors.SoilInk)
                    Text("${(progress * 100).toInt()} %", style = DinoType.Small, color = DinoColors.SoilInk)
                } else {
                    Text("Rang maximum atteint. Respect.", style = DinoType.Small, color = DinoColors.SoilInk)
                }
            }
        }
    }
}

@Composable
fun Strata(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val bands = listOf(DinoColors.Strata1, DinoColors.Strata2, DinoColors.Strata3)
        val h = size.height
        val bh = h / 4f
        var y = 0f
        var i = 0
        while (y < h) {
            drawRect(bands[i % 3], topLeft = Offset(0f, y), size = Size(size.width, bh))
            y += bh; i++
        }
        drawRect(DinoColors.StrataTop, size = Size(size.width, 3.dp.toPx()))
        val pebble = Color(0xFFB8955E)
        drawCircle(pebble, 2.dp.toPx(), Offset(size.width * 0.18f, h * 0.6f))
        drawCircle(pebble, 1.5.dp.toPx(), Offset(size.width * 0.64f, h * 0.3f))
        drawCircle(pebble, 2.dp.toPx(), Offset(size.width * 0.86f, h * 0.75f))
    }
}

// ---------- Pas ----------

@Composable
fun StepsRing(steps: Int, goal: Int, modifier: Modifier = Modifier, diameter: Dp = 132.dp) {
    val target = if (goal > 0) (steps.toFloat() / goal).coerceIn(0f, 1f) else 0f
    // L'anneau et le chiffre partent de la dernière valeur affichée (0 au premier affichage).
    val sweep = remember { Animatable(0f) }
    val count = remember { Animatable(0f) }
    LaunchedEffect(target) { sweep.animateTo(target, tween(900, easing = Motion.EaseOut)) }
    LaunchedEffect(steps) { count.animateTo(steps.toFloat(), tween(900, easing = Motion.EaseOut)) }
    val pct = sweep.value
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(DinoColors.Card2, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            if (pct > 0f) {
                drawArc(DinoColors.Green, -90f, 360f * pct, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(Formats.int(count.value.roundToInt()), style = DinoType.Number.copy(fontSize = 24.sp), color = DinoColors.Ink)
            Text("sur ${Formats.int(goal)}", style = DinoType.Label, color = DinoColors.InkDim)
        }
    }
}

@Composable
fun WeekChart(days: List<DaySteps>, goal: Int, modifier: Modifier = Modifier) {
    val maxV = max(goal, days.maxOfOrNull { it.steps } ?: 0).coerceAtLeast(1)
    val today = LocalDate.now()
    Row(modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        days.forEachIndexed { i, d ->
            val grow = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                delay(120L + i * 40L)
                grow.animateTo(1f, tween(600, easing = Motion.EaseOut))
            }
            val isToday = d.date == today
            val color = when {
                isToday -> DinoColors.Green
                d.steps >= goal -> DinoColors.GreenMid
                else -> DinoColors.Card2
            }
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Box(
                    Modifier
                        .width(28.dp)
                        .fillMaxHeight((d.steps.toFloat() / maxV).coerceIn(0.03f, 1f) * 0.82f)
                        .heightIn(min = 4.dp)
                        .graphicsLayer {
                            scaleY = grow.value
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                        .background(color),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    Formats.shortDay(d.date), style = DinoType.Label, textAlign = TextAlign.Center,
                    color = if (isToday) DinoColors.Ink else DinoColors.InkFaint,
                )
            }
        }
    }
}

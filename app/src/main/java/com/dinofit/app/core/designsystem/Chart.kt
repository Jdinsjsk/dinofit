package com.dinofit.app.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import com.dinofit.app.core.common.Formats
import com.dinofit.app.core.model.ProgressPoint
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val shortDate = DateTimeFormatter.ofPattern("d MMM", Locale.FRANCE)
private fun fmtDate(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().format(shortDate)

/**
 * Courbe de progression (max estimé par séance). Le tracé se dessine de gauche à droite à l'ouverture.
 * Axe vertical : du plus bas au plus haut de tes valeurs, pour bien voir les écarts.
 */
@Composable
fun ProgressChart(points: List<ProgressPoint>, modifier: Modifier = Modifier) {
    val draw = remember { Animatable(0f) }
    LaunchedEffect(points.size) { draw.snapTo(0f); draw.animateTo(1f, tween(900, easing = Motion.EaseOut)) }
    val minV = points.minOfOrNull { it.estimatedMaxKg } ?: 0.0
    val maxV = points.maxOfOrNull { it.estimatedMaxKg } ?: 0.0
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            Text("${maxV.roundToInt()} kg", style = DinoType.Label, color = DinoColors.InkDim, modifier = Modifier.weight(1f))
        }
        Canvas(Modifier.fillMaxWidth().height(170.dp)) {
            val padX = 6.dp.toPx()
            val padY = 10.dp.toPx()
            val w = size.width - 2 * padX
            val h = size.height - 2 * padY
            // Lignes de repère
            for (i in 0..3) {
                val y = padY + h * i / 3f
                drawLine(DinoColors.Line, Offset(padX, y), Offset(padX + w, y), strokeWidth = 1.dp.toPx())
            }
            if (points.isEmpty()) return@Canvas
            val span = (maxV - minV).takeIf { it > 0.01 } ?: 1.0
            val xy = points.mapIndexed { i, p ->
                val x = if (points.size == 1) padX + w / 2 else padX + w * i / (points.size - 1)
                val y = padY + h * (1f - ((p.estimatedMaxKg - minV) / span).toFloat())
                Offset(x, if (maxV - minV < 0.01) padY + h / 2 else y)
            }
            val path = Path().apply {
                moveTo(xy.first().x, xy.first().y)
                xy.drop(1).forEach { lineTo(it.x, it.y) }
            }
            val partial = Path()
            PathMeasure().apply {
                setPath(path, false)
                getSegment(0f, length * draw.value, partial, true)
            }
            drawPath(partial, DinoColors.Amber, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            val shown = (xy.size * draw.value).roundToInt().coerceAtMost(xy.size)
            xy.take(shown).forEachIndexed { i, o ->
                val last = i == xy.lastIndex
                drawCircle(Color.White, (if (last) 7 else 5).dp.toPx(), o)
                drawCircle(if (last) DinoColors.AmberInk else DinoColors.Amber, (if (last) 5 else 3).dp.toPx(), o)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text("${minV.roundToInt()} kg", style = DinoType.Label, color = DinoColors.InkDim, modifier = Modifier.weight(1f))
        }
        if (points.isNotEmpty()) {
            Row(Modifier.fillMaxWidth()) {
                Text(fmtDate(points.first().time), style = DinoType.Label, color = DinoColors.InkFaint, modifier = Modifier.weight(1f))
                Text(fmtDate(points.last().time), style = DinoType.Label, color = DinoColors.InkFaint)
            }
        }
    }
}

/** Résumé chiffré d'une progression : départ, maintenant, écart en %. */
fun progressSummary(points: List<ProgressPoint>): String {
    if (points.isEmpty()) return "Pas encore de données."
    if (points.size == 1) return "Une seule séance pour l'instant : ${points.first().estimatedMaxKg.roundToInt()} kg estimés."
    val first = points.first().estimatedMaxKg
    val last = points.last().estimatedMaxKg
    val pct = if (first > 0) ((last - first) / first * 100).roundToInt() else 0
    val sign = if (pct > 0) "+" else ""
    return "${first.roundToInt()} kg au début, ${last.roundToInt()} kg maintenant ($sign$pct %) sur ${Formats.int(points.size)} séances."
}

fun starIcon(filled: Boolean): ImageVector = ImageVector.Builder(
    defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
).addPath(
    pathData = addPathNodes("M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9z"),
    fill = if (filled) SolidColor(Color.Black) else null,
    stroke = SolidColor(Color.Black),
    strokeLineWidth = 2f,
    strokeLineJoin = StrokeJoin.Round,
).build()

object StarIcons {
    val Filled = starIcon(true)
    val Outline = starIcon(false)
}

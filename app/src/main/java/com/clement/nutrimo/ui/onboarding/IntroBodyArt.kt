// Animated illustration of the tour's body page: a weight chart drawing itself down to its goal line.
package com.clement.nutrimo.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.ui.theme.NutrimoColors

private val WEIGHTS = listOf(82.4f, 81.9f, 82.1f, 81.3f, 80.8f, 81.0f, 80.3f, 79.9f, 79.4f)
const val WEIGHT_MS = 2600
private const val GOAL_KG = 78.5f
private const val TOP_KG = 82.8f
private const val LINE_START = 0.12f
private const val LINE_END = 0.75f

@Composable
fun WeightArt(active: Boolean) {
    val t = rememberTimeline(active, WEIGHT_MS)
    val pulse = rememberPulse(900)
    val line = easeOut(t.phase(LINE_START, LINE_END))
    Box(Modifier.size(ART_SIZE, 180.dp).shadow(2.dp, RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface)) {
        Canvas(Modifier.fillMaxSize()) { drawWeightChart(t, line, pulse) }
        Text("Poids", Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium)
        LossBadge(t.phase(LINE_END, 0.92f), Modifier.align(Alignment.TopEnd))
        Text(
            "Objectif",
            Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 4.dp).graphicsLayer { alpha = t.phase(0f, LINE_START) },
            style = MaterialTheme.typography.labelSmall,
            color = NutrimoColors.objective,
        )
    }
}

/** Goal line first, then the weight line drawn left to right with its dots popping as it passes them. */
private fun DrawScope.drawWeightChart(t: Float, line: Float, pulse: Float) {
    val pad = 28.dp.toPx()
    val min = GOAL_KG - 0.4f
    val points = chartPoints(WEIGHTS, size.width, size.height, pad, min, TOP_KG)
    val goalY = chartPoints(listOf(GOAL_KG, GOAL_KG), size.width, size.height, pad, min, TOP_KG)[0].y
    drawLine(
        NutrimoColors.objective.copy(alpha = t.phase(0f, LINE_START)), Offset(pad, goalY), Offset(size.width - pad, goalY),
        strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
    )
    drawPartialLine(points, line)
    points.forEachIndexed { i, p ->
        val s = popScale(t, LINE_START + (LINE_END - LINE_START) * i / (points.size - 1), 0.08f)
        drawCircle(Color.White, 5.dp.toPx() * s, p)
        drawCircle(NutrimoColors.weight, 3.5.dp.toPx() * s, p)
    }
    // The latest weigh-in keeps a soft ripple once the line has reached it.
    if (t >= LINE_END) drawCircle(NutrimoColors.weight.copy(alpha = 0.3f * (1f - pulse)), (6 + 10 * pulse).dp.toPx(), points.last())
}

private fun DrawScope.drawPartialLine(points: List<Offset>, fraction: Float) {
    if (fraction <= 0f) return
    val full = Path().apply {
        moveTo(points[0].x, points[0].y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    val measure = PathMeasure().apply { setPath(full, false) }
    val part = Path()
    measure.getSegment(0f, measure.length * fraction, part, true)
    drawPath(part, NutrimoColors.weight, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
}

/** "−3,0 kg" slides in from the right once the line is complete. */
@Composable
private fun LossBadge(p: Float, modifier: Modifier) {
    Text(
        "−3,0 kg",
        modifier.padding(10.dp).offset(x = 40.dp * (1f - overshoot(p))).graphicsLayer { alpha = p }
            .clip(RoundedCornerShape(50)).background(NutrimoColors.weight).padding(horizontal = 10.dp, vertical = 3.dp),
        color = Color.White,
        style = MaterialTheme.typography.labelLarge,
    )
}

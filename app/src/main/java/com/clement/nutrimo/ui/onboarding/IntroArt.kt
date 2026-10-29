// Animated illustrations of the tour, part 1: timeline helpers, welcome bloom, calorie ring, weight chart.
package com.clement.nutrimo.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.R
import com.clement.nutrimo.ui.common.fmt
import com.clement.nutrimo.ui.theme.NutrimoColors

val ART_SIZE = 220.dp
const val CALORIES_MS = 2600
private const val RING_KCAL = 1850
private const val RING_FILL = 0.8f
private val FOODS = listOf(Icons.Default.EggAlt, Icons.Default.BakeryDining, Icons.Default.RamenDining)
private val FOOD_COLORS = listOf(NutrimoColors.carbs, NutrimoColors.protein, NutrimoColors.fat)

/** Plays 0 → 1 over [durationMs] each time [active] turns true; back to 0 when the page is left. */
@Composable
fun rememberTimeline(active: Boolean, durationMs: Int): Float {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(active) {
        progress.snapTo(0f)
        if (active) progress.animateTo(1f, tween(durationMs, easing = LinearEasing))
    }
    return progress.value
}

/** Local progress of a step running from [start] to [end] of the timeline. */
fun Float.phase(start: Float, end: Float): Float = ((this - start) / (end - start)).coerceIn(0f, 1f)

fun easeOut(t: Float): Float = FastOutSlowInEasing.transform(t)

/** Goes slightly past 1 then settles, like OvershootInterpolator (tension 2). */
fun overshoot(t: Float): Float {
    val x = t - 1f
    return x * x * (3f * x + 2f) + 1f
}

/** Gentle endless 0 → 1 → 0 wave, for things that keep living once the scene is played. */
@Composable
fun rememberPulse(periodMs: Int): Float {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        0f, 1f, infiniteRepeatable(tween(periodMs, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse",
    )
    return pulse
}

/** The splash bloom, replayed once the splash is gone, over a softly breathing halo. */
@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun BloomArt(active: Boolean) {
    val image = AnimatedImageVector.animatedVectorResource(R.drawable.splash_icon)
    val halo = rememberPulse(1400)
    // An animated vector only plays when its target changes: start hidden, then switch on after the first frame.
    var play by remember { mutableStateOf(false) }
    LaunchedEffect(active) { play = active }
    val green = colorResource(R.color.ic_launcher_background)
    Box(contentAlignment = Alignment.Center) {
        Box(Modifier.size(ART_SIZE).graphicsLayer { scaleX = 1f + halo * 0.06f; scaleY = scaleX }.clip(CircleShape).background(green.copy(alpha = 0.18f)))
        // The launcher drawing is 108 units wide with a 72-unit visible circle.
        Box(Modifier.size(ART_SIZE - 24.dp).clip(CircleShape).background(green), contentAlignment = Alignment.Center) {
            Image(rememberAnimatedVectorPainter(image, play), null, Modifier.requiredSize((ART_SIZE - 24.dp) * 1.5f))
        }
    }
}

/** Foods drop into the journal ring one by one; each one fills it and raises the count. */
@Composable
fun CaloriesArt(active: Boolean) {
    val t = rememberTimeline(active, CALORIES_MS)
    val fill = FOODS.indices.sumOf { i -> easeOut(t.phase(foodStart(i) + 0.18f, foodStart(i) + 0.4f)).toDouble() }.toFloat() * RING_FILL / FOODS.size
    val track = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    Box(Modifier.size(ART_SIZE), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(ART_SIZE - 40.dp)) {
            val stroke = androidx.compose.ui.graphics.drawscope.Stroke(18.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawArc(track, 0f, 360f, false, style = stroke)
            if (fill > 0f) drawArc(NutrimoColors.weight, -90f, 360f * fill, false, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(fmt((RING_KCAL * fill / RING_FILL).toDouble(), 0), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("kcal", style = MaterialTheme.typography.labelMedium)
        }
        FOODS.forEachIndexed { i, icon -> FallingFood(icon, FOOD_COLORS[i], t.phase(foodStart(i), foodStart(i) + 0.24f)) }
        RemainingPill(t.phase(0.82f, 0.95f))
    }
}

private fun foodStart(i: Int) = 0.04f + 0.22f * i

/** Falls from above the ring to its center (first 75 %), then shrinks as if eaten. */
@Composable
private fun FallingFood(icon: ImageVector, color: Color, p: Float) {
    if (p <= 0f || p >= 1f) return
    val fall = (p / 0.75f).coerceAtMost(1f)
    val eaten = ((p - 0.75f) / 0.25f).coerceIn(0f, 1f)
    Box(
        Modifier.offset(y = (-110).dp * (1f - fall * fall)).graphicsLayer { scaleX = 1f - eaten; scaleY = 1f - eaten }
            .size(48.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, Modifier.size(28.dp), tint = Color.White) }
}

@Composable
private fun RemainingPill(p: Float) {
    Text(
        "Reste 450 kcal",
        Modifier.offset(y = 96.dp + 12.dp * (1f - easeOut(p))).graphicsLayer { alpha = p }
            // White outline: the pill sits on the ring and shares its green.
            .clip(RoundedCornerShape(50)).background(NutrimoColors.weight).border(2.dp, Color.White, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        color = Color.White,
        style = MaterialTheme.typography.labelLarge,
    )
}

/** Draws a point that pops when the timeline reaches it. */
fun popScale(t: Float, at: Float, length: Float = 0.12f): Float = overshoot(t.phase(at, at + length))

/** Converts a value series to canvas points (higher value = higher on screen). */
fun chartPoints(values: List<Float>, width: Float, height: Float, pad: Float, min: Float, max: Float): List<Offset> =
    values.mapIndexed { i, v ->
        Offset(pad + (width - 2 * pad) * i / (values.size - 1), pad + (height - 2 * pad) * (max - v) / (max - min))
    }

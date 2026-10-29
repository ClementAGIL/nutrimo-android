// Animated illustrations of the tour, part 2: habit tiles getting checked, and a week-long streak lighting a flame.
package com.clement.nutrimo.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.ui.theme.NutrimoColors

private class Habit(val label: String, val icon: ImageVector, val color: Color, val tilt: Float)

private val HABITS = listOf(
    Habit("Pas", Icons.AutoMirrored.Filled.DirectionsWalk, NutrimoColors.steps, -8f),
    Habit("Eau", Icons.Default.WaterDrop, NutrimoColors.water, 4f),
    Habit("Sport", Icons.Default.FitnessCenter, NutrimoColors.sport, 10f),
)
private val STREAK_DAYS = listOf("L", "M", "M", "J", "V", "S", "D")
private val TILE = 64.dp
const val HABITS_MS = 2400
const val STREAK_MS = 2600

/** Tiles slide up, their progress bars fill, then each one gets its check badge. */
@Composable
fun HabitsArt(active: Boolean) {
    val t = rememberTimeline(active, HABITS_MS)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
        HABITS.forEachIndexed { i, habit ->
            HabitColumn(
                habit,
                appear = easeOut(t.phase(0.04f + 0.1f * i, 0.26f + 0.1f * i)),
                fill = easeOut(t.phase(0.35f + 0.14f * i, 0.6f + 0.14f * i)),
                check = popScale(t, 0.6f + 0.14f * i),
            )
        }
    }
}

@Composable
private fun HabitColumn(habit: Habit, appear: Float, fill: Float, check: Float) {
    Column(
        Modifier.offset(y = 48.dp * (1f - appear)).graphicsLayer { alpha = appear },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(TILE + 12.dp)) {
            Box(
                Modifier.align(Alignment.Center).size(TILE).graphicsLayer { rotationZ = habit.tilt }
                    .clip(RoundedCornerShape(TILE * 0.27f)).background(habit.color),
                contentAlignment = Alignment.Center,
            ) { Icon(habit.icon, null, Modifier.size(TILE * 0.55f), tint = Color.White) }
            CheckBadge(check, Modifier.align(Alignment.TopEnd))
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.width(TILE).height(6.dp).clip(CircleShape).background(habit.color.copy(alpha = 0.2f))) {
            Box(Modifier.fillMaxWidth(fill).height(6.dp).clip(CircleShape).background(habit.color))
        }
        Spacer(Modifier.height(4.dp))
        Text(habit.label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun CheckBadge(scale: Float, modifier: Modifier) {
    if (scale <= 0f) return
    Box(
        modifier.size(24.dp).graphicsLayer { scaleX = scale; scaleY = scale }.clip(CircleShape)
            .background(NutrimoColors.caution).border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = Color.White) }
}

/** The days fill one by one, the flame grows with the streak and keeps flickering; sparkles and confetti at the end. */
@Composable
fun StreakArt(active: Boolean) {
    val t = rememberTimeline(active, STREAK_MS)
    val flicker = rememberPulse(450)
    val fills = STREAK_DAYS.indices.map { popScale(t, 0.1f + 0.09f * it, 0.1f) }
    val days = fills.count { it >= 0.5f }
    val sparkle = popScale(t, 0.82f, 0.14f)
    // Its own clock: the confetti keeps flying after the scene timeline has ended.
    val burst = rememberTimeline(active && t >= 0.82f, CONFETTI_MS)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            val grow = 0.5f + 0.5f * days / STREAK_DAYS.size
            Icon(
                Icons.Default.LocalFireDepartment, null,
                Modifier.size(120.dp).graphicsLayer {
                    scaleX = grow
                    scaleY = grow * (0.96f + 0.08f * flicker)
                    rotationZ = -3f + 6f * flicker
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                },
                tint = NutrimoColors.caution,
            )
            Sparkle(sparkle, Modifier.align(Alignment.TopStart).offset(12.dp, 10.dp), 26)
            Sparkle(sparkle, Modifier.align(Alignment.TopEnd).offset((-8).dp, 28.dp), 18)
            ConfettiBurst(burst, Modifier.matchParentSize())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            STREAK_DAYS.forEachIndexed { i, day -> DayDot(day, fills[i]) }
        }
        Spacer(Modifier.height(8.dp))
        Text("$days jour${if (days > 1) "s" else ""} de suite", fontWeight = FontWeight.SemiBold, color = NutrimoColors.weight)
    }
}

@Composable
private fun DayDot(day: String, fill: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(20.dp).border(2.dp, NutrimoColors.weight, CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(20.dp).graphicsLayer { scaleX = fill; scaleY = fill }.clip(CircleShape).background(NutrimoColors.weight))
        }
        Text(day, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun Sparkle(scale: Float, modifier: Modifier, sizeDp: Int) {
    if (scale <= 0f) return
    Icon(
        Icons.Default.AutoAwesome, null,
        modifier.size(sizeDp.dp).graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = 90f * (1f - scale) },
        tint = NutrimoColors.caution,
    )
}

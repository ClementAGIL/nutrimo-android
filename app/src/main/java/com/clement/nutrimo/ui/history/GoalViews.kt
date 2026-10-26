// Daily goal visuals: one color per goal, a dot (filled = achieved, ring = missed) and labelled rows.
package com.clement.nutrimo.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.domain.calc.DailyGoal
import com.clement.nutrimo.ui.common.StatRow
import com.clement.nutrimo.ui.theme.NutrimoColors

val DailyGoal.color: Color
    get() = when (this) {
        DailyGoal.CALORIES -> NutrimoColors.carbs
        DailyGoal.WEIGHT -> NutrimoColors.weight
        DailyGoal.SPORT -> NutrimoColors.trend
        DailyGoal.STEPS -> NutrimoColors.fat
        DailyGoal.WATER -> NutrimoColors.water
    }

/** Nothing is drawn when [achieved] is null (no data that day), but the space is kept so dots stay aligned. */
@Composable
fun GoalDot(goal: DailyGoal, achieved: Boolean?, size: Dp = 8.dp) {
    Canvas(Modifier.size(size)) {
        when (achieved) {
            true -> drawCircle(goal.color)
            false -> drawCircle(goal.color, radius = (this.size.minDimension - 1.5.dp.toPx()) / 2, style = Stroke(1.5.dp.toPx()))
            null -> Unit
        }
    }
}

/** A stat line preceded by the goal's dot. */
@Composable
fun GoalRow(goal: DailyGoal, achieved: Boolean?, value: String, label: String = goal.label) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GoalDot(goal, achieved, 12.dp)
        StatRow(label, value)
    }
}

@Composable
fun GoalLegend() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DailyGoal.entries.forEach { goal ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                GoalDot(goal, true, 10.dp)
                Spacer(Modifier.size(4.dp))
                Text(goal.label, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    Text(
        "Rond plein : objectif atteint · anneau : manqué · rien : aucune donnée ce jour-là.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

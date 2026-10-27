// Dashboard tab: goal and time progress bars, and the key figures of the current goal.
package com.clement.nutrimo.ui.weight

import com.clement.nutrimo.ui.common.HintText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.domain.calc.Bmi
import com.clement.nutrimo.domain.calc.GoalProgress
import com.clement.nutrimo.domain.calc.MIN_TREND_POINTS
import com.clement.nutrimo.ui.common.FAB_CLEARANCE
import com.clement.nutrimo.ui.common.ProgressBar
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.StatRow
import com.clement.nutrimo.ui.common.WarningText
import com.clement.nutrimo.ui.common.fmt
import com.clement.nutrimo.ui.common.full
import com.clement.nutrimo.ui.common.kcal
import com.clement.nutrimo.ui.common.kg
import com.clement.nutrimo.ui.common.percent
import com.clement.nutrimo.ui.theme.NutrimoColors

@Composable
fun WeightDashboard(state: WeightUiState, onNewGoal: () -> Unit, onGoals: () -> Unit, onAchieved: () -> Unit) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val progress = state.progress
        if (state.goal == null || progress == null) {
            SectionCard("Aucun objectif en cours") {
                Button(onClick = onNewGoal, modifier = Modifier.fillMaxWidth()) { Text("Définir un objectif") }
            }
        } else {
            if (state.goalReached) GoalReachedCard(onAchieved)
            ProgressCard(progress)
            FiguresCard(state, progress)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onNewGoal, modifier = Modifier.weight(1f)) { Text("Nouvel objectif") }
            OutlinedButton(onClick = onGoals, modifier = Modifier.weight(1f)) { Text("Mes objectifs") }
        }
        Spacer(Modifier.height(FAB_CLEARANCE))
    }
}

@Composable
private fun GoalReachedCard(onAchieved: () -> Unit) {
    SectionCard("🎉 Objectif atteint !") {
        Text("Bravo ! Vous pouvez clôturer cet objectif et en définir un nouveau.")
        Button(onClick = onAchieved) { Text("Marquer comme atteint") }
    }
}

@Composable
private fun ProgressCard(p: GoalProgress) {
    SectionCard("Progression") {
        PercentProgress("Poids", p.weightProgress, "${kg(p.lostKg.coerceAtLeast(0.0))} perdus sur ${kg(p.startKg - p.targetKg)}", NutrimoColors.weight)
        PercentProgress("Temps", p.timeProgress, "${p.daysElapsed} j écoulés · ${p.daysRemaining} j restants", NutrimoColors.objective)
        if (p.weightProgress + 0.05 < p.timeProgress) WarningText("Un peu en retard sur le plan : courage !")
    }
}

/** Title, then the bar with its percentage on the same line, then the detail. */
@Composable
private fun PercentProgress(title: String, progress: Double, detail: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProgressBar(progress, color, Modifier.weight(1f))
            Text(percent(progress, 1), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        HintText(detail)
    }
}

@Composable
private fun FiguresCard(state: WeightUiState, p: GoalProgress) {
    val goal = state.goal ?: return
    SectionCard("Chiffres clés") {
        StatRow("Poids de départ", "${kg(p.startKg)} (${goal.startDate.full()})")
        StatRow("Poids actuel", kg(p.currentKg))
        StatRow("Objectif", kg(p.targetKg))
        StatRow("Perdu", kg(p.lostKg))
        StatRow("Reste à perdre", kg(p.remainingKg))
        state.bmi?.let { StatRow("IMC", "${fmt(it)} · ${Bmi.category(it)}") }
        p.avgLossPerWeek?.let { StatRow("Perte moyenne", "${fmt(it, 2)} kg / semaine") }
        p.requiredPace?.let { StatRow("Rythme nécessaire", "${fmt(it, 2)} kg / semaine") }
        state.target?.let { StatRow("Objectif calorique", "${kcal(it.target)} / jour") }
        StatRow("Date prévue", goal.endDate.full())
        StatRow("Estimation (tendance)", estimate(p))
        HintText(
            "L'estimation prolonge la droite de tendance de vos pesées depuis le début de l'objectif.",
        )
    }
}

private fun estimate(p: GoalProgress): String = when {
    p.trend == null -> "$MIN_TREND_POINTS pesées minimum"
    p.estimatedEnd == null -> "pas de tendance à la baisse"
    else -> p.estimatedEnd.full()
}
